package com.runcat.ui;

import com.runcat.RunCatApp;
import com.runcat.animation.AnimationManager;
import com.runcat.config.AppConfig;
import com.runcat.core.SystemMonitor;
import com.runcat.i18n.I18nManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.LinkedList;

/**
 * Desktop Pet Window - floating always-on-top pet like QQ Pet
 * 
 * Features:
 * - Frameless, transparent window with the pet animation
 * - Draggable to any position on screen
 * - Rounded shadow/glow underneath
 * - Right-click context menu
 * - Double-click to open dashboard
 * - Configurable size (64/96/128 px)
 * - Configurable opacity
 * - Position persisted across restarts
 */
public class DesktopPetWindow extends JWindow {

    private static DesktopPetWindow instance;
    private final AppConfig config;
    private final AnimationManager animManager;
    private final SystemMonitor systemMonitor;

    private JLabel petLabel;
    private JPanel contentPanel;
    private Timer animTimer;
    private Point dragStart;
    private boolean dragging = false;

    private int currentSize;
    private ImageIcon currentFrameIcon;

    public static void showOrFocus() {
        if (instance != null && instance.isVisible()) {
            instance.toFront();
            return;
        }
        instance = new DesktopPetWindow();
        instance.setVisible(true);
    }

    public static void hideInstance() {
        if (instance != null) {
            instance.stopAnimation();
            instance.dispose();
            instance = null;
        }
    }

    public static boolean isPetShowing() {
        return instance != null && instance.isVisible();
    }

    public static void refreshIfShowing() {
        if (instance != null && instance.isVisible()) {
            instance.refreshAppearance();
        }
    }

    private DesktopPetWindow() {
        this.config = RunCatApp.config();
        this.animManager = RunCatApp.getAnimationManager();
        this.systemMonitor = RunCatApp.getSystemMonitor();
        this.currentSize = config.getDesktopPetSize();

        initUI();
        startAnimation();
    }

    private void initUI() {
        // Transparent background
        setBackground(new Color(0, 0, 0, 0));

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);

        // Pet label with icon
        petLabel = new JLabel();
        petLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        petLabel.setHorizontalAlignment(SwingConstants.CENTER);
        petLabel.setVerticalAlignment(SwingConstants.CENTER);

