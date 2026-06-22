package com.runcat.ui;

import com.runcat.RunCatApp;
import com.runcat.config.AppConfig;
import com.runcat.core.SystemMonitor;
import com.runcat.i18n.I18nManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.LinkedList;

/**
 * Mini dashboard window - shown on left-click of tray icon
 * Displays CPU/memory usage with real-time chart
 */
public class DashboardWindow extends JDialog {

    private static DashboardWindow instance;
    private final SystemMonitor monitor;
    private final AppConfig config;

    private JLabel cpuLabel;
    private JLabel memLabel;
    private JLabel memDetailLabel;
    private ChartPanel cpuChart;
    private ChartPanel memChart;
    private Timer refreshTimer;

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
            instance.dispose();
        }
    }

    private DashboardWindow() {
        this.monitor = RunCatApp.getSystemMonitor();
        this.config = RunCatApp.config();
        I18nManager i18n = I18nManager.getInstance();

        setTitle(i18n.get("dashboard.title"));
        setLayout(new BorderLayout(8, 8));
        setSize(360, 340);
        setResizable(false);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setAlwaysOnTop(true);

        // Restore position
        if (config.getDashboardX() >= 0 && config.getDashboardY() >= 0) {
            setLocation(config.getDashboardX(), config.getDashboardY());
        } else {
            setLocationRelativeTo(null);
        }

        initUI();
        startRefresh();
    }

    private void initUI() {
        I18nManager i18n = I18nManager.getInstance();
        JPanel mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Top: current values
        JPanel topPanel = new JPanel(new GridLayout(2, 2, 8, 4));

        JLabel cpuTitle = new JLabel(i18n.get("dashboard.cpu") + ":");
        cpuTitle.setFont(cpuTitle.getFont().deriveFont(Font.BOLD));
        cpuLabel = new JLabel("--");
        cpuLabel.setFont(cpuLabel.getFont().deriveFont(Font.BOLD, 16f));
        cpuLabel.setForeground(new Color(0, 120, 215));

        JLabel memTitle = new JLabel(i18n.get("dashboard.memory") + ":");
        memTitle.setFont(memTitle.getFont().deriveFont(Font.BOLD));
        memLabel = new JLabel("--");
        memLabel.setFont(memLabel.getFont().deriveFont(Font.BOLD, 16f));
        memLabel.setForeground(new Color(180, 80, 0));

        topPanel.add(cpuTitle);
        topPanel.add(cpuLabel);
        topPanel.add(memTitle);
        topPanel.add(memLabel);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Center: charts
        JPanel chartPanel = new JPanel(new GridLayout(2, 1, 4, 8));
        cpuChart = new ChartPanel(new Color(0, 120, 215), i18n.get("dashboard.cpuHistory"));
        memChart = new ChartPanel(new Color(180, 80, 0), i18n.get("dashboard.memHistory"));
        chartPanel.add(cpuChart);
        chartPanel.add(memChart);
        mainPanel.add(chartPanel, BorderLayout.CENTER);

        // Bottom: memory detail
        memDetailLabel = new JLabel(" ");
        memDetailLabel.setFont(memDetailLabel.getFont().deriveFont(11f));
        memDetailLabel.setForeground(Color.GRAY);
        mainPanel.add(memDetailLabel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void startRefresh() {
        refreshTimer = new Timer(1000, e -> refreshData());
        refreshTimer.start();
        refreshData();
    }

    private void refreshData() {
        if (!isVisible()) return;
        cpuLabel.setText(monitor.getCpuUsageText());
        memLabel.setText(monitor.getMemoryUsageText());
        memDetailLabel.setText(monitor.getMemoryDetailText());
        cpuChart.setData(monitor.getCpuHistory());
        memChart.setData(monitor.getMemHistory());

        // Color-code CPU label
        double cpu = monitor.getCpuUsage();
        if (cpu > 90) cpuLabel.setForeground(Color.RED);
        else if (cpu > 70) cpuLabel.setForeground(new Color(255, 140, 0));
        else cpuLabel.setForeground(new Color(0, 120, 215));
    }

    @Override
    public void dispose() {
        if (refreshTimer != null) refreshTimer.stop();
        config.saveDashboardPosition(getX(), getY());
        instance = null;
        super.dispose();
    }

    /**
     * Simple chart panel that draws a line chart of history data
     */
    private static class ChartPanel extends JPanel {
        private LinkedList<Double> data = new LinkedList<>();
        private final Color lineColor;
        private final String title;

        ChartPanel(Color lineColor, String title) {
            this.lineColor = lineColor;
            this.title = title;
            setPreferredSize(new Dimension(320, 100));
            setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
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
            int padding = 4;

            // Draw title
            g2d.setColor(Color.GRAY);
            g2d.setFont(g2d.getFont().deriveFont(9f));
            g2d.drawString(title, padding, 12);

            // Draw grid lines (25%, 50%, 75%)
            g2d.setColor(new Color(240, 240, 240));
            int chartTop = 16;
            int chartBottom = h - padding;
            int chartHeight = chartBottom - chartTop;
            for (int pct : new int[]{25, 50, 75}) {
                int y = chartBottom - (int)(chartHeight * pct / 100.0);
                g2d.drawLine(padding, y, w - padding, y);
            }

            if (data.size() < 2) return;

            // Draw line chart
            g2d.setColor(lineColor);
            g2d.setStroke(new BasicStroke(1.5f));

            int dataLen = data.size();
            double xStep = (double)(w - 2 * padding) / (60 - 1);  // 60 slots

            int[] xPoints = new int[dataLen];
            int[] yPoints = new int[dataLen];

            for (int i = 0; i < dataLen; i++) {
                xPoints[i] = padding + (int)((60 - dataLen + i) * xStep);
                double val = Math.min(100, Math.max(0, data.get(i)));
                yPoints[i] = chartBottom - (int)(chartHeight * val / 100.0);
            }

            g2d.drawPolyline(xPoints, yPoints, dataLen);

            // Fill area under line
            g2d.setColor(new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), 40));
            int[] fillX = new int[dataLen + 2];
            int[] fillY = new int[dataLen + 2];
            System.arraycopy(xPoints, 0, fillX, 0, dataLen);
            System.arraycopy(yPoints, 0, fillY, 0, dataLen);
            fillX[dataLen] = xPoints[dataLen - 1];
            fillY[dataLen] = chartBottom;
            fillX[dataLen + 1] = xPoints[0];
            fillY[dataLen + 1] = chartBottom;
            g2d.fillPolygon(fillX, fillY, dataLen + 2);
        }
    }
}
