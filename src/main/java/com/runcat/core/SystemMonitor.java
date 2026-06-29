package com.runcat.core;

import com.runcat.RunCatApp;
import com.runcat.config.AppConfig;
import com.runcat.i18n.I18nManager;

import java.awt.Image;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.io.*;
import java.nio.file.*;
import java.util.LinkedList;
import java.util.List;

/**
 * Monitors system CPU, memory, disk I/O, and network usage
 * with history tracking and alerts
 */
public class SystemMonitor {

    private final com.sun.management.OperatingSystemMXBean osBean;
    private double cpuUsage;
    private double memoryUsage;

    // History for dashboard chart (last 60 samples = 1 minute at 1s interval)
    private static final int HISTORY_SIZE = 60;
    private final LinkedList<Double> cpuHistory = new LinkedList<>();
    private final LinkedList<Double> memHistory = new LinkedList<>();

    // CPU alert cooldown tracking
    private long lastAlertTime = 0;

    // Disk I/O tracking
    private long lastDiskReadBytes = 0;
    private long lastDiskWriteBytes = 0;
    private double diskReadKBps = 0;
    private double diskWriteKBps = 0;
    private long lastDiskSampleTime = 0;

    // Network tracking
    private long lastNetBytesSent = 0;
    private long lastNetBytesRecv = 0;
    private double netUploadKBps = 0;
    private double netDownloadKBps = 0;
    private long lastNetSampleTime = 0;
    private final ProcessMetricsCollector processMetricsCollector;
    private volatile MonitorSnapshot latestSnapshot = MonitorSnapshot.empty();

    public SystemMonitor() {
        this.osBean = (com.sun.management.OperatingSystemMXBean)
                java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        this.cpuUsage = 0;
        this.memoryUsage = 0;
        this.lastDiskSampleTime = System.currentTimeMillis();
        this.lastNetSampleTime = System.currentTimeMillis();
        this.processMetricsCollector = new ProcessMetricsCollector();
    }

    public void update() {
        // CPU usage (0-100)
        cpuUsage = osBean.getSystemCpuLoad() * 100.0;
        if (cpuUsage < 0) cpuUsage = 0;

        // Memory usage
        long totalMem = osBean.getTotalMemorySize();
        long freeMem = osBean.getFreeMemorySize();
        if (totalMem > 0) {
            memoryUsage = ((double) (totalMem - freeMem) / totalMem) * 100.0;
        }

        // Record history
        cpuHistory.addLast(cpuUsage);
        if (cpuHistory.size() > HISTORY_SIZE) cpuHistory.removeFirst();
        memHistory.addLast(memoryUsage);
        if (memHistory.size() > HISTORY_SIZE) memHistory.removeFirst();

        // Update disk and network stats
        updateDiskStats();
        updateNetworkStats();

        // Check CPU alert
        checkCpuAlert();

        List<ProcessUsageSnapshot> processSamples = processMetricsCollector.collect();
        int topProcessCount = Math.max(1, RunCatApp.config().getTopProcessCount());
        latestSnapshot = new MonitorSnapshot(
                System.currentTimeMillis(),
                cpuUsage,
                memoryUsage,
                diskReadKBps,
                diskWriteKBps,
                netDownloadKBps,
                netUploadKBps,
                getCpuHistory(),
                getMemHistory(),
                ProcessMetricsCollector.topNByCpu(processSamples, topProcessCount),
                ProcessMetricsCollector.topNByMemory(processSamples, topProcessCount),
                ProcessMetricsCollector.topNByDisk(processSamples, topProcessCount),
                ProcessMetricsCollector.topNByNetwork(processSamples, topProcessCount));
    }

