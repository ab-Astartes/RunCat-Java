package com.runcat.core;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

/**
 * Monitors system CPU and memory usage
 */
public class SystemMonitor {

    private final OperatingSystemMXBean osBean;
    private double cpuUsage;
    private double memoryUsage;

    public SystemMonitor() {
        this.osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
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
}
