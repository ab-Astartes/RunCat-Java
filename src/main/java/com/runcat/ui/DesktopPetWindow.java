package com.runcat.ui;

import com.runcat.RunCatApp;
import com.runcat.animation.AnimationPlayback;
import com.runcat.animation.AnimationManager;
import com.runcat.animation.PetAnimationState;
import com.runcat.config.AppConfig;
import com.runcat.core.SystemMonitor;
import com.runcat.i18n.I18nManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
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
    private final AnimationPlayback petPlayback;

    private JLabel petLabel;
    private JPanel contentPanel;
    private Timer animTimer;
    private Point dragStart;
    private boolean dragging = false;
    private boolean dragArmed = false;
    private Point dragOffset;
    private AWTEventListener globalMouseListener;

    private int currentSize;
    private ImageIcon currentFrameIcon;
    private PetAnimationState petState = PetAnimationState.IDLE;
    private Timer stateResetTimer;

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
        this.petPlayback = animManager.createPetPlayback();
        this.currentSize = config.getDesktopPetSize();

        initUI();
        startAnimation();
    }

    private void initUI() {
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
                Color baseShadow = petState == PetAnimationState.ALERT
                        ? new Color(220, 85, 55, 30)
                        : new Color(0, 0, 0, 24);
                // Elliptical shadow
                for (int i = 3; i >= 0; i--) {
                    int alpha = Math.min(90, baseShadow.getAlpha() + i * 10);
                    int expand = i * 4;
                    g2d.setColor(new Color(baseShadow.getRed(), baseShadow.getGreen(), baseShadow.getBlue(), alpha));
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
                if (SwingUtilities.isLeftMouseButton(e)) {
                    armDrag(e);
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                moveWindowWithMouse(e.getLocationOnScreen());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                finishPointerInteraction(e);
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
        shadowPanel.addMouseListener(mouseAdapter);
        shadowPanel.addMouseMotionListener(mouseAdapter);
        petLabel.addMouseListener(mouseAdapter);
        petLabel.addMouseMotionListener(mouseAdapter);

        // Right-click menu (with auto-dismiss for transparent JWindow)
        JPopupMenu popupMenu = createPopupMenu();
        installPetMenuAutoDismiss(popupMenu);
        contentPanel.setComponentPopupMenu(popupMenu);
        shadowPanel.setComponentPopupMenu(popupMenu);
        petLabel.setComponentPopupMenu(popupMenu);

        add(contentPanel);
        installGlobalMouseTracking();

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

    /**
     * Auto-dismiss the pet's popup menu when the mouse moves away.
     * Because DesktopPetWindow is a transparent always-on-top JWindow,
     * Swing's default "click outside to close" mechanism doesn't work —
     * clicks on the desktop pass through the transparent window.
     * So we poll MouseInfo and close the menu when the cursor leaves
     * the menu bounds for a sustained period.
     */
    private Timer petMenuDismissTimer;
    private long petMenuShownTime;

    private void installPetMenuAutoDismiss(JPopupMenu menu) {
        menu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                petMenuShownTime = System.currentTimeMillis();
                startPetMenuDismissPoll(menu);
            }

            @Override
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                stopPetMenuDismissPoll();
            }

            @Override
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
                stopPetMenuDismissPoll();
            }
        });
    }

    private void startPetMenuDismissPoll(JPopupMenu menu) {
        stopPetMenuDismissPoll();
        petMenuDismissTimer = new Timer(150, e -> {
            if (!menu.isVisible()) {
                stopPetMenuDismissPoll();
                return;
            }
            // Don't dismiss too quickly after showing
            if (System.currentTimeMillis() - petMenuShownTime < 400) return;

            Point mouse = MouseInfo.getPointerInfo().getLocation();
            try {
                Rectangle menuBounds = new Rectangle(menu.getLocationOnScreen(), menu.getSize());
                if (menuBounds.contains(mouse)) return;

                // Check sub-menus too
                for (MenuElement elem : MenuSelectionManager.defaultManager().getSelectedPath()) {
                    if (elem instanceof JPopupMenu subMenu && subMenu.isVisible()) {
                        try {
                            Rectangle subBounds = new Rectangle(subMenu.getLocationOnScreen(), subMenu.getSize());
                            if (subBounds.contains(mouse)) return;
                        } catch (Exception ex) { /* ignore */ }
                    }
                }

                // Mouse outside all menus — dismiss
                menu.setVisible(false);
                stopPetMenuDismissPoll();
            } catch (Exception ex) {
                stopPetMenuDismissPoll();
            }
        });
        petMenuDismissTimer.start();
    }

    private void stopPetMenuDismissPoll() {
        if (petMenuDismissTimer != null) {
            petMenuDismissTimer.stop();
            petMenuDismissTimer = null;
        }
    }

    private void startAnimation() {
        if (animTimer != null && animTimer.isRunning()) return;

        // Calculate initial interval based on CPU
        int interval = 150; // ms between frames for desktop pet (faster = smoother)
        animTimer = new Timer(interval, e -> {
            updatePetStateFromLoad();
            updateFrame();
            updateTimerDelay();
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
        Image frame = petPlayback.nextFrame();
        if (frame == null) return;

        // Scale to desktop pet size
        BufferedImage scaled = new BufferedImage(currentSize, currentSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.drawImage(frame, 0, 0, currentSize, currentSize, null);
        g2d.dispose();

        currentFrameIcon = new ImageIcon(polishPetFrame(scaled));
        petLabel.setIcon(currentFrameIcon);
    }

    private BufferedImage polishPetFrame(BufferedImage source) {
        BufferedImage polished = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = polished.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Subtle outline to separate the pet from bright desktop backgrounds.
        g.drawImage(tintAlphaMask(source, new Color(28, 30, 36, 34)), -1, 0, null);
        g.drawImage(tintAlphaMask(source, new Color(28, 30, 36, 34)), 1, 0, null);
        g.drawImage(tintAlphaMask(source, new Color(28, 30, 36, 34)), 0, -1, null);
        g.drawImage(tintAlphaMask(source, new Color(28, 30, 36, 34)), 0, 1, null);

        g.drawImage(source, 0, 0, null);

        // Soft top-left rim light so the pet feels less flat.
        g.setComposite(AlphaComposite.SrcOver.derive(0.14f));
        g.setPaint(new GradientPaint(
                0, 0, new Color(255, 255, 255, 180),
                source.getWidth(), source.getHeight(), new Color(255, 255, 255, 0)));
        g.fillOval(source.getWidth() / 7, source.getHeight() / 10, source.getWidth() * 2 / 3, source.getHeight() / 2);

        g.dispose();
        return polished;
    }

    private BufferedImage tintAlphaMask(BufferedImage source, Color color) {
        BufferedImage mask = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = mask.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.setComposite(AlphaComposite.SrcIn);
        g.setColor(color);
        g.fillRect(0, 0, source.getWidth(), source.getHeight());
        g.dispose();
        return mask;
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
        petPlayback.reset();
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
        final int totalSteps = 14;
        final int baseY = getY();

        bounceTimer.addActionListener(e -> {
            step[0]++;
            double t = (double) step[0] / totalSteps;
            int offset = easeOutBounceOffset(t);
            setLocation(getX(), baseY + offset);

            if (step[0] >= totalSteps) {
                setLocation(getX(), baseY);
                updatePetStateFromLoad();
                bounceTimer.stop();
            }
        });
        bounceTimer.start();
    }

    private int easeOutBounceOffset(double t) {
        double eased = Math.sin(Math.PI * t);
        return (int) Math.round(-16 * eased * (0.75 + (1 - t) * 0.25));
    }

    private void handlePrimaryClick() {
        String clickAction = config.getDesktopPetClickAction();
        if ("dashboard".equals(clickAction)) {
            DashboardWindow.showOrFocus();
            return;
        }
        setPetState(PetAnimationState.JUMP);
        bouncePet();
    }

    private void updateTimerDelay() {
        if (animTimer == null) return;
        double cpuUsage = Math.max(0, systemMonitor.getCpuUsage());
        double multiplier = config.getSpeedMultiplier();
        int targetDelay = (int) ((220 - (cpuUsage / 100.0) * 120) / multiplier);
        targetDelay = Math.max(45, Math.min(220, targetDelay));
        if (config.isAnimationSmoothingEnabled()) {
            targetDelay = (int) Math.round((animTimer.getDelay() * 0.65) + (targetDelay * 0.35));
        }
        animTimer.setDelay(targetDelay);
    }

    private void updatePetStateFromLoad() {
        if (dragging) {
            setPetState(PetAnimationState.DRAG);
            return;
        }
        if (systemMonitor.getCpuUsage() >= Math.max(90, config.getCpuAlertThreshold())) {
            setPetState(PetAnimationState.ALERT);
            return;
        }
        setPetState(systemMonitor.getCpuUsage() >= 25 ? PetAnimationState.RUN : PetAnimationState.IDLE);
    }

    private void setPetState(PetAnimationState state) {
        if (state == null) return;
        petState = state;
        petPlayback.setState(state);
        if (state == PetAnimationState.JUMP || state == PetAnimationState.ALERT) {
            scheduleStateReset();
        }
        repaint();
    }

    private void scheduleStateReset() {
        if (stateResetTimer != null) {
            stateResetTimer.stop();
        }
        stateResetTimer = new Timer(900, e -> {
            updatePetStateFromLoad();
            stateResetTimer.stop();
        });
        stateResetTimer.setRepeats(false);
        stateResetTimer.start();
    }

    static Shape createHitShape(int width, int height) {
        return new RoundRectangle2D.Double(0, 0, Math.max(1, width), Math.max(1, height), 26, 26);
    }

    private void armDrag(MouseEvent e) {
        dragStart = e.getLocationOnScreen();
        dragOffset = new Point(dragStart.x - getX(), dragStart.y - getY());
        dragArmed = true;
        dragging = false;
    }

    private void moveWindowWithMouse(Point pointOnScreen) {
        if (!dragArmed || dragOffset == null || pointOnScreen == null) {
            return;
        }
        setPetState(PetAnimationState.DRAG);
        setLocation(pointOnScreen.x - dragOffset.x, pointOnScreen.y - dragOffset.y);
        dragging = true;
    }

    private void finishPointerInteraction(MouseEvent e) {
        if (!dragArmed) {
            return;
        }
        boolean shouldTriggerClick = SwingUtilities.isLeftMouseButton(e) && !dragging && e.getClickCount() <= 1;
        dragArmed = false;
        dragStart = null;
        dragOffset = null;
        if (dragging) {
            updatePetStateFromLoad();
        }
        dragging = false;
        if (shouldTriggerClick) {
            handlePrimaryClick();
        }
    }

    private void installGlobalMouseTracking() {
        globalMouseListener = event -> {
            if (!(event instanceof MouseEvent mouseEvent) || !dragArmed) {
                return;
            }
            if (mouseEvent.getID() == MouseEvent.MOUSE_DRAGGED) {
                moveWindowWithMouse(mouseEvent.getLocationOnScreen());
            } else if (mouseEvent.getID() == MouseEvent.MOUSE_RELEASED) {
                finishPointerInteraction(mouseEvent);
            }
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(
                globalMouseListener,
                AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK);
    }

    @Override
    public void dispose() {
        stopAnimation();
        stopPetMenuDismissPoll();
        if (stateResetTimer != null) {
            stateResetTimer.stop();
        }
        if (globalMouseListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(globalMouseListener);
        }
        config.saveDesktopPetPosition(getX(), getY());
        instance = null;
        super.dispose();
    }
}
