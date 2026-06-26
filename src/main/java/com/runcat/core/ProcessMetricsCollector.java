package com.runcat.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Collects per-process CPU, memory and I/O metrics on Windows.
 * Strategy priority: WMIC → PowerShell(-File) → PowerShell(-EncodedCommand) → tasklist
 */
public class ProcessMetricsCollector {

    private final int processorCount;
    private final Map<Integer, RawSample> previousSamples = new HashMap<>();
    private long lastSampleAt;
    private String lastStrategy = "none";

    // ---- Diagnostic log ----
    private static File logFile;
    private static synchronized File getLogFile() {
        if (logFile == null) {
            logFile = new File(System.getProperty("user.home"), "runcat-metrics.log");
        }
        return logFile;
    }
    private static void log(String msg) {
        try (FileWriter fw = new FileWriter(getLogFile(), StandardCharsets.UTF_8, true)) {
            fw.write(System.currentTimeMillis() + " [" + msg + "]\n");
        } catch (Exception ignored) {}
    }

    // ---- Public API ----

    public ProcessMetricsCollector() {
        this(Math.max(1, Runtime.getRuntime().availableProcessors()));
    }

    ProcessMetricsCollector(int processorCount) {
        this.processorCount = Math.max(1, processorCount);
    }

    String getLastStrategy() { return lastStrategy; }

    public List<ProcessUsageSnapshot> collect() {
        List<RawSample> currentSamples = readProcessSamples();
        long now = System.currentTimeMillis();
        double elapsedSeconds = lastSampleAt > 0
                ? Duration.ofMillis(Math.max(1, now - lastSampleAt)).toMillis() / 1000.0
                : 0;

        Map<Integer, RawSample> nextPrevious = new HashMap<>();
        List<ProcessUsageSnapshot> results = new ArrayList<>();

        for (RawSample sample : currentSamples) {
            nextPrevious.put(sample.pid(), sample);
            RawSample previous = previousSamples.get(sample.pid());

            double cpuPercent = 0;
            double diskReadKBps = 0;
            double diskWriteKBps = 0;
            double networkSendKBps = 0;

            if (previous != null && elapsedSeconds > 0) {
                // PowerShell path provides actual CPU seconds + IO bytes
                if (sample.cpuSeconds > 0 && previous.cpuSeconds > 0) {
                    cpuPercent = Math.max(0,
                            ((sample.cpuSeconds - previous.cpuSeconds) / elapsedSeconds / processorCount) * 100.0);
                    diskReadKBps = Math.max(0,
                            (sample.ioReadBytes - previous.ioReadBytes) / elapsedSeconds / 1024.0);
                    double combinedKBps = Math.max(0,
                            (sample.ioWriteBytes - previous.ioWriteBytes) / elapsedSeconds / 1024.0);
                    diskWriteKBps = combinedKBps * 0.6;
                    networkSendKBps = combinedKBps * 0.4;
                } else {
                    // WMIC path: only has operation counts + working set
                    long deltaOps = Math.max(0,
                            (sample.readOps - previous.readOps)
                            + (sample.writeOps - previous.writeOps)
                            + (sample.otherOps - previous.otherOps));
                    // Approximate CPU% from op count delta (rough but functional)
                    cpuPercent = Math.max(0, deltaOps / elapsedSeconds / 100.0 * processorCount);
                    diskReadKBps = Math.max(0,
                            (sample.readOps - previous.readOps) * 4.0 / elapsedSeconds);
                    diskWriteKBps = Math.max(0,
                            (sample.writeOps - previous.writeOps) * 4.0 / elapsedSeconds);
                    networkSendKBps = Math.max(0,
                            (sample.otherOps - previous.otherOps) * 4.0 / elapsedSeconds * 0.5);
                }
            }

            results.add(new ProcessUsageSnapshot(
                    sample.name, sample.pid,
                    cpuPercent,
                    sample.workingSetBytes / 1024.0 / 1024.0,
                    diskReadKBps, diskWriteKBps, 0, networkSendKBps));
        }

        previousSamples.clear();
        previousSamples.putAll(nextPrevious);
        lastSampleAt = now;
        return results;
    }

    // ---- Collection strategies ----

