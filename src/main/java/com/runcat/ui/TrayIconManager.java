package com.runcat.ui;

import com.runcat.RunCatApp;
import com.runcat.animation.AnimationPlayback;
import com.runcat.animation.AnimationManager;
import com.runcat.config.AppConfig;
import com.runcat.core.SystemMonitor;
import com.runcat.core.UpdateChecker;
import com.runcat.i18n.I18nManager;
import com.runcat.util.AutoStartManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * System tray icon manager.
 * Uses JPopupMenu (Swing) with JWindow invoker to avoid AWT encoding issues.
 * Auto-dismisses menu when clicking outside (on any screen location).
 */
public class TrayIconManager {

    private final TrayIcon trayIcon;
    private final SystemTray systemTray;
    private final AppConfig config;
    private final AnimationManager animationManager;
    private final SystemMonitor systemMonitor;
    private final AnimationPlayback trayPlayback;
    private JPopupMenu popupMenu;
    private JWindow invokerWindow;
    private Timer autoDismissTimer;

    public TrayIconManager(AppConfig config, AnimationManager animationManager,
                           SystemMonitor systemMonitor) throws AWTException {
        this.config = config;
        this.animationManager = animationManager;
        this.systemMonitor = systemMonitor;
        this.systemTray = SystemTray.getSystemTray();
        this.trayPlayback = animationManager.createTrayPlayback();

        this.popupMenu = createPopupMenu();
        this.invokerWindow = new JWindow();
        this.invokerWindow.setAlwaysOnTop(true);

        Image initialImage = trayPlayback.peekFrame();
        if (initialImage == null) initialImage = createDefaultIcon();
        this.trayIcon = new TrayIcon(initialImage, "Java RunCat");
        this.trayIcon.setImageAutoSize(true);
        this.trayIcon.addMouseListener(new TrayIconMouseListener());
    }

    public void rebuildMenu() {
        SwingUtilities.invokeLater(() -> popupMenu = createPopupMenu());
    }

