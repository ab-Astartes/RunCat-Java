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
        // Parse CLI args
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

        // Single instance lock
        if (!acquireInstanceLock()) {
            JOptionPane.showMessageDialog(null,
                    "Java RunCat is already running.",
                    "Java RunCat", JOptionPane.WARNING_MESSAGE);
            System.exit(0);
        }

        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // fallback to default
        }

        // Check system tray support
        if (!SystemTray.isSupported()) {
            JOptionPane.showMessageDialog(null,
                    "System tray is not supported on this platform.",
                    "Java RunCat", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // Load configuration
        appConfig = AppConfig.load();

        // Initialize i18n
        I18nManager.getInstance().setLocale(appConfig.getLanguage());

        // Initialize animation manager
        animationManager = new AnimationManager(appConfig);

        // Initialize system monitor
        systemMonitor = new SystemMonitor();

        // Initialize tray icon
        SwingUtilities.invokeLater(() -> {
            try {
                trayIconManager = new TrayIconManager(appConfig, animationManager, systemMonitor);
                trayIconManager.start();

                // Start animation loop
                startAnimationLoop();

                // Start CPU monitor loop
                startCpuMonitorLoop();
            } catch (AWTException e) {
                JOptionPane.showMessageDialog(null,
                        "Failed to create tray icon: " + e.getMessage(),
                        "Java RunCat", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running = false;
            if (trayIconManager != null) {
                trayIconManager.stop();
            }
            releaseInstanceLock();
        }));
    }

    /**
     * Acquire a file lock to ensure single instance
     */
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

    private static void startAnimationLoop() {
        Thread animationThread = new Thread(() -> {
            while (running) {
                try {
                    double cpuUsage = systemMonitor.getCpuUsage();
                    double multiplier = config().getSpeedMultiplier();
                    // Base interval: 50ms at 100% CPU, 300ms at 0% CPU
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

    public static AppConfig config() {
        return appConfig;
    }

    public static SystemMonitor getSystemMonitor() {
        return systemMonitor;
    }

    public static AnimationManager getAnimationManager() {
        return animationManager;
    }

    public static void restart() {
        if (trayIconManager != null) {
            trayIconManager.stop();
        }
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

    public static boolean isRunning() {
        return running;
    }
}
