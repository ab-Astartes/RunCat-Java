package com.runcat.core;

import com.runcat.RunCatApp;
import com.runcat.config.AppConfig;
import com.runcat.i18n.I18nManager;

import java.awt.*;
import java.util.LinkedList;

/**
 * Monitors system CPU and memory usage with history tracking and alerts
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

    public SystemMonitor() {
        this.osBean = (com.sun.management.OperatingSystemMXBean)
                java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        this.cpuUsage = 0;
        this.memoryUsage = 0;
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

        // Check CPU alert
        checkCpuAlert();
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
            // Use system tray notification
            if (SystemTray.isSupported()) {
                try {
                    SystemTray tray = SystemTray.getSystemTray();
                    // Use a temporary TrayIcon for the notification
                    Image img = Toolkit.getDefaultToolkit().createImage("");
                    TrayIcon notifyIcon = new TrayIcon(img);
                    notifyIcon.setImageAutoSize(true);
                    tray.add(notifyIcon);
                    notifyIcon.displayMessage(
                            i18n.get("notification.cpuHigh.title"),
                            i18n.get("notification.cpuHigh", getCpuUsageText()),
                            TrayIcon.MessageType.WARNING);
                    // Remove after a short delay
                    new Thread(() -> {
                        try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
                        tray.remove(notifyIcon);
                    }).start();
                } catch (Exception ignored) {}
            }
        }
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public double getMemoryUsage() {
        return memoryUsage;
    }

    public String getCpuUsageText() {
        return String.format("%.1f%%", cpuUsage);
    }

    public String getMemoryUsageText() {
        return String.format("%.1f%%", memoryUsage);
    }

    public LinkedList<Double> getCpuHistory() {
        return new LinkedList<>(cpuHistory);
    }

    public LinkedList<Double> getMemHistory() {
        return new LinkedList<>(memHistory);
    }

    /**
     * Get formatted memory info (used / total in MB)
     */
    public String getMemoryDetailText() {
        long totalMem = osBean.getTotalMemorySize();
        long freeMem = osBean.getFreeMemorySize();
        long usedMem = totalMem - freeMem;
        return String.format("%.0f MB / %.0f MB", usedMem / 1e6, totalMem / 1e6);
    }
}
