package com.runcat.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.runcat.i18n.I18nManager;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Auto-update via GitHub Releases API.
 * Downloads JavaRunCat-Windows-x64.zip, extracts, replaces, and restarts.
 */
public class UpdateChecker {

    private static final String GITHUB_API = "https://api.github.com/repos/ab-Astartes/RunCat-Java/releases/latest";
    private static final String CURRENT_VERSION = "1.2.0";
    private static final AtomicBoolean updating = new AtomicBoolean(false);

    public static void checkForUpdates() {
        if (updating.get()) return;
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(GITHUB_API).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                conn.setRequestProperty("User-Agent", "JavaRunCat/" + CURRENT_VERSION);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);

                if (conn.getResponseCode() != 200) return;

                String body = readStream(conn.getInputStream());
                JsonObject release = JsonParser.parseString(body).getAsJsonObject();
                String latestTag = release.get("tag_name").getAsString();
                String latestVersion = latestTag.startsWith("v") ? latestTag.substring(1) : latestTag;

                if (compareVersions(latestVersion, CURRENT_VERSION) > 0) {
                    String[] zipInfo = new String[2]; // [url, name]
                    for (var asset : release.getAsJsonArray("assets")) {
                        JsonObject a = asset.getAsJsonObject();
                        String name = a.get("name").getAsString();
                        if (name.contains("Windows") && name.endsWith(".zip")) {
                            zipInfo[0] = a.get("browser_download_url").getAsString();
                            zipInfo[1] = name;
                            break;
                        }
                    }

                    if (zipInfo[0] != null) {
                        String changelog = "";
                        if (release.has("body") && !release.get("body").isJsonNull()) {
                            changelog = release.get("body").getAsString();
                            if (changelog.length() > 300) changelog = changelog.substring(0, 300) + "...";
                        }

                        I18nManager i18n = I18nManager.getInstance();
                        String message = i18n.get("update.available", latestVersion, CURRENT_VERSION);
                        if (!changelog.isEmpty()) message += "\n\n" + changelog;

                        final String finalZipUrl = zipInfo[0];
                        final String finalZipName = zipInfo[1];
                        final String finalMessage = message;
                        SwingUtilities.invokeLater(() -> {
                            int choice = JOptionPane.showConfirmDialog(null,
                                    finalMessage, i18n.get("update.title"),
                                    JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                            if (choice == JOptionPane.YES_OPTION) {
                                performUpdate(finalZipUrl, finalZipName, latestVersion);
                            }
                        });
                    }
                }
                conn.disconnect();
            } catch (Exception ignored) {}
        }, "UpdateCheckThread").start();
    }

    public static void manualCheck() {
        if (updating.get()) {
            I18nManager i18n = I18nManager.getInstance();
            JOptionPane.showMessageDialog(null, i18n.get("update.inProgress"),
                    i18n.get("update.title"), JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        checkForUpdates();
    }

    private static void performUpdate(String zipUrl, String zipName, String newVersion) {
        if (!updating.compareAndSet(false, true)) return;

        I18nManager i18n = I18nManager.getInstance();
        JDialog progressDialog = new JDialog();
        progressDialog.setTitle(i18n.get("update.title"));
        progressDialog.setModal(false);
        progressDialog.setSize(400, 120);
        progressDialog.setLocationRelativeTo(null);
        progressDialog.setAlwaysOnTop(true);

        JLabel statusLabel = new JLabel(i18n.get("update.downloading", zipName));
        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        panel.add(statusLabel, BorderLayout.NORTH);
        panel.add(progressBar, BorderLayout.CENTER);
        progressDialog.add(panel);
        progressDialog.setVisible(true);

        new Thread(() -> {
            try {
                // Step 1: Download the zip
                Path downloadDir = Paths.get(System.getProperty("user.home"), ".java-runcat", "update");
                Files.createDirectories(downloadDir);
                Path zipPath = downloadDir.resolve(zipName);

                HttpURLConnection conn = (HttpURLConnection) new URL(zipUrl).openConnection();
                conn.setRequestProperty("User-Agent", "JavaRunCat/" + CURRENT_VERSION);
                long totalSize = conn.getContentLengthLong();

                try (InputStream in = conn.getInputStream();
                     OutputStream out = Files.newOutputStream(zipPath)) {
                    byte[] buf = new byte[8192];
                    long downloaded = 0;
                    int read;
                    while ((read = in.read(buf)) > 0) {
                        out.write(buf, 0, read);
                        downloaded += read;
                        if (totalSize > 0) {
                            int pct = (int) (downloaded * 100 / totalSize);
                            SwingUtilities.invokeLater(() -> progressBar.setValue(pct));
                        }
                    }
                }
                conn.disconnect();

                // Step 2: Extract
                statusLabel.setText(i18n.get("update.extracting"));
                SwingUtilities.invokeLater(() -> progressBar.setValue(50));
                Path extractDir = downloadDir.resolve("extracted");
                if (Files.exists(extractDir)) deleteDirectory(extractDir);
                Files.createDirectories(extractDir);

                ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-Command",
                        "Expand-Archive -Path '" + zipPath + "' -DestinationPath '" + extractDir + "' -Force");
                pb.redirectErrorStream(true);
                Process p = pb.start();
                p.waitFor(60, java.util.concurrent.TimeUnit.SECONDS);

                // Step 3: Find extracted JavaRunCat directory
                Path newAppDir = findAppDir(extractDir);
                if (newAppDir == null) {
                    SwingUtilities.invokeLater(() -> {
                        progressDialog.dispose();
                        JOptionPane.showMessageDialog(null, i18n.get("update.failedExtract"),
                                i18n.get("update.title"), JOptionPane.ERROR_MESSAGE);
                    });
                    updating.set(false);
                    return;
                }

                // Step 4: Determine current app location
                String appPath = ProcessHandle.current().info().command().orElse(null);
                if (appPath == null) {
                    appPath = Paths.get("JavaRunCat.exe").toAbsolutePath().toString();
                }
                Path currentAppDir = Paths.get(appPath).getParent();
                if (currentAppDir == null || !Files.exists(currentAppDir)) {
                    SwingUtilities.invokeLater(() -> {
                        progressDialog.dispose();
                        JOptionPane.showMessageDialog(null, i18n.get("update.failedPath"),
                                i18n.get("update.title"), JOptionPane.ERROR_MESSAGE);
                    });
                    updating.set(false);
                    return;
                }

                // Step 5: Create update script
                statusLabel.setText(i18n.get("update.installing"));
                SwingUtilities.invokeLater(() -> progressBar.setValue(75));

                Path updateScript = downloadDir.resolve("apply-update.ps1");
                long currentPid = ProcessHandle.current().pid();
                StringBuilder sb = new StringBuilder();
                sb.append("# JavaRunCat Update Script\n");
                sb.append("Write-Host \"Waiting for JavaRunCat (PID ").append(currentPid).append(") to exit...\"\n");
                sb.append("$proc = Get-Process -Id ").append(currentPid).append(" -ErrorAction SilentlyContinue\n");
                sb.append("if ($proc) {\n");
                sb.append("    $proc.WaitForExit()\n");
                sb.append("    Write-Host \"Process exited.\"\n");
                sb.append("}\n");
                sb.append("Start-Sleep -Seconds 2\n\n");
                sb.append("Write-Host \"Copying new files...\"\n");
                sb.append("Get-ChildItem -Path '").append(currentAppDir).append("' -Exclude '.java-runcat' |\n");
                sb.append("    Where-Object { $_.Name -ne 'app' } |\n");
                sb.append("    Remove-Item -Recurse -Force -ErrorAction SilentlyContinue\n\n");
                sb.append("Copy-Item -Path '").append(newAppDir).append("\\*' -Destination '").append(currentAppDir).append("' -Recurse -Force\n\n");
                sb.append("Write-Host \"Starting new version...\"\n");
                sb.append("Start-Process -FilePath '").append(currentAppDir).append("\\JavaRunCat.exe' -ArgumentList '--silent'\n\n");
                sb.append("Start-Sleep -Seconds 5\n");
                sb.append("Remove-Item -Path '").append(downloadDir).append("' -Recurse -Force -ErrorAction SilentlyContinue\n");
                Files.writeString(updateScript, sb.toString());

                // Step 6: Launch update script and exit
                SwingUtilities.invokeLater(() -> progressBar.setValue(90));
                statusLabel.setText(i18n.get("update.restarting"));

                ProcessBuilder scriptPb = new ProcessBuilder("powershell", "-NoProfile",
                        "-ExecutionPolicy", "Bypass", "-File", updateScript.toString());
                scriptPb.start();

                Thread.sleep(1000);

                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    System.exit(0);
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    progressDialog.dispose();
                    JOptionPane.showMessageDialog(null,
                            i18n.get("update.failed", e.getMessage()),
                            i18n.get("update.title"), JOptionPane.ERROR_MESSAGE);
                });
                updating.set(false);
            }
        }, "UpdatePerformThread").start();
    }

    private static Path findAppDir(Path extractDir) throws IOException {
        try (var stream = Files.walk(extractDir, 2)) {
            for (Path p : stream.toList()) {
                if (p.getFileName().toString().equals("JavaRunCat") && Files.isDirectory(p)) {
                    if (Files.exists(p.resolve("JavaRunCat.exe"))) return p;
                }
            }
        }
        if (Files.exists(extractDir.resolve("JavaRunCat.exe"))) return extractDir;
        return null;
    }

    private static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int maxLen = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLen; i++) {
            int n1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int n2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (n1 != n2) return n2 - n1;
        }
        return 0;
    }

    private static String readStream(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        return sb.toString();
    }

    private static void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (var stream = Files.walk(dir)) {
                stream.sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }
}
