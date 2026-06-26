package com.runcat.core;

import java.util.List;

/**
 * Unified monitoring snapshot consumed by tray, pet and dashboard UIs.
 */
public record MonitorSnapshot(
        long timestamp,
        double cpuUsage,
        double memoryUsage,
        double diskReadKBps,
        double diskWriteKBps,
        double networkRecvKBps,
        double networkSendKBps,
        List<Double> cpuHistory,
        List<Double> memoryHistory,
        List<ProcessUsageSnapshot> topCpuProcesses,
        List<ProcessUsageSnapshot> topMemoryProcesses,
        List<ProcessUsageSnapshot> topDiskProcesses,
        List<ProcessUsageSnapshot> topNetworkProcesses) {

    public static MonitorSnapshot empty() {
        return new MonitorSnapshot(
                System.currentTimeMillis(),
                0, 0, 0, 0, 0, 0,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
