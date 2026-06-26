package com.runcat.ui;

import com.runcat.RunCatApp;
import com.runcat.config.AppConfig;
import com.runcat.core.MonitorSnapshot;
import com.runcat.core.ProcessUsageSnapshot;
import com.runcat.core.SystemMonitor;
import com.runcat.i18n.I18nManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Dashboard window — lightweight, no JProgressBar (avoids OOM on HiDPI).
 * Title bar: − / + / ▼ controls (top-right, standard convention).
 */
public class DashboardWindow extends JDialog {

    private static DashboardWindow instance;
    private final SystemMonitor monitor;
    private final AppConfig config;

    private static final int[][] SIZE_LEVELS = {
            {420, 520},   // 0: compact
            {560, 740},   // 1: normal (default)
            {740, 960},   // 2: expanded
    };
    private int currentSizeLevel = 1;

    private JLabel cpuLabel, memLabel, diskLabel, netLabel, memDetailLabel;
    private ChartPanel cpuChart, memChart;
    private Timer refreshTimer;
    private final Map<ProcessMetricType, ProcessListPanel> processPanels = new LinkedHashMap<>();
    private JButton zoomInBtn, zoomOutBtn, minimizeBtn;

    public static void showOrFocus() {
        if (instance != null && instance.isVisible()) {
            instance.toFront();
            instance.requestFocus();
            return;
        }
        instance = new DashboardWindow();
        instance.setVisible(true);
    }

    public static void hideInstance() {
        if (instance != null) instance.setVisible(false);
    }

    public static void showInstance() {
        if (instance != null) {
            instance.setVisible(true);
            instance.toFront();
        } else {
            showOrFocus();
        }
    }

    public static boolean isDashboardVisible() {
        return instance != null && instance.isVisible();
    }

