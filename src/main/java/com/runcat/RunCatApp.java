package com.runcat;

import com.runcat.core.SystemMonitor;
import com.runcat.ui.TrayIconManager;
import com.runcat.config.AppConfig;
import com.runcat.i18n.I18nManager;
import com.runcat.animation.AnimationManager;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Java RunCat - Enhanced desktop pet for Windows taskbar
 * Based on RunCat365 concept with multi-language and custom animation support
 */
public class RunCatApp {

    private static volatile boolean running = true;
    private static TrayIconManager trayIconManager;
    private static SystemMonitor systemMonitor;
    private static AnimationManager animationManager;
    private static AppConfig appConfig;
    private static FileLock instanceLock;
    private static FileChannel lockChannel;

    public static void main(String[] args) {
        boolean silent = false;
        for (String arg : args) {
            if ("--silent".equals(arg) || "-s".equals(arg)) {
                silent = true;
            }
            if ("--help".equals(arg) || "-h".equals(arg)) {
                System.out.println("Java RunCat v1.0.0");
                System.out.println("Usage: java-runcat [options]");
                System.out.println("  --silent, -s   Start silently (no initial notification)");
                System.out.println("  --help,    -h  Show this help");
                System.exit(0);
            }
        }

        if (!acquireInstanceLock()) {
            JOptionPane.showMessageDialog(null,
                    "Java RunCat is already running.",
                    "Java RunCat", JOptionPane.WARNING_MESSAGE);
            System.exit(0);
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        if (!SystemTray.isSupported()) {
            JOptionPane.showMessageDialog(null,
                    "System tray is not supported on this platform.",
                    "Java RunCat", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        appConfig = AppConfig.load();
        I18nManager.getInstance().setLocale(appConfig.getLanguage());
        animationManager = new AnimationManager(appConfig);
        systemMonitor = new SystemMonitor();

        SwingUtilities.invokeLater(() -> {
            try {
                trayIconManager = new TrayIconManager(appConfig, animationManager, systemMonitor);
                trayIconManager.start();
                startAnimationLoop();
                startCpuMonitorLoop();
                startThemeWatcher();
            } catch (AWTException e) {
                JOptionPane.showMessageDialog(null,
                        "Failed to create tray icon: " + e.getMessage(),
                        "Java RunCat", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running = false;
            if (trayIconManager != null) trayIconManager.stop();
            releaseInstanceLock();
        }));
    }

    private static boolean acquireInstanceLock() {
        try {
            String lockFile = System.getProperty("java.io.tmpdir") + "\\java-runcat.lock";
            lockChannel = FileChannel.open(
                    Paths.get(lockFile),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE);
            instanceLock = lockChannel.tryLock();
            return instanceLock != null;
        } catch (IOException e) {
            return false;
        }
    }

    private static void releaseInstanceLock() {
        try {
            if (instanceLock != null) instanceLock.release();
            if (lockChannel != null) lockChannel.close();
        } catch (IOException ignored) {}
    }

    /**
     * Animation loop - frame interval is driven by CPU usage (RunCat365 core feature).
     * CPU 0%  → 300ms interval (slow walk)
     * CPU 50% → 175ms interval (trot)
     * CPU 100% → 50ms interval (sprint)
     * Speed multiplier adjusts the base range.
     */
    private static void startAnimationLoop() {
        Thread animationThread = new Thread(() -> {
            while (running) {
                try {
                    double cpuUsage = systemMonitor.getCpuUsage();
                    double multiplier = config().getSpeedMultiplier();

                    // Map CPU 0-100% → interval 300-50ms, then divide by speed multiplier
                    int interval = (int) ((300 - (cpuUsage / 100.0) * 250) / multiplier);
                    interval = Math.max(30, Math.min(500, interval));

                    SwingUtilities.invokeLater(() -> {
                        if (trayIconManager != null) {
                            trayIconManager.updateIcon();
                        }
                    });

                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "AnimationThread");
        animationThread.setDaemon(true);
        animationThread.start();
    }

    private static void startCpuMonitorLoop() {
        Thread monitorThread = new Thread(() -> {
            while (running) {
                try {
                    systemMonitor.update();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "CpuMonitorThread");
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    /**
     * Watch Windows theme changes and auto-switch icon theme.
     * Uses registry polling (lightweight, ~5s interval) to detect dark/light mode.
     */
    private static void startThemeWatcher() {
        if (!"auto".equals(appConfig.getIconTheme())) return;

        Thread themeThread = new Thread(() -> {
            boolean lastDark = isWindowsDarkMode();
            while (running) {
                try {
                    Thread.sleep(5000);
                    boolean isDark = isWindowsDarkMode();
                    if (isDark != lastDark) {
                        lastDark = isDark;
                        // Theme changed - rebuild menu with new state
                        SwingUtilities.invokeLater(() -> {
                            if (trayIconManager != null) {
                                trayIconManager.rebuildMenu();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "ThemeWatcherThread");
        themeThread.setDaemon(true);
        themeThread.start();
    }

    /**
     * Check if Windows is in dark mode by reading registry
     */
    private static boolean isWindowsDarkMode() {
        try {
            ProcessBuilder pb = new ProcessBuilder("reg", "query",
                    "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                    "/v", "AppsUseLightTheme");
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("0x0")) return true;   // dark mode
                if (line.contains("0x1")) return false;   // light mode
            }
        } catch (Exception ignored) {}
        return true; // default to dark
    }

    public static boolean isSystemDarkMode() {
        return isWindowsDarkMode();
    }

    public static AppConfig config() { return appConfig; }
    public static SystemMonitor getSystemMonitor() { return systemMonitor; }
    public static AnimationManager getAnimationManager() { return animationManager; }

    public static void restart() {
        if (trayIconManager != null) trayIconManager.stop();
        SwingUtilities.invokeLater(() -> {
            try {
                I18nManager.getInstance().setLocale(appConfig.getLanguage());
                animationManager = new AnimationManager(appConfig);
                trayIconManager = new TrayIconManager(appConfig, animationManager, systemMonitor);
                trayIconManager.start();
            } catch (AWTException e) {
                e.printStackTrace();
            }
        });
    }

    public static boolean isRunning() { return running; }
}