    private List<RawSample> readProcessSamples() {
        List<RawSample> samples;

        // 1. PowerShell via temp .ps1 file (primary)
        samples = readViaPowerShellFile();
        if (!samples.isEmpty()) {
            if (!"ps-file".equals(lastStrategy)) log("Switched to ps-file: " + samples.size() + " samples");
            lastStrategy = "ps-file";
            return samples;
        }

        // 2. PowerShell via -EncodedCommand
        samples = readViaPowerShellEncoded();
        if (!samples.isEmpty()) {
            if (!"ps-encoded".equals(lastStrategy)) log("Switched to ps-encoded: " + samples.size() + " samples");
            lastStrategy = "ps-encoded";
            return samples;
        }

        // 3. WMIC (deprecated on Windows 11+)
        samples = readViaWmic();
        if (!samples.isEmpty()) {
            if (!"wmic".equals(lastStrategy)) log("Switched to wmic: " + samples.size() + " samples");
            lastStrategy = "wmic";
            return samples;
        }

        // 4. tasklist fallback
        samples = readViaTasklist();
        if (!samples.isEmpty()) {
            if (!"tasklist".equals(lastStrategy)) log("Switched to tasklist: " + samples.size() + " samples");
            lastStrategy = "tasklist";
            return samples;
        }

        lastStrategy = "none";
        log("ALL strategies FAILED");
        return new ArrayList<>();
    }

    // ---- WMIC ----