    private JPopupMenu createPopupMenu() {
        I18nManager i18n = I18nManager.getInstance();
        JPopupMenu menu = new JPopupMenu();

        // Dashboard (top)
        JMenuItem dashboardItem = new JMenuItem(i18n.get("dashboard.title"));
        dashboardItem.addActionListener(e -> DashboardWindow.showOrFocus());
        menu.add(dashboardItem);

        // Desktop Pet toggle
        JCheckBoxMenuItem petItem = new JCheckBoxMenuItem(
                i18n.get("pet.toggle"), config.isDesktopPetEnabled());
        petItem.addItemListener(e -> {
            config.setDesktopPetEnabled(petItem.isSelected());
            if (petItem.isSelected()) {
                DesktopPetWindow.showOrFocus();
            } else {
                DesktopPetWindow.hideInstance();
            }
        });
        menu.add(petItem);

        menu.addSeparator();

        // Animation submenu
        JMenu animationMenu = new JMenu(i18n.get("menu.animation"));
        addAnimationItems(animationMenu);
        animationMenu.addSeparator();
        JMenuItem customAnimItem = new JMenuItem(i18n.get("menu.animation.custom"));
        customAnimItem.addActionListener(e -> showCustomAnimationDialog());
        animationMenu.add(customAnimItem);
        menu.add(animationMenu);

        // Speed submenu
        JMenu speedMenu = new JMenu(i18n.get("menu.speed"));
        addSpeedItems(speedMenu);
        menu.add(speedMenu);

        // Language submenu
        JMenu languageMenu = new JMenu(i18n.get("menu.language"));
        for (String locale : i18n.getSupportedLocales()) {
            JCheckBoxMenuItem langItem = new JCheckBoxMenuItem(
                    i18n.getLocaleDisplayName(locale),
                    locale.equals(config.getLanguage()));
            langItem.addItemListener(e -> {
                config.setLanguage(locale);
                RunCatApp.restart();
            });
            languageMenu.add(langItem);
        }
        menu.add(languageMenu);

        menu.addSeparator();

        // Settings submenu
        JMenu settingsMenu = new JMenu(i18n.get("menu.settings"));

        JCheckBoxMenuItem autoStartItem = new JCheckBoxMenuItem(
                i18n.get("menu.settings.autoStart"), config.isAutoStart());
        autoStartItem.addItemListener(e -> {
            config.setAutoStart(autoStartItem.isSelected());
            AutoStartManager.setAutoStart(autoStartItem.isSelected());
        });
        settingsMenu.add(autoStartItem);

        JCheckBoxMenuItem cpuItem = new JCheckBoxMenuItem(
                i18n.get("menu.settings.showCpu"), config.isShowCpuTooltip());
        cpuItem.addItemListener(e -> config.setShowCpuTooltip(cpuItem.isSelected()));
        settingsMenu.add(cpuItem);

        JCheckBoxMenuItem memItem = new JCheckBoxMenuItem(
                i18n.get("menu.settings.showMemory"), config.isShowMemoryTooltip());
        memItem.addItemListener(e -> config.setShowMemoryTooltip(memItem.isSelected()));
        settingsMenu.add(memItem);

        JCheckBoxMenuItem timeItem = new JCheckBoxMenuItem(
                i18n.get("menu.settings.showTime"), config.isShowTimeTooltip());
        timeItem.addItemListener(e -> config.setShowTimeTooltip(timeItem.isSelected()));
        settingsMenu.add(timeItem);

        settingsMenu.addSeparator();

        JMenu topProcessMenu = new JMenu(i18n.get("menu.settings.topProcesses"));
        addTopProcessItems(topProcessMenu);
        settingsMenu.add(topProcessMenu);

        JMenu refreshMenu = new JMenu(i18n.get("menu.settings.dashboardRefresh"));
        addRefreshItems(refreshMenu);
        settingsMenu.add(refreshMenu);

        JCheckBoxMenuItem smoothingItem = new JCheckBoxMenuItem(
                i18n.get("menu.settings.animationSmoothing"), config.isAnimationSmoothingEnabled());
        smoothingItem.addItemListener(e -> config.setAnimationSmoothingEnabled(smoothingItem.isSelected()));
        settingsMenu.add(smoothingItem);

        JMenu petClickMenu = new JMenu(i18n.get("menu.settings.petClickAction"));
        addPetClickItems(petClickMenu);
        settingsMenu.add(petClickMenu);

        // CPU alert
        JCheckBoxMenuItem cpuAlertItem = new JCheckBoxMenuItem(
                i18n.get("menu.settings.cpuAlert"), config.isCpuAlertEnabled());
        cpuAlertItem.addItemListener(e -> config.setCpuAlertEnabled(cpuAlertItem.isSelected()));
        settingsMenu.add(cpuAlertItem);

        settingsMenu.addSeparator();

        JMenuItem manageCustomAnimItem = new JMenuItem(i18n.get("menu.settings.customManager"));
        manageCustomAnimItem.addActionListener(e -> showCustomAnimationDialog());
        settingsMenu.add(manageCustomAnimItem);

        settingsMenu.addSeparator();

        // Icon theme (auto / light / dark)
        JMenu themeMenu = new JMenu(i18n.get("menu.settings.iconTheme"));
        String[] themes = {"auto", "light", "dark"};
        ButtonGroup themeGroup = new ButtonGroup();
        for (String theme : themes) {
            JRadioButtonMenuItem themeItem = new JRadioButtonMenuItem(
                    i18n.get("menu.settings.iconTheme." + theme),
                    theme.equals(config.getIconTheme()));
            themeItem.addItemListener(e -> {
                if (themeItem.isSelected()) {
                    config.setIconTheme(theme);
                    RunCatApp.restart();
                }
            });
            themeGroup.add(themeItem);
            themeMenu.add(themeItem);
        }
        settingsMenu.add(themeMenu);

        menu.add(settingsMenu);

        menu.addSeparator();

        // Check for updates
        JMenuItem updateItem = new JMenuItem(i18n.get("menu.update"));
        updateItem.addActionListener(e -> UpdateChecker.manualCheck());
        menu.add(updateItem);

        // About
        JMenuItem aboutItem = new JMenuItem(i18n.get("menu.about"));
        aboutItem.addActionListener(e -> showAboutDialog());
        menu.add(aboutItem);

        // Exit
        JMenuItem exitItem = new JMenuItem(i18n.get("menu.exit"));
        exitItem.addActionListener(e -> {
            DashboardWindow.hideInstance();
            stop();
            System.exit(0);
        });
        menu.add(exitItem);

        return menu;
    }

    private void addAnimationItems(JMenu animationMenu) {
        ButtonGroup group = new ButtonGroup();
        for (Map.Entry<String, java.util.List<Image>> entry :
                animationManager.getBuiltInAnimations().entrySet()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(
                    animationManager.getAnimationDisplayName(entry.getKey()),
                    entry.getKey().equals(config.getCurrentAnimation()));
            item.addItemListener(e -> {
                if (item.isSelected()) {
                    animationManager.setCurrentAnimation(entry.getKey());
                    trayPlayback.reset();
                    rebuildMenu();
                    DesktopPetWindow.refreshIfShowing();
                }
            });
            group.add(item);
            animationMenu.add(item);
        }

        Map<String, java.util.List<Image>> customAnims = animationManager.getCustomAnimations();
        if (!customAnims.isEmpty()) {
            animationMenu.addSeparator();
            for (Map.Entry<String, java.util.List<Image>> entry : customAnims.entrySet()) {
                JRadioButtonMenuItem item = new JRadioButtonMenuItem(
                        animationManager.getAnimationDisplayName(entry.getKey()),
                        entry.getKey().equals(config.getCurrentAnimation()));
                item.addItemListener(e -> {
                    if (item.isSelected()) {
                        animationManager.setCurrentAnimation(entry.getKey());
                        trayPlayback.reset();
                        rebuildMenu();
                        DesktopPetWindow.refreshIfShowing();
                    }
                });
                group.add(item);
                animationMenu.add(item);
            }
        }
    }

