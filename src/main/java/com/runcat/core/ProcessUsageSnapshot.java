package com.runcat.core;

/**
 * Single process usage snapshot rendered in the dashboard leaderboards.
 */
public record ProcessUsageSnapshot(
        String name,
        int pid,
        double cpuPercent,
        double memoryMb,
        double diskReadKBps,
        double diskWriteKBps,
        double networkRecvKBps,
        double networkSendKBps) {

    public double diskTotalKBps() {
        return Math.max(0, diskReadKBps) + Math.max(0, diskWriteKBps);
    }

    public double networkTotalKBps() {
        return Math.max(0, networkRecvKBps) + Math.max(0, networkSendKBps);
    }

    public String displayName() {
        if (name == null || name.isBlank()) {
            return "pid-" + pid;
        }
        return name + " (" + pid + ")";
    }
}