    private void updateDiskStats() {
        try {
            // Use PowerShell to get disk IO counters (WMIC removed, Win11 deprecated)
            long totalRead = 0, totalWrite = 0;
            File scriptFile = File.createTempFile("runcat-disk-", ".ps1");
            scriptFile.deleteOnExit();
            try (FileWriter fw = new FileWriter(scriptFile, java.nio.charset.StandardCharsets.UTF_8)) {
                fw.write("[Console]::OutputEncoding = [System.Text.Encoding]::UTF8\n");
                fw.write("$disk = Get-CimInstance Win32_PerfFormattedData_PerfDisk_PhysicalDisk | Where-Object { $_.Name -eq '_Total' }\n");
                fw.write("if ($disk) { '{0}|{1}' -f $disk.DiskReadBytesPersec, $disk.DiskWriteBytesPersec }\n");
            }
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-File", scriptFile.getAbsolutePath());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("[")) continue;
                    String[] parts = line.split("\\|");
                    if (parts.length >= 2) {
                        try {
                            totalRead = Long.parseLong(parts[0].trim());
                            totalWrite = Long.parseLong(parts[1].trim());
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            p.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
            scriptFile.delete();

            long now = System.currentTimeMillis();
            double elapsed = (now - lastDiskSampleTime) / 1000.0;
            if (elapsed > 0 && lastDiskReadBytes > 0) {
                diskReadKBps = (totalRead - lastDiskReadBytes) / 1024.0 / elapsed;
                diskWriteKBps = (totalWrite - lastDiskWriteBytes) / 1024.0 / elapsed;
                if (diskReadKBps < 0) diskReadKBps = 0;
                if (diskWriteKBps < 0) diskWriteKBps = 0;
            }
            lastDiskReadBytes = totalRead;
            lastDiskWriteBytes = totalWrite;
            lastDiskSampleTime = now;
        } catch (Exception e) {
            updateDiskStatsFallback();
        }
    }

    private void updateDiskStatsFallback() {
        try {
            long totalRead = 0, totalWrite = 0;
            for (Path root : FileSystems.getDefault().getRootDirectories()) {
                FileStore store = Files.getFileStore(root);
                totalRead += store.getTotalSpace() - store.getUsableSpace();
            }
            long now = System.currentTimeMillis();
            double elapsed = (now - lastDiskSampleTime) / 1000.0;
            if (elapsed > 0 && lastDiskReadBytes > 0) {
                diskReadKBps = Math.abs(totalRead - lastDiskReadBytes) / 1024.0 / elapsed;
            }
            lastDiskReadBytes = totalRead;
            lastDiskWriteBytes = totalWrite;
            lastDiskSampleTime = now;
        } catch (Exception ignored) {}
    }

    private void updateNetworkStats() {
        try {
            // Use netstat -e which outputs cumulative bytes received/sent
            long totalRecv = 0, totalSent = 0;
            ProcessBuilder pb = new ProcessBuilder("netstat", "-e");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                // Find the line with two large cumulative byte numbers (locale-independent)
                // Chinese: "字节  3628365290  2559373627" → parts=["字节","3628365290","2559373627"]
                // English: "Bytes  3628365290  2559373627" → parts=["Bytes","3628365290","2559373627"]
                // Strategy: find two long values > 1M in the line
                String[] parts = line.split("\\s+");
                long[] bigNums = new long[2];
                int bigCount = 0;
                for (String part : parts) {
                    try {
                        long v = Long.parseLong(part);
                        if (v > 1000000 && bigCount < 2) {
                            bigNums[bigCount++] = v;
                        }
                    } catch (NumberFormatException ignored) {}
                }
                if (bigCount == 2) {
                    totalRecv = bigNums[0];
                    totalSent = bigNums[1];
                }
            }
            p.waitFor();

            long now = System.currentTimeMillis();
            double elapsed = (now - lastNetSampleTime) / 1000.0;
            if (elapsed > 0 && lastNetBytesRecv > 0) {
                netDownloadKBps = (totalRecv - lastNetBytesRecv) / 1024.0 / elapsed;
                netUploadKBps = (totalSent - lastNetBytesSent) / 1024.0 / elapsed;
                if (netDownloadKBps < 0) netDownloadKBps = 0;
                if (netUploadKBps < 0) netUploadKBps = 0;
            }
            lastNetBytesRecv = totalRecv;
            lastNetBytesSent = totalSent;
            lastNetSampleTime = now;
        } catch (Exception ignored) {
            // Network stats unavailable
        }
    }

    private void checkCpuAlert() {
        AppConfig config = RunCatApp.config();
        if (!config.isCpuAlertEnabled()) return;

        long now = System.currentTimeMillis();
        long cooldownMs = config.getCpuAlertCooldown() * 1000;

        if (cpuUsage >= config.getCpuAlertThreshold()
                && (now - lastAlertTime) > cooldownMs) {
            lastAlertTime = now;
            I18nManager i18n = I18nManager.getInstance();
            if (SystemTray.isSupported()) {
                try {
                    SystemTray tray = SystemTray.getSystemTray();
                    Image img = Toolkit.getDefaultToolkit().createImage("");
                    TrayIcon notifyIcon = new TrayIcon(img);
                    notifyIcon.setImageAutoSize(true);
                    tray.add(notifyIcon);
                    notifyIcon.displayMessage(
                            i18n.get("notification.cpuHigh.title"),
                            i18n.get("notification.cpuHigh", getCpuUsageText()),
                            TrayIcon.MessageType.WARNING);
                    new Thread(() -> {
                        try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
                        tray.remove(notifyIcon);
                    }).start();
                } catch (Exception ignored) {}
            }
        }
    }

    // ======================== Getters ========================

    public double getCpuUsage() { return cpuUsage; }
    public double getMemoryUsage() { return memoryUsage; }

    public String getCpuUsageText() { return String.format("%.1f%%", cpuUsage); }
    public String getMemoryUsageText() { return String.format("%.1f%%", memoryUsage); }

    public String getDiskUsageText() {
        I18nManager i18n = I18nManager.getInstance();
        if (diskReadKBps < 0 && diskWriteKBps < 0) return i18n.get("monitor.notAvailable");
        return String.format(i18n.get("monitor.diskFormat"), Math.max(0, diskReadKBps), Math.max(0, diskWriteKBps));
    }

    public String getNetworkUsageText() {
        I18nManager i18n = I18nManager.getInstance();
        if (netDownloadKBps <= 0 && netUploadKBps <= 0) return i18n.get("monitor.notAvailable");
        return String.format(i18n.get("monitor.netFormat"), netDownloadKBps, netUploadKBps);
    }

    public LinkedList<Double> getCpuHistory() { return new LinkedList<>(cpuHistory); }
    public LinkedList<Double> getMemHistory() { return new LinkedList<>(memHistory); }

    public String getMemoryDetailText() {
        long totalMem = osBean.getTotalMemorySize();
        long freeMem = osBean.getFreeMemorySize();
        long usedMem = totalMem - freeMem;
        return String.format("%.0f MB / %.0f MB", usedMem / 1e6, totalMem / 1e6);
    }

    public MonitorSnapshot getSnapshot() {
        return latestSnapshot;
    }
}