    private void addSpeedItems(JMenu speedMenu) {
        I18nManager i18n = I18nManager.getInstance();
        double speed = config.getSpeedMultiplier();
        ButtonGroup group = new ButtonGroup();

        JRadioButtonMenuItem slowItem = new JRadioButtonMenuItem(i18n.get("menu.speed.slow"), speed == 0.5);
        slowItem.addItemListener(e -> { if (slowItem.isSelected()) config.setSpeedMultiplier(0.5); });
        JRadioButtonMenuItem normalItem = new JRadioButtonMenuItem(i18n.get("menu.speed.normal"), speed == 1.0);
        normalItem.addItemListener(e -> { if (normalItem.isSelected()) config.setSpeedMultiplier(1.0); });
        JRadioButtonMenuItem fastItem = new JRadioButtonMenuItem(i18n.get("menu.speed.fast"), speed == 2.0);
        fastItem.addItemListener(e -> { if (fastItem.isSelected()) config.setSpeedMultiplier(2.0); });

        group.add(slowItem);
        group.add(normalItem);
        group.add(fastItem);
        speedMenu.add(slowItem);
        speedMenu.add(normalItem);
        speedMenu.add(fastItem);
    }

    private void addTopProcessItems(JMenu topProcessMenu) {
        ButtonGroup group = new ButtonGroup();
        for (int count : new int[]{3, 5, 8}) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(
                    count + " " + I18nManager.getInstance().get("menu.settings.topProcesses.suffix"),
                    config.getTopProcessCount() == count);
            item.addItemListener(e -> {
                if (item.isSelected()) {
                    config.setTopProcessCount(count);
                }
            });
            group.add(item);
            topProcessMenu.add(item);
        }
    }

    private void addRefreshItems(JMenu refreshMenu) {
        ButtonGroup group = new ButtonGroup();
        int[] refreshValues = {1000, 2000, 5000};
        for (int refreshValue : refreshValues) {
            String label = (refreshValue / 1000) + "s";
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(label, config.getDashboardRefreshMs() == refreshValue);
            item.addItemListener(e -> {
                if (item.isSelected()) {
                    config.setDashboardRefreshMs(refreshValue);
                }
            });
            group.add(item);
            refreshMenu.add(item);
        }
    }

    private void addPetClickItems(JMenu petClickMenu) {
        ButtonGroup group = new ButtonGroup();
        String current = config.getDesktopPetClickAction();
        for (String action : new String[]{"bounce", "dashboard"}) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(
                    I18nManager.getInstance().get("menu.settings.petClickAction." + action),
                    action.equals(current));
            item.addItemListener(e -> {
                if (item.isSelected()) {
                    config.setDesktopPetClickAction(action);
                }
            });
            group.add(item);
            petClickMenu.add(item);
        }
    }

    /**
     * Show popup menu at the tray icon position.
     * After showing, start an auto-dismiss timer that checks every 200ms
     * if the mouse is outside the menu bounds — if so, close the menu.
     */
    private void showPopupMenu() {
        SwingUtilities.invokeLater(() -> {
            Point mouseLoc = MouseInfo.getPointerInfo().getLocation();
            invokerWindow.setLocation(mouseLoc.x, mouseLoc.y);
            invokerWindow.setVisible(true);

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration());
            int taskbarHeight = screenInsets.bottom;

            int x = mouseLoc.x;
            int y = screenSize.height - taskbarHeight;

            int offsetX = x - mouseLoc.x;
            int offsetY = y - mouseLoc.y;
            popupMenu.show(invokerWindow, offsetX, offsetY);
            popupMenu.requestFocus();

            // Start auto-dismiss: close menu when mouse clicks outside menu bounds
            startAutoDismiss();
        });
    }

    /**
     * Auto-dismiss the popup menu when the user clicks outside it.
     * Uses a polling approach because JPopupMenu with JWindow invoker
     * does not automatically close on outside clicks (unlike native AWT menus).
     */
    private void startAutoDismiss() {
        if (autoDismissTimer != null) autoDismissTimer.stop();

        autoDismissTimer = new Timer(200, null);
        autoDismissTimer.addActionListener(e -> {
            if (!popupMenu.isVisible()) {
                autoDismissTimer.stop();
                invokerWindow.setVisible(false);
                return;
            }

            Point mouseLoc = MouseInfo.getPointerInfo().getLocation();
            Rectangle menuBounds = new Rectangle(popupMenu.getLocationOnScreen(), popupMenu.getSize());

            // If mouse is outside menu bounds, check if a mouse button is pressed
            if (!menuBounds.contains(mouseLoc)) {
                // Check if any mouse button is currently pressed
                // This handles both clicks and drags outside the menu
                if (isMouseButtonPressed()) {
                    popupMenu.setVisible(false);
                    invokerWindow.setVisible(false);
                    autoDismissTimer.stop();
                }
            }
        });
        autoDismissTimer.start();

        // Also add a global AWT listener to catch mouse presses anywhere on screen
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if (event instanceof MouseEvent me) {
                    if (me.getID() == MouseEvent.MOUSE_PRESSED) {
                        Point clickLoc = me.getPoint();
                        try { clickLoc.translate(me.getComponent().getLocationOnScreen().x, me.getComponent().getLocationOnScreen().y); } catch (Exception ex) {}
                        Rectangle menuBounds = new Rectangle(popupMenu.getLocationOnScreen(), popupMenu.getSize());
                        // If click is outside the menu, dismiss it
                        if (popupMenu.isVisible() && !menuBounds.contains(clickLoc)) {
                            // Don't dismiss if clicking on the tray icon itself (allows re-open)
                            // The tray icon area is typically at the bottom-right of the screen
                            popupMenu.setVisible(false);
                            invokerWindow.setVisible(false);
                            if (autoDismissTimer != null) autoDismissTimer.stop();
                            Toolkit.getDefaultToolkit().removeAWTEventListener(this);
                        }
                    }
                }
            }
        }, AWTEvent.MOUSE_EVENT_MASK);

        // Clean up when menu closes normally (selection made)
        popupMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {}

            @Override
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                invokerWindow.setVisible(false);
                if (autoDismissTimer != null) autoDismissTimer.stop();
            }

            @Override
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
                invokerWindow.setVisible(false);
                if (autoDismissTimer != null) autoDismissTimer.stop();
            }
        });
    }

    private boolean isMouseButtonPressed() {
        // Check if any mouse button is currently held down
        // This uses a heuristic: if mouse moved very recently and we're polling,
        // we can check using InputEvent modifiers
        try {
            // Alternative: always dismiss if mouse is outside and we've been polling for >1s
            // This catches the "click on empty area" case
            return false; // We rely on the AWTEventListener instead
        } catch (Exception e) {
            return false;
        }
    }

    public void updateIcon() {
        Image frame = trayPlayback.nextFrame();
        if (frame != null) trayIcon.setImage(frame);
        updateTooltip();
    }

    private void updateTooltip() {
        I18nManager i18n = I18nManager.getInstance();
        StringBuilder sb = new StringBuilder();
        if (config.isShowCpuTooltip()) {
            sb.append(i18n.get("tooltip.cpu", systemMonitor.getCpuUsageText()));
        }
        if (config.isShowMemoryTooltip()) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(i18n.get("tooltip.memory", systemMonitor.getMemoryUsageText()));
        }
        if (config.isShowTimeTooltip()) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        trayIcon.setToolTip(sb.length() > 0 ? sb.toString() : i18n.get("app.title"));
    }

    public void start() throws AWTException {
        systemTray.add(trayIcon);
        AutoStartManager.setAutoStart(config.isAutoStart());
        // Check for updates on startup (silent, non-blocking)
        UpdateChecker.checkForUpdates();
    }

    public void stop() {
        systemTray.remove(trayIcon);
        if (invokerWindow != null) invokerWindow.dispose();
        if (autoDismissTimer != null) autoDismissTimer.stop();
    }

    private Image createDefaultIcon() {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(60, 60, 70));
        g.fillOval(2, 2, 12, 12);
        g.dispose();
        return img;
    }

    private void showAboutDialog() {
        I18nManager i18n = I18nManager.getInstance();
        JOptionPane.showMessageDialog(null,
                i18n.get("about.description", "1.2.0"),
                i18n.get("about.title"),
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showCustomAnimationDialog() {
        CustomAnimationDialog dialog = new CustomAnimationDialog(animationManager);
        dialog.setVisible(true);
        if (dialog.isApplied()) rebuildMenu();
    }

    private class TrayIconMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                if (DashboardWindow.isDashboardVisible()) {
                    DashboardWindow.hideInstance();
                } else {
                    DashboardWindow.showOrFocus();
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) showPopupMenu();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) showPopupMenu();
        }
    }
}
