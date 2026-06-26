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
 * Dashboard window with system overview, history charts and top process leaderboards.
 * Zoom/minimize/close controls in the title bar (top-right), following standard software conventions.
 */
public class DashboardWindow extends JDialog {

    private static DashboardWindow instance;
    private final SystemMonitor monitor;
    private final AppConfig config;

    // Size levels: compact, normal, expanded
    private static final int[][] SIZE_LEVELS = {
            {420, 520},   // 0: compact
            {560, 740},   // 1: normal (default)
            {740, 960},   // 2: expanded
    };
    private int currentSizeLevel = 1;

    private JLabel cpuLabel;
    private JLabel memLabel;
    private JLabel memDetailLabel;
    private JLabel diskLabel;
    private JLabel netLabel;
    private ChartPanel cpuChart;
    private ChartPanel memChart;
    private Timer refreshTimer;
    private final Map<ProcessMetricType, ProcessListPanel> processPanels = new LinkedHashMap<>();
    private JButton zoomInBtn;
    private JButton zoomOutBtn;
    private JButton minimizeBtn;

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
        if (instance != null) {
            instance.setVisible(false);
        }
    }

    public static void showInstance() {
        if (instance != null) {
            instance.setVisible(true);
            instance.toFront();
            instance.requestFocus();
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

        // Handle close button = hide to tray
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                hideToTray();
            }
        });

        initUI();
        startRefresh();
    }

    private void applySizeLevel() {
        int[] size = SIZE_LEVELS[currentSizeLevel];
        setSize(size[0], size[1]);
        // Update button states
        if (zoomInBtn != null) {
            zoomInBtn.setEnabled(currentSizeLevel < SIZE_LEVELS.length - 1);
        }
        if (zoomOutBtn != null) {
            zoomOutBtn.setEnabled(currentSizeLevel > 0);
        }
    }

    private void hideToTray() {
        setVisible(false);
    }

    // ---- UI Construction ----

    private void initUI() {
        I18nManager i18n = I18nManager.getInstance();
        JPanel mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 8, 10));
        mainPanel.setBackground(new Color(248, 250, 252));

        // Title bar with controls on the right (standard layout)
        mainPanel.add(createTitleBar(i18n), BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.setOpaque(false);

        centerPanel.add(createSummaryPanel(i18n), BorderLayout.NORTH);

        // Charts
        JPanel chartContainer = new JPanel(new GridLayout(2, 1, 0, 8));
        chartContainer.setOpaque(false);
        cpuChart = new ChartPanel(new Color(0, 120, 215), i18n.get("dashboard.cpuHistory"));
        memChart = new ChartPanel(new Color(212, 120, 52), i18n.get("dashboard.memHistory"));
        chartContainer.add(cpuChart);
        chartContainer.add(memChart);
        centerPanel.add(chartContainer, BorderLayout.CENTER);

        // Process leaderboard panels in a 2x2 grid
        JPanel processesPanel = new JPanel(new GridLayout(2, 2, 6, 6));
        processesPanel.setOpaque(false);
        registerProcessPanel(processesPanel, ProcessMetricType.CPU, i18n.get("dashboard.topCpu"));
        registerProcessPanel(processesPanel, ProcessMetricType.MEMORY, i18n.get("dashboard.topMemory"));
        registerProcessPanel(processesPanel, ProcessMetricType.DISK, i18n.get("dashboard.topDisk"));
        registerProcessPanel(processesPanel, ProcessMetricType.NETWORK, i18n.get("dashboard.topNetwork"));
        centerPanel.add(processesPanel, BorderLayout.SOUTH);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Bottom bar: memory detail only
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setOpaque(false);
        memDetailLabel = new JLabel(" ");
        memDetailLabel.setFont(memDetailLabel.getFont().deriveFont(11f));
        memDetailLabel.setForeground(new Color(95, 105, 120));
        bottomBar.add(memDetailLabel, BorderLayout.WEST);
        mainPanel.add(bottomBar, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    /** Title bar: title on the left, −/+/minimize on the right (standard software convention). */
    private JPanel createTitleBar(I18nManager i18n) {
        JPanel titleBar = new JPanel(new BorderLayout(8, 0));
        titleBar.setOpaque(false);

        // Left: title + subtitle + LIVE badge
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

        // Right: − zoom out, + zoom in, ▼ minimize (standard top-right controls)
        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightControls.setOpaque(false);

        zoomOutBtn = createTitleBarButton("−", i18n.get("dashboard.zoomOut"));
        zoomOutBtn.setEnabled(currentSizeLevel > 0);
        zoomOutBtn.addActionListener(e -> {
            if (currentSizeLevel > 0) {
                currentSizeLevel--;
                applySizeLevel();
            }
        });

        zoomInBtn = createTitleBarButton("+", i18n.get("dashboard.zoomIn"));
        zoomInBtn.setEnabled(currentSizeLevel < SIZE_LEVELS.length - 1);
        zoomInBtn.addActionListener(e -> {
            if (currentSizeLevel < SIZE_LEVELS.length - 1) {
                currentSizeLevel++;
                applySizeLevel();
            }
        });

        minimizeBtn = createTitleBarButton("▼", i18n.get("dashboard.hide"));
        minimizeBtn.setForeground(new Color(100, 108, 118));
        minimizeBtn.addActionListener(e -> hideToTray());

        rightControls.add(zoomOutBtn);
        rightControls.add(zoomInBtn);
        rightControls.add(minimizeBtn);
        titleBar.add(rightControls, BorderLayout.EAST);

        return titleBar;
    }

    /** Small button styled like title-bar controls (common software convention). */
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
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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

    private JComponent createMetricCard(String titleText, JLabel valueLabel, Color background, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setOpaque(true);
        card.setBackground(background);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 230, 236)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = createMetricTitle(titleText);
        JPanel dot = new JPanel();
        dot.setPreferredSize(new Dimension(10, 10));
        dot.setOpaque(true);
        dot.setBackground(accent);
        top.add(title, BorderLayout.WEST);
        top.add(dot, BorderLayout.EAST);

        card.add(top, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JLabel createMetricTitle(String text) {
        JLabel label = new JLabel(text + ":");
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
        label.setForeground(new Color(70, 78, 90));
        return label;
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

    // ---- Data Refresh ----

    private void startRefresh() {
        refreshTimer = new Timer(config.getDashboardRefreshMs(), e -> refreshData());
        refreshTimer.start();
        // Immediate first refresh for CPU/mem/disk/net labels
        refreshData();
    }

    private void refreshData() {
        if (!isVisible()) return;

        cpuLabel.setText(monitor.getCpuUsageText());
        memLabel.setText(monitor.getMemoryUsageText());
        memDetailLabel.setText(monitor.getMemoryDetailText());
        diskLabel.setText(monitor.getDiskUsageText());
        netLabel.setText(monitor.getNetworkUsageText());
        cpuChart.setData(monitor.getCpuHistory());
        memChart.setData(monitor.getMemHistory());

        double cpu = monitor.getCpuUsage();
        if (cpu > 90) cpuLabel.setForeground(new Color(210, 45, 45));
        else if (cpu > 70) cpuLabel.setForeground(new Color(255, 140, 0));
        else cpuLabel.setForeground(new Color(0, 120, 215));

        // Update leaderboard from snapshot
        MonitorSnapshot snapshot = monitor.getSnapshot();
        List<ProcessUsageSnapshot> cpuList = snapshot.topCpuProcesses();
        List<ProcessUsageSnapshot> memList = snapshot.topMemoryProcesses();
        List<ProcessUsageSnapshot> diskList = snapshot.topDiskProcesses();
        List<ProcessUsageSnapshot> netList = snapshot.topNetworkProcesses();

        processPanels.get(ProcessMetricType.CPU).setProcesses(cpuList);
        processPanels.get(ProcessMetricType.MEMORY).setProcesses(memList);
        processPanels.get(ProcessMetricType.DISK).setProcesses(diskList);
        processPanels.get(ProcessMetricType.NETWORK).setProcesses(netList);
    }

    @Override
    public void dispose() {
        if (refreshTimer != null) refreshTimer.stop();
        config.saveDashboardPosition(getX(), getY());
        instance = null;
        super.dispose();
    }

    // ---- Enums & Inner Classes ----

    private enum ProcessMetricType {
        CPU, MEMORY, DISK, NETWORK
    }

    private static class ProcessListPanel extends JScrollPane {
        private final ProcessMetricType type;
        private final JPanel innerPanel;
        private final JLabel titleLabel;
        private final JPanel rowsPanel;
        private final JLabel emptyLabel;

        ProcessListPanel(String title, ProcessMetricType type) {
            this.type = type;

            innerPanel = new JPanel(new BorderLayout(4, 4));
            innerPanel.setOpaque(true);
            innerPanel.setBackground(Color.WHITE);

            titleLabel = new JLabel(title);
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));
            titleLabel.setForeground(new Color(38, 45, 56));
            titleLabel.setBorder(BorderFactory.createEmptyBorder(4, 6, 2, 6));
            innerPanel.add(titleLabel, BorderLayout.NORTH);

            rowsPanel = new JPanel();
            rowsPanel.setLayout(new BoxLayout(rowsPanel, BoxLayout.Y_AXIS));
            rowsPanel.setOpaque(false);
            rowsPanel.setBorder(BorderFactory.createEmptyBorder(0, 6, 4, 6));
            innerPanel.add(rowsPanel, BorderLayout.CENTER);

            emptyLabel = new JLabel(I18nManager.getInstance().get("monitor.notAvailable"));
            emptyLabel.setFont(emptyLabel.getFont().deriveFont(Font.PLAIN, 10f));
            emptyLabel.setForeground(new Color(130, 135, 145));
            emptyLabel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
            innerPanel.add(emptyLabel, BorderLayout.SOUTH);

            setViewportView(innerPanel);
            setBorder(BorderFactory.createLineBorder(new Color(220, 225, 230)));
            setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            setPreferredSize(new Dimension(250, 120));
            getVerticalScrollBar().setUnitIncrement(12);
        }

        void setProcesses(List<ProcessUsageSnapshot> processes) {
            rowsPanel.removeAll();
            if (processes == null || processes.isEmpty()) {
                emptyLabel.setVisible(true);
            } else {
                emptyLabel.setVisible(false);
                int index = 0;
                for (ProcessUsageSnapshot process : processes) {
                    rowsPanel.add(createRow(process, index++));
                    rowsPanel.add(Box.createVerticalStrut(4));
                }
            }
            revalidate();
            repaint();
        }

        private JComponent createRow(ProcessUsageSnapshot process, int index) {
            JPanel row = new JPanel(new BorderLayout(2, 1));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

            JLabel nameLbl = new JLabel((index + 1) + ". " + process.displayName());
            nameLbl.setFont(nameLbl.getFont().deriveFont(Font.PLAIN, 10f));
            nameLbl.setToolTipText(process.displayName());

            JLabel valLbl = new JLabel(metricText(process));
            valLbl.setFont(valLbl.getFont().deriveFont(Font.BOLD, 10f));
            valLbl.setForeground(metricColor(index));

            row.add(nameLbl, BorderLayout.CENTER);
            row.add(valLbl, BorderLayout.EAST);

            // Thin progress bar
            JProgressBar pb = new JProgressBar(0, 100);
            pb.setValue(metricPercent(process));
            pb.setForeground(metricColor(index));
            pb.setBackground(new Color(235, 239, 244));
            pb.setBorderPainted(false);
            pb.setPreferredSize(new Dimension(Integer.MAX_VALUE, 4));
            pb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));

            JPanel rowWithBar = new JPanel(new BorderLayout(2, 1));
            rowWithBar.setOpaque(false);
            rowWithBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            rowWithBar.add(row, BorderLayout.NORTH);
            rowWithBar.add(pb, BorderLayout.SOUTH);

            return rowWithBar;
        }

        private Color metricColor(int index) {
            return switch (type) {
                case CPU -> index == 0 ? new Color(0, 120, 215) : new Color(70, 146, 223);
                case MEMORY -> index == 0 ? new Color(212, 120, 52) : new Color(221, 152, 88);
                case DISK -> index == 0 ? new Color(110, 110, 110) : new Color(150, 150, 150);
                case NETWORK -> index == 0 ? new Color(0, 150, 110) : new Color(55, 170, 130);
            };
        }

        private int metricPercent(ProcessUsageSnapshot process) {
            return switch (type) {
                case CPU -> clampPercent(process.cpuPercent());
                case MEMORY -> clampPercent(Math.min(100, process.memoryMb() / 256.0));
                case DISK -> clampPercent(Math.min(100, process.diskTotalKBps() / 100.0));
                case NETWORK -> clampPercent(Math.min(100, process.networkTotalKBps() / 100.0));
            };
        }

        private int clampPercent(double value) {
            return (int) Math.max(0, Math.min(100, Math.round(value)));
        }

        private String metricText(ProcessUsageSnapshot process) {
            return switch (type) {
                case CPU -> String.format("%.1f%%", process.cpuPercent());
                case MEMORY -> String.format("%.0f MB", process.memoryMb());
                case DISK -> String.format("%.0f KB/s", process.diskTotalKBps());
                case NETWORK -> String.format("%.0f KB/s", process.networkTotalKBps());
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

            int w = getWidth();
            int h = getHeight();
            int padding = 8;
            int chartTop = 18;
            int chartBottom = h - padding;
            int chartHeight = chartBottom - chartTop;

            g2d.setColor(new Color(110, 118, 130));
            g2d.setFont(g2d.getFont().deriveFont(Font.PLAIN, 10f));
            g2d.drawString(title, padding, 14);

            g2d.setColor(new Color(238, 241, 245));
            for (int pct : new int[]{25, 50, 75}) {
                int y = chartBottom - (int) (chartHeight * pct / 100.0);
                g2d.drawLine(padding, y, w - padding, y);
            }

            if (data.size() < 2) return;

            g2d.setColor(lineColor);
            g2d.setStroke(new BasicStroke(1.6f));

            int dataLen = data.size();
            double xStep = (double) (w - 2 * padding) / Math.max(1, 60 - 1);
            int[] xPoints = new int[dataLen];
            int[] yPoints = new int[dataLen];
            for (int i = 0; i < dataLen; i++) {
                xPoints[i] = padding + (int) ((60 - dataLen + i) * xStep);
                double val = Math.min(100, Math.max(0, data.get(i)));
                yPoints[i] = chartBottom - (int) (chartHeight * val / 100.0);
            }

            int[] fillX = new int[dataLen + 2];
            int[] fillY = new int[dataLen + 2];
            System.arraycopy(xPoints, 0, fillX, 0, dataLen);
            System.arraycopy(yPoints, 0, fillY, 0, dataLen);
            fillX[dataLen] = xPoints[dataLen - 1];
            fillY[dataLen] = chartBottom;
            fillX[dataLen + 1] = xPoints[0];
            fillY[dataLen + 1] = chartBottom;
            g2d.setColor(new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 36));
            g2d.fillPolygon(fillX, fillY, dataLen + 2);

            g2d.setColor(lineColor);
            g2d.drawPolyline(xPoints, yPoints, dataLen);

            g2d.fillOval(xPoints[dataLen - 1] - 3, yPoints[dataLen - 1] - 3, 7, 7);
        }
    }
}
