package com.runcat.util;

import java.nio.file.*;

/**
 * Windows auto-start (startup) manager
 * Uses Windows Registry to add/remove auto-start entry
 */
public class AutoStartManager {

    private static final String REG_KEY = "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String APP_NAME = "JavaRunCat";

    /**
     * Set or remove auto-start
     */
    public static void setAutoStart(boolean enable) {
        if (enable) {
            String jarPath = getJarPath();
            if (jarPath != null) {
                String command = "reg add \"" + REG_KEY + "\" /v \"" + APP_NAME +
                        "\" /t REG_SZ /d \"\\\"\" + jarPath + \"\\\"\" /f";
                try {
                    ProcessBuilder pb = new ProcessBuilder("cmd", "/c", command);
                    pb.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            String command = "reg delete \"" + REG_KEY + "\" /v \"" + APP_NAME + "\" /f";
            try {
                ProcessBuilder pb = new ProcessBuilder("cmd", "/c", command);
                pb.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Check if auto-start is currently enabled
     */
    public static boolean isAutoStartEnabled() {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c",
                    "reg query \"" + REG_KEY + "\" /v \"" + APP_NAME + "\"");
            Process p = pb.start();
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static String getJarPath() {
        try {
            String path = AutoStartManager.class.getProtectionDomain()
                    .getCodeSource().getLocation().getPath();
            // Decode URL-encoded path
            path = java.net.URLDecoder.decode(path, "UTF-8");
            // Remove leading slash on Windows
            if (path.startsWith("/") && path.contains(":")) {
                path = path.substring(1);
            }
            return path;
        } catch (Exception e) {
            return null;
        }
    }
}