        // Wrap pet label in a panel that draws shadow
        JPanel shadowPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Draw soft glow/shadow under the pet
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int shadowSize = currentSize + 16;
                int cx = getWidth() / 2;
                int cy = getHeight() / 2 + 8;
                // Elliptical shadow
                for (int i = 3; i >= 0; i--) {
                    int alpha = 15 + i * 8;
                    int expand = i * 4;
                    g2d.setColor(new Color(0, 0, 0, alpha));
                    g2d.fill(new Ellipse2D.Double(
                            cx - shadowSize / 2.0 - expand,
                            cy - shadowSize / 6.0 - expand / 2.0,
                            shadowSize + expand * 2,
                            shadowSize / 3.0 + expand));
                }
                g2d.dispose();
            }
        };
        shadowPanel.setOpaque(false);
        shadowPanel.add(petLabel, BorderLayout.CENTER);

        contentPanel.add(shadowPanel, BorderLayout.CENTER);

        // Tooltip showing CPU/Memory
        petLabel.setToolTipText(buildTooltip());

        // Mouse listeners for drag + right-click
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    dragStart = SwingUtilities.convertPoint(DesktopPetWindow.this, e.getPoint(), contentPanel);
                    dragging = false;
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStart != null) {
                    Point current = e.getLocationOnScreen();
                    setLocation(current.x - dragStart.x, current.y - dragStart.y);
                    dragging = true;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && !dragging) {
                    // Single click - show tooltip or bounce effect
                    bouncePet();
                }
                dragStart = null;
                dragging = false;
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // Double-click - open dashboard
                    DashboardWindow.showOrFocus();
                }
            }
        };

        contentPanel.addMouseListener(mouseAdapter);
        contentPanel.addMouseMotionListener(mouseAdapter);

        // Right-click menu
        contentPanel.setComponentPopupMenu(createPopupMenu());

        add(contentPanel);

        // Position
        if (config.getDesktopPetX() >= 0 && config.getDesktopPetY() >= 0) {
            setLocation(config.getDesktopPetX(), config.getDesktopPetY());
        } else {
            // Default: bottom-right of screen, above taskbar
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration());
            setLocation(screen.width - currentSize - 60, screen.height - insets.bottom - currentSize - 30);
        }

        setAlwaysOnTop(true);
        refreshAppearance();
    }

    private JPopupMenu createPopupMenu() {
        I18nManager i18n = I18nManager.getInstance();
        JPopupMenu menu = new JPopupMenu();

        // Dashboard
        JMenuItem dashboardItem = new JMenuItem(i18n.get("dashboard.title"));
        dashboardItem.addActionListener(e -> DashboardWindow.showOrFocus());
        menu.add(dashboardItem);

        menu.addSeparator();

        // Pet size submenu
        JMenu sizeMenu = new JMenu(i18n.get("pet.size"));
        int[] sizes = {64, 96, 128};
        for (int s : sizes) {
            JCheckBoxMenuItem sizeItem = new JCheckBoxMenuItem(
                    s + "px", s == config.getDesktopPetSize());
            int finalSize = s;
            sizeItem.addActionListener(e -> {
                config.setDesktopPetSize(finalSize);
                currentSize = finalSize;
                refreshAppearance();
            });
            sizeMenu.add(sizeItem);
        }
        menu.add(sizeMenu);

        // Opacity submenu
        JMenu opacityMenu = new JMenu(i18n.get("pet.opacity"));
        double[] opacities = {1.0, 0.9, 0.8, 0.6};
        String[] opacityLabels = {"100%", "90%", "80%", "60%"};
        for (int i = 0; i < opacities.length; i++) {
            JCheckBoxMenuItem opItem = new JCheckBoxMenuItem(
                    opacityLabels[i], opacities[i] == config.getDesktopPetOpacity());
            double finalOp = opacities[i];
            opItem.addActionListener(e -> {
                config.setDesktopPetOpacity(finalOp);
                setOpacity((float) finalOp);
            });
            opacityMenu.add(opItem);
        }
        menu.add(opacityMenu);

        menu.addSeparator();

        // Hide pet
        JMenuItem hideItem = new JMenuItem(i18n.get("pet.hide"));
        hideItem.addActionListener(e -> {
            config.setDesktopPetEnabled(false);
            hideInstance();
        });
        menu.add(hideItem);

        return menu;
    }

    private void startAnimation() {
        if (animTimer != null && animTimer.isRunning()) return;

        // Calculate initial interval based on CPU
        int interval = 150; // ms between frames for desktop pet (faster = smoother)
        animTimer = new Timer(interval, e -> {
            updateFrame();
            // Update tooltip every few frames
            petLabel.setToolTipText(buildTooltip());
        });
        animTimer.start();
    }

    private void stopAnimation() {
        if (animTimer != null) {
            animTimer.stop();
            animTimer = null;
        }
    }

    private void updateFrame() {
        Image frame = animManager.getNextHiResFrame();
        if (frame == null) {
            frame = animManager.getNextFrame();
            if (frame == null) return;
        }

        // Scale to desktop pet size
        BufferedImage scaled = new BufferedImage(currentSize, currentSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.drawImage(frame, 0, 0, currentSize, currentSize, null);
        g2d.dispose();

        currentFrameIcon = new ImageIcon(scaled);
        petLabel.setIcon(currentFrameIcon);
    }

    private void refreshAppearance() {
        // Update size
        int padding = 8;
        int windowSize = currentSize + padding * 2;
        setSize(windowSize, windowSize + 12); // extra for shadow
        contentPanel.setPreferredSize(new Dimension(windowSize, windowSize + 12));
        petLabel.setPreferredSize(new Dimension(currentSize, currentSize));

        // Update opacity
        float opacity = (float) config.getDesktopPetOpacity();
        if (opacity < 0.3f) opacity = 0.3f;
        if (opacity > 1.0f) opacity = 1.0f;
        setOpacity(opacity);

        // Force first frame render
        updateFrame();

        revalidate();
        repaint();
    }

    private String buildTooltip() {
        I18nManager i18n = I18nManager.getInstance();
        StringBuilder sb = new StringBuilder();
        sb.append(i18n.get("tooltip.cpu", systemMonitor.getCpuUsageText()));
        sb.append(" | ");
        sb.append(i18n.get("tooltip.memory", systemMonitor.getMemoryUsageText()));
        return sb.toString();
    }

    /**
     * Bounce animation when clicked
     */
    private void bouncePet() {
        Timer bounceTimer = new Timer(30, null);
        final int[] step = {0};
        final int totalSteps = 10;
        final int baseY = getY();

        bounceTimer.addActionListener(e -> {
            step[0]++;
            double t = (double) step[0] / totalSteps;
            // Parabolic bounce: go up then back down
            int offset = (int) (-12 * Math.sin(t * Math.PI));
            setLocation(getX(), baseY + offset);

            if (step[0] >= totalSteps) {
                setLocation(getX(), baseY);
                bounceTimer.stop();
            }
        });
        bounceTimer.start();
    }

    @Override
    public void dispose() {
        stopAnimation();
        config.saveDesktopPetPosition(getX(), getY());
        instance = null;
        super.dispose();
    }
}
