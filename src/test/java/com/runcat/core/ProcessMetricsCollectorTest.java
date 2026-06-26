package com.runcat.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessMetricsCollectorTest {

    @Test
    void psScriptContainsFormatString() {
        String script = ProcessMetricsCollector.getPsScript();
        assertTrue(script.contains("{0}|{1}|{2}|{3}|{4}|{5}"));
    }

    @Test
    void psScriptForcesUtf8ConsoleOutput() {
        String script = ProcessMetricsCollector.getPsScript();
        assertTrue(script.contains("[Console]::OutputEncoding = [System.Text.Encoding]::UTF8"));
    }

    @Test
    void sortsSnapshotsByCpuDescendingAndTruncatesToTopN() {
        List<ProcessUsageSnapshot> samples = List.of(
                new ProcessUsageSnapshot("a.exe", 1, 12.0, 100, 0, 0, 0, 0),
                new ProcessUsageSnapshot("b.exe", 2, 55.0, 80, 0, 0, 0, 0),
                new ProcessUsageSnapshot("c.exe", 3, 31.0, 60, 0, 0, 0, 0)
        );
        List<ProcessUsageSnapshot> top = ProcessMetricsCollector.topNByCpu(samples, 2);
        assertEquals(List.of("b.exe", "c.exe"), top.stream().map(ProcessUsageSnapshot::name).toList());
    }

    @Test
    void sortsSnapshotsByCombinedDiskThroughput() {
        List<ProcessUsageSnapshot> samples = List.of(
                new ProcessUsageSnapshot("slow.exe", 1, 0, 0, 10, 10, 0, 0),
                new ProcessUsageSnapshot("fast.exe", 2, 0, 0, 100, 50, 0, 0)
        );
        List<ProcessUsageSnapshot> top = ProcessMetricsCollector.topNByDisk(samples, 1);
        assertEquals("fast.exe", top.get(0).name());
    }
}