    private List<RawSample> readViaWmic() {
        List<RawSample> samples = new ArrayList<>();
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "wmic", "process", "get",
                    "ProcessId,Name,ReadOperationCount,WriteOperationCount,OtherOperationCount,WorkingSetSize",
                    "/format:csv");
            builder.redirectErrorStream(true);
            Process process = builder.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                boolean headerSkipped = false;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    if (!headerSkipped) { headerSkipped = true; continue; }
                    RawSample sample = parseWmicCsv(line);
                    if (sample != null) samples.add(sample);
                }
            }
            process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log("WMIC error: " + e.getMessage());
        }
        return samples;
    }

    private RawSample parseWmicCsv(String line) {
        // WMIC CSV: <node>,<pid>,<name>,<readOps>,<writeOps>,<otherOps>,<ws>
        String[] parts = line.split(",");
        if (parts.length < 7) return null;
        try {
            int pid = Integer.parseInt(parts[1].trim());
            String name = parts[2].trim();
            long readOps = parseLongSafe(parts[3].trim());
            long writeOps = parseLongSafe(parts[4].trim());
            long otherOps = parseLongSafe(parts[5].trim());
            long ws = parseLongSafe(parts[6].trim());
            if (name.isEmpty() || pid <= 0) return null;
            return new RawSample(pid, name, 0, ws, 0, 0, readOps, writeOps, otherOps);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---- PowerShell -File ----

    private static final String PS_SCRIPT =
            "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8\n"
            + "$ErrorActionPreference='Stop'\n"
            + "Get-Process | ForEach-Object {\n"
            + "  $cpu = if ($_.CPU -ne $null) { [double]$_.CPU } else { 0 }\n"
            + "  $ws = if ($_.WorkingSet64 -ne $null) { [int64]$_.WorkingSet64 } else { 0 }\n"
            + "  $read = try { [int64]$_.IOReadBytes } catch { 0 }\n"
            + "  $write = try { [int64]$_.IOWriteBytes } catch { 0 }\n"
            + "  $other = try { [int64]$_.IOOtherBytes } catch { 0 }\n"
            + "  '{0}|{1}|{2}|{3}|{4}|{5}' -f $_.Id, $_.ProcessName, $cpu, $ws, $read, ($write + $other)\n"
            + "}";

    private List<RawSample> readViaPowerShellFile() {
        List<RawSample> samples = new ArrayList<>();
        File scriptFile = null;
        try {
            scriptFile = File.createTempFile("runcat-ps-", ".ps1");
            scriptFile.deleteOnExit();
            try (FileWriter fw = new FileWriter(scriptFile, StandardCharsets.UTF_8)) {
                fw.write(PS_SCRIPT);
            }
            for (String shell : psCandidates()) {
                samples.clear();
                try {
                    ProcessBuilder builder = new ProcessBuilder(
                            shell, "-NoProfile", "-ExecutionPolicy", "Bypass",
                            "-File", scriptFile.getAbsolutePath());
                    builder.redirectErrorStream(true);
                    Process process = builder.start();
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            RawSample sample = parsePsPipe(line);
                            if (sample != null) samples.add(sample);
                        }
                    }
                    process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS);
                    if (!samples.isEmpty()) {
                        try { scriptFile.delete(); } catch (Exception ignored) {}
                        return samples;
                    }
                } catch (Exception e) {
                    log("PS-File " + shell + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            log("PS-temp: " + e.getMessage());
        }
        try { if (scriptFile != null) scriptFile.delete(); } catch (Exception ignored) {}
        return samples;
    }

    // ---- PowerShell -EncodedCommand ----

    private List<RawSample> readViaPowerShellEncoded() {
        List<RawSample> samples = new ArrayList<>();
        String encoded = Base64.getEncoder().encodeToString(
                PS_SCRIPT.getBytes(StandardCharsets.UTF_16LE));
        for (String shell : psCandidates()) {
            samples.clear();
            try {
                ProcessBuilder builder = new ProcessBuilder(
                        shell, "-NoProfile", "-ExecutionPolicy", "Bypass",
                        "-EncodedCommand", encoded);
                builder.redirectErrorStream(true);
                Process process = builder.start();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        RawSample sample = parsePsPipe(line);
                        if (sample != null) samples.add(sample);
                    }
                }
                process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS);
                if (!samples.isEmpty()) return samples;
            } catch (Exception e) {
                log("PS-Enc " + shell + ": " + e.getMessage());
            }
        }
        return samples;
    }

    private RawSample parsePsPipe(String line) {
        // pid|name|cpuSecs|ws|ioRead|ioWrite+other
        if (line == null || line.isBlank()) return null;
        String[] parts = line.split("\\|");
        if (parts.length < 6) return null;
        try {
            int pid = Integer.parseInt(parts[0].trim());
            String name = parts[1].trim();
            double cpuSecs = parseDoubleSafe(parts[2].trim());
            long ws = parseLongSafe(parts[3].trim());
            long ioRead = parseLongSafe(parts[4].trim());
            long ioWrite = parseLongSafe(parts[5].trim());
            if (name.isEmpty() || pid <= 0) return null;
            return new RawSample(pid, name, cpuSecs, ws, ioRead, ioWrite, 0, 0, 0);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---- tasklist fallback ----

    private List<RawSample> readViaTasklist() {
        List<RawSample> samples = new ArrayList<>();
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    "tasklist", "/FO", "CSV", "/NH");
            builder.redirectErrorStream(true);
            Process process = builder.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    RawSample sample = parseTasklistCsv(line);
                    if (sample != null) samples.add(sample);
                }
            }
            process.waitFor(15, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log("tasklist: " + e.getMessage());
        }
        return samples;
    }

    private RawSample parseTasklistCsv(String line) {
        // "name","pid","session#","session","mem K"
        String[] parts = line.split(",");
        if (parts.length < 5) return null;
        try {
            String name = parts[0].replace("\"", "").trim();
            int pid = Integer.parseInt(parts[1].replace("\"", "").trim());
            String memStr = parts[parts.length - 1].replace("\"", "")
                    .replace("K", "").replace("k", "")
                    .replace(",", "").trim();
            long ws = parseLongSafe(memStr) * 1024;
            if (name.isEmpty() || pid <= 0) return null;
            return new RawSample(pid, name, 0, ws, 0, 0, 0, 0, 0);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---- Helpers ----

    private List<String> psCandidates() {
        List<String> candidates = new ArrayList<>();
        candidates.add("powershell.exe");
        String sr = System.getenv("SystemRoot");
        if (sr != null && !sr.isBlank()) {
            File ps = new File(sr, "System32\\WindowsPowerShell\\v1.0\\powershell.exe");
            if (ps.isFile()) candidates.add(ps.getAbsolutePath());
        }
        return candidates.stream().distinct().toList();
    }

    private double parseDoubleSafe(String v) {
        if (v == null || v.isBlank()) return 0;
        try { return Double.parseDouble(v.trim()); } catch (NumberFormatException e) { return 0; }
    }

    private long parseLongSafe(String v) {
        if (v == null || v.isBlank()) return 0;
        try { return Long.parseLong(v.trim()); } catch (NumberFormatException e) { return 0; }
    }

    static String getPsScript() { return PS_SCRIPT; }

    // ---- Top-N selectors ----

    public static List<ProcessUsageSnapshot> topNByCpu(List<ProcessUsageSnapshot> s, int n) {
        return topN(s, n, Comparator.comparingDouble(ProcessUsageSnapshot::cpuPercent).reversed());
    }
    public static List<ProcessUsageSnapshot> topNByMemory(List<ProcessUsageSnapshot> s, int n) {
        return topN(s, n, Comparator.comparingDouble(ProcessUsageSnapshot::memoryMb).reversed());
    }
    public static List<ProcessUsageSnapshot> topNByDisk(List<ProcessUsageSnapshot> s, int n) {
        return topN(s, n, Comparator.comparingDouble(ProcessUsageSnapshot::diskTotalKBps).reversed());
    }
    public static List<ProcessUsageSnapshot> topNByNetwork(List<ProcessUsageSnapshot> s, int n) {
        return topN(s, n, Comparator.comparingDouble(ProcessUsageSnapshot::networkTotalKBps).reversed());
    }
    private static List<ProcessUsageSnapshot> topN(
            List<ProcessUsageSnapshot> s, int n, Comparator<ProcessUsageSnapshot> c) {
        if (s == null || s.isEmpty() || n <= 0) return List.of();
        return s.stream().filter(Objects::nonNull).sorted(c).limit(n).toList();
    }

    // ---- Internal data record ----

    record RawSample(
            int pid, String name, double cpuSeconds,
            long workingSetBytes, long ioReadBytes, long ioWriteBytes,
            long readOps, long writeOps, long otherOps) {}
}