    private DashboardWindow() {
        this.monitor = RunCatApp.getSystemMonitor();
        this.config = RunCatApp.config();
        I18nManager i18n = I18nManager.getInstance();

        setTitle(i18n.get("dashboard.title"));
        setLayout(new BorderLayout(0, 0));
        applySizeLevel();
        setResizable(true);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        setAlwaysOnTop(true);

        if (config.getDashboardX() >= 0 && config.getDashboardY() >= 0) {
            setLocation(config.getDashboardX(), config.getDashboardY());
        } else {
            setLocationRelativeTo(null);
        }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) { setVisible(false); }
        });

        initUI();
        startRefresh();
    }

    private void applySizeLevel() {
        int[] size = SIZE_LEVELS[currentSizeLevel];
        setSize(size[0], size[1]);
        if (zoomInBtn != null) zoomInBtn.setEnabled(currentSizeLevel < SIZE_LEVELS.length - 1);
        if (zoomOutBtn != null) zoomOutBtn.setEnabled(currentSizeLevel > 0);
    }

    // ---- UI Construction ----

    private void initUI() {
        I18nManager i18n = I18nManager.getInstance();
        JPanel mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 8, 10));
        mainPanel.setBackground(new Color(248, 250, 252));

        mainPanel.add(createTitleBar(i18n), BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.setOpaque(false);

        centerPanel.add(createSummaryPanel(i18n), BorderLayout.NORTH);

        JPanel chartContainer = new JPanel(new GridLayout(2, 1, 0, 8));
        chartContainer.setOpaque(false);
        cpuChart = new ChartPanel(new Color(0, 120, 215), i18n.get("dashboard.cpuHistory"));
        memChart = new ChartPanel(new Color(212, 120, 52), i18n.get("dashboard.memHistory"));
        chartContainer.add(cpuChart);
        chartContainer.add(memChart);
        centerPanel.add(chartContainer, BorderLayout.CENTER);

        JPanel processesPanel = new JPanel(new GridLayout(2, 2, 6, 6));
        processesPanel.setOpaque(false);
        registerProcessPanel(processesPanel, ProcessMetricType.CPU, i18n.get("dashboard.topCpu"));
        registerProcessPanel(processesPanel, ProcessMetricType.MEMORY, i18n.get("dashboard.topMemory"));
        registerProcessPanel(processesPanel, ProcessMetricType.DISK, i18n.get("dashboard.topDisk"));
        registerProcessPanel(processesPanel, ProcessMetricType.NETWORK, i18n.get("dashboard.topNetwork"));
        centerPanel.add(processesPanel, BorderLayout.SOUTH);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        memDetailLabel = new JLabel(" ");
        memDetailLabel.setFont(memDetailLabel.getFont().deriveFont(11f));
        memDetailLabel.setForeground(new Color(95, 105, 120));
        mainPanel.add(memDetailLabel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createTitleBar(I18nManager i18n) {
        JPanel titleBar = new JPanel(new BorderLayout(8, 0));
        titleBar.setOpaque(false);

        JPanel leftBlock = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftBlock.setOpaque(false);
        JLabel title = new JLabel(i18n.get("dashboard.title"));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(new Color(28, 34, 42));
        JLabel subtitle = new JLabel(i18n.get("dashboard.subtitle"));
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 11f));
        subtitle.setForeground(new Color(100, 110, 124));
        JLabel badge = new JLabel("LIVE");
        badge.setOpaque(true);
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        badge.setFont(badge.getFont().deriveFont(Font.BOLD, 10f));
        badge.setForeground(Color.WHITE);
        badge.setBackground(new Color(22, 163, 74));
        badge.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        leftBlock.add(title);
        leftBlock.add(subtitle);
        leftBlock.add(badge);
        titleBar.add(leftBlock, BorderLayout.WEST);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightControls.setOpaque(false);
        zoomOutBtn = createTitleBarButton("−", i18n.get("dashboard.zoomOut"));
        zoomOutBtn.addActionListener(e -> { if (currentSizeLevel > 0) { currentSizeLevel--; applySizeLevel(); } });
        zoomInBtn = createTitleBarButton("+", i18n.get("dashboard.zoomIn"));
        zoomInBtn.addActionListener(e -> { if (currentSizeLevel < SIZE_LEVELS.length - 1) { currentSizeLevel++; applySizeLevel(); } });
        minimizeBtn = createTitleBarButton("▼", i18n.get("dashboard.hide"));
        minimizeBtn.setForeground(new Color(100, 108, 118));
        minimizeBtn.addActionListener(e -> setVisible(false));
        rightControls.add(zoomOutBtn);
        rightControls.add(zoomInBtn);
        rightControls.add(minimizeBtn);
        titleBar.add(rightControls, BorderLayout.EAST);

        return titleBar;
    }

    private JButton createTitleBarButton(String text, String tooltip) {
        JButton btn = new JButton(text);
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 13f));
        btn.setForeground(new Color(60, 65, 75));
        btn.setBackground(new Color(240, 243, 248));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 205, 215)),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(36, 24));
        btn.setToolTipText(tooltip);
        return btn;
    }

    private JPanel createSummaryPanel(I18nManager i18n) {
        JPanel panel = new JPanel(new GridLayout(1, 4, 8, 8));
        panel.setOpaque(false);
        cpuLabel = createMetricValue(new Color(0, 120, 215));
        diskLabel = createMetricValue(new Color(105, 105, 105));
        memLabel = createMetricValue(new Color(212, 120, 52));
        netLabel = createMetricValue(new Color(0, 150, 110));
        panel.add(createMetricCard(i18n.get("dashboard.cpu"), cpuLabel, new Color(232, 242, 255), new Color(0, 120, 215)));
        panel.add(createMetricCard(i18n.get("dashboard.disk"), diskLabel, new Color(245, 245, 245), new Color(105, 105, 105)));
        panel.add(createMetricCard(i18n.get("dashboard.memory"), memLabel, new Color(255, 243, 232), new Color(212, 120, 52)));
        panel.add(createMetricCard(i18n.get("dashboard.network"), netLabel, new Color(232, 249, 243), new Color(0, 150, 110)));
        return panel;
    }

    private JComponent createMetricCard(String titleText, JLabel valueLabel, Color bg, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setOpaque(true);
        card.setBackground(bg);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 230, 236)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JLabel title = new JLabel(titleText + ":");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 12f));
        title.setForeground(new Color(70, 78, 90));
        card.add(title, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JLabel createMetricValue(Color color) {
        JLabel label = new JLabel("--");
        label.setFont(label.getFont().deriveFont(Font.BOLD, 18f));
        label.setForeground(color);
        return label;
    }

    private void registerProcessPanel(JPanel container, ProcessMetricType type, String title) {
        ProcessListPanel panel = new ProcessListPanel(title, type);
        processPanels.put(type, panel);
        container.add(panel);
    }

    // ---- Data Refresh (lightweight, no JProgressBar) ----

    private void startRefresh() {
        refreshTimer = new Timer(config.getDashboardRefreshMs(), e -> refreshData());
        refreshTimer.start();
        refreshData();
    }

    private void refreshData() {
        if (!isVisible()) return;
        try {
            cpuLabel.setText(monitor.getCpuUsageText());
            memLabel.setText(monitor.getMemoryUsageText());
            memDetailLabel.setText(monitor.getMemoryDetailText());
            diskLabel.setText(monitor.getDiskUsageText());
            netLabel.setText(monitor.getNetworkUsageText());
            cpuChart.setData(monitor.getCpuHistory());
            memChart.setData(monitor.getMemHistory());

            double cpu = monitor.getCpuUsage();
            cpuLabel.setForeground(cpu > 90 ? new Color(210, 45, 45)
                    : cpu > 70 ? new Color(255, 140, 0) : new Color(0, 120, 215));

            MonitorSnapshot snapshot = monitor.getSnapshot();
            processPanels.get(ProcessMetricType.CPU).setProcesses(snapshot.topCpuProcesses());
            processPanels.get(ProcessMetricType.MEMORY).setProcesses(snapshot.topMemoryProcesses());
            processPanels.get(ProcessMetricType.DISK).setProcesses(snapshot.topDiskProcesses());
            processPanels.get(ProcessMetricType.NETWORK).setProcesses(snapshot.topNetworkProcesses());
        } catch (Exception ignored) {
            // Prevent OOM cascade from killing the entire refresh loop
        }
    }

    @Override
    public void dispose() {
        if (refreshTimer != null) refreshTimer.stop();
        config.saveDashboardPosition(getX(), getY());
        instance = null;
        super.dispose();
    }

    // ---- Enums & Inner Classes ----

    private enum ProcessMetricType { CPU, MEMORY, DISK, NETWORK }

    /** Lightweight process list — pure JLabel rows, NO JProgressBar. */
    private static class ProcessListPanel extends JScrollPane {
        private final ProcessMetricType type;
        private final JPanel innerPanel;
        private final JLabel titleLabel;
        private final JLabel[] rowLabels = new JLabel[5]; // fixed 5 rows
        private final JLabel emptyLabel;

        ProcessListPanel(String title, ProcessMetricType type) {
            this.type = type;

            innerPanel = new JPanel();
            innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
            innerPanel.setOpaque(true);
            innerPanel.setBackground(Color.WHITE);

            titleLabel = new JLabel(title);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));
            titleLabel.setForeground(new Color(38, 45, 56));
            titleLabel.setBorder(BorderFactory.createEmptyBorder(4, 6, 2, 6));
            innerPanel.add(titleLabel);

            for (int i = 0; i < rowLabels.length; i++) {
                rowLabels[i] = new JLabel(" ");
                rowLabels[i].setFont(rowLabels[i].getFont().deriveFont(Font.PLAIN, 10f));
                rowLabels[i].setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 6));
                innerPanel.add(rowLabels[i]);
            }

            emptyLabel = new JLabel(I18nManager.getInstance().get("monitor.notAvailable"));
            emptyLabel.setFont(emptyLabel.getFont().deriveFont(Font.PLAIN, 10f));
            emptyLabel.setForeground(new Color(130, 135, 145));
            emptyLabel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
            innerPanel.add(emptyLabel);

            setViewportView(innerPanel);
            setBorder(BorderFactory.createLineBorder(new Color(220, 225, 230)));
            setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            setPreferredSize(new Dimension(250, 120));
        }

        void setProcesses(List<ProcessUsageSnapshot> processes) {
            if (processes == null || processes.isEmpty()) {
                emptyLabel.setVisible(true);
                for (JLabel row : rowLabels) row.setText(" ");
            } else {
                emptyLabel.setVisible(false);
                for (int i = 0; i < rowLabels.length; i++) {
                    if (i < processes.size()) {
                        ProcessUsageSnapshot p = processes.get(i);
                        rowLabels[i].setText((i + 1) + ". " + p.displayName() + "  " + metricText(p));
                    } else {
                        rowLabels[i].setText(" ");
                    }
                }
            }
        }

        private String metricText(ProcessUsageSnapshot p) {
            return switch (type) {
                case CPU -> String.format("%.1f%%", p.cpuPercent());
                case MEMORY -> String.format("%.0f MB", p.memoryMb());
                case DISK -> String.format("%.0f KB/s", p.diskTotalKBps());
                case NETWORK -> String.format("%.0f KB/s", p.networkTotalKBps());
            };
        }
    }

    private static class ChartPanel extends JPanel {
        private LinkedList<Double> data = new LinkedList<>();
        private final Color lineColor;
        private final String title;

        ChartPanel(Color lineColor, String title) {
            this.lineColor = lineColor;
            this.title = title;
            setPreferredSize(new Dimension(520, 100));
            setBorder(BorderFactory.createLineBorder(new Color(220, 225, 230)));
            setBackground(Color.WHITE);
        }

        void setData(LinkedList<Double> data) {
            this.data = data;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight(), pad = 8;
            int chartTop = 18, chartBottom = h - pad, chartH = chartBottom - chartTop;

            g2d.setColor(new Color(110, 118, 130));
            g2d.setFont(g2d.getFont().deriveFont(Font.PLAIN, 10f));
            g2d.drawString(title, pad, 14);

            g2d.setColor(new Color(238, 241, 245));
            for (int pct : new int[]{25, 50, 75}) {
                int y = chartBottom - (int)(chartH * pct / 100.0);
                g2d.drawLine(pad, y, w - pad, y);
            }

            if (data.size() < 2) return;

            g2d.setColor(lineColor);
            g2d.setStroke(new BasicStroke(1.6f));

            double xStep = (double)(w - 2*pad) / 59;
            int n = data.size();
            int[] xs = new int[n], ys = new int[n];
            for (int i = 0; i < n; i++) {
                xs[i] = pad + (int)((60 - n + i) * xStep);
                ys[i] = chartBottom - (int)(chartH * Math.min(100, Math.max(0, data.get(i))) / 100.0);
            }

            // Fill area
            int[] fillX = new int[n+2], fillY = new int[n+2];
            System.arraycopy(xs, 0, fillX, 0, n);
            System.arraycopy(ys, 0, fillY, 0, n);
            fillX[n] = xs[n-1]; fillY[n] = chartBottom;
            fillX[n+1] = xs[0]; fillY[n+1] = chartBottom;
            g2d.setColor(new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 36));
            g2d.fillPolygon(fillX, fillY, n+2);

            g2d.setColor(lineColor);
            g2d.drawPolyline(xs, ys, n);
            g2d.fillOval(xs[n-1]-3, ys[n-1]-3, 7, 7);
        }
    }
}
