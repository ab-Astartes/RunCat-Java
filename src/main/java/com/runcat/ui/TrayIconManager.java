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
 * Auto-dismisses menu when clicking outside using:
 * 1) A thin 1-pixel border around the menu area as click target
 * 2) Polling MouseInfo + checking focus loss
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
    private long menuShownTime;

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

    private boolean menuRebuildPending = false;

    public void rebuildMenu() {
        if (popupMenu != null && popupMenu.isVisible()) {
            // Don't rebuild while menu is showing — schedule it for after close
            menuRebuildPending = true;
        } else {
            SwingUtilities.invokeLater(() -> popupMenu = createPopupMenu());
        }
    }

    /** Called when popup menu closes — execute any pending rebuild */
    private void onMenuClosed() {
        if (menuRebuildPending) {
            menuRebuildPending = false;
            SwingUtilities.invokeLater(() -> popupMenu = createPopupMenu());
        }
    }

    private JPopupMenu createPopupMenu() {
        I18nManager i18n = I18nManager.getInstance();
        JPopupMenu menu = new JPopupMenu();

        JMenuItem dashboardItem = new JMenuItem(i18n.get("dashboard.title"));
        dashboardItem.addActionListener(e -> DashboardWindow.showOrFocus());
        menu.add(dashboardItem);

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

        JMenu animationMenu = new JMenu(i18n.get("menu.animation"));
        addAnimationItems(animationMenu);
        animationMenu.addSeparator();
        JMenuItem customAnimItem = new JMenuItem(i18n.get("menu.animation.custom"));
        customAnimItem.addActionListener(e -> showCustomAnimationDialog());
        animationMenu.add(customAnimItem);
        menu.add(animationMenu);

        JMenu speedMenu = new JMenu(i18n.get("menu.speed"));
        addSpeedItems(speedMenu);
        menu.add(speedMenu);

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

        JCheckBoxMenuItem cpuAlertItem = new JCheckBoxMenuItem(
                i18n.get("menu.settings.cpuAlert"), config.isCpuAlertEnabled());
        cpuAlertItem.addItemListener(e -> config.setCpuAlertEnabled(cpuAlertItem.isSelected()));
        settingsMenu.add(cpuAlertItem);

        settingsMenu.addSeparator();

        JMenuItem manageCustomAnimItem = new JMenuItem(i18n.get("menu.settings.customManager"));
        manageCustomAnimItem.addActionListener(e -> showCustomAnimationDialog());
        settingsMenu.add(manageCustomAnimItem);

        settingsMenu.addSeparator();

        JMenu themeMenu = new JMenu(i18n.get("menu.settings.iconTheme"));
        String[] themes = {"auto", "light", "dark"};
        for (String theme : themes) {
            JCheckBoxMenuItem themeItem = new JCheckBoxMenuItem(
                    i18n.get("menu.settings.iconTheme." + theme),
                    theme.equals(config.getIconTheme()));
            themeItem.addItemListener(e -> {
                if (themeItem.isSelected()) {
                    config.setIconTheme(theme);
                    uncheckOtherItems(themeMenu, themeItem);
                    RunCatApp.restart();
                } else if (theme.equals(config.getIconTheme())) {
                    themeItem.setSelected(true);
                }
            });
            themeMenu.add(themeItem);
        }
        settingsMenu.add(themeMenu);

        menu.add(settingsMenu);

        menu.addSeparator();

        JMenuItem updateItem = new JMenuItem(i18n.get("menu.update"));
        updateItem.addActionListener(e -> UpdateChecker.manualCheck());
        menu.add(updateItem);

        JMenuItem aboutItem = new JMenuItem(i18n.get("menu.about"));
        aboutItem.addActionListener(e -> showAboutDialog());
        menu.add(aboutItem);

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
        for (Map.Entry<String, java.util.List<Image>> entry :
                animationManager.getBuiltInAnimations().entrySet()) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(
                    animationManager.getAnimationDisplayName(entry.getKey()),
                    entry.getKey().equals(config.getCurrentAnimation()));
            item.addItemListener(e -> {
                if (item.isSelected()) {
                    animationManager.setCurrentAnimation(entry.getKey());
                    trayPlayback.reset();
                    uncheckOtherItems(animationMenu, item);
                    DesktopPetWindow.refreshIfShowing();
                } else {
                    if (entry.getKey().equals(config.getCurrentAnimation())) {
                        item.setSelected(true);
                    }
                }
            });
            animationMenu.add(item);
        }

        Map<String, java.util.List<Image>> customAnims = animationManager.getCustomAnimations();
        if (!customAnims.isEmpty()) {
            animationMenu.addSeparator();
            for (Map.Entry<String, java.util.List<Image>> entry : customAnims.entrySet()) {
                JCheckBoxMenuItem item = new JCheckBoxMenuItem(
                        animationManager.getAnimationDisplayName(entry.getKey()),
                        entry.getKey().equals(config.getCurrentAnimation()));
                item.addItemListener(e -> {
                    if (item.isSelected()) {
                        animationManager.setCurrentAnimation(entry.getKey());
                        trayPlayback.reset();
                        uncheckOtherItems(animationMenu, item);
                        DesktopPetWindow.refreshIfShowing();
                    } else {
                        if (entry.getKey().equals(config.getCurrentAnimation())) {
                            item.setSelected(true);
                        }
                    }
                });
                animationMenu.add(item);
            }
        }
    }

    private void uncheckOtherItems(JMenu animationMenu, JCheckBoxMenuItem selected) {
        for (int i = 0; i < animationMenu.getItemCount(); i++) {
            JMenuItem mi = animationMenu.getItem(i);
            if (mi instanceof JCheckBoxMenuItem cb && cb != selected) {
                cb.setSelected(false);
            }
        }
    }

    private void addSpeedItems(JMenu speedMenu) {
        I18nManager i18n = I18nManager.getInstance();
        double speed = config.getSpeedMultiplier();

        JCheckBoxMenuItem slowItem = new JCheckBoxMenuItem(i18n.get("menu.speed.slow"), speed == 0.5);
        slowItem.addItemListener(e -> {
            if (slowItem.isSelected()) {
                config.setSpeedMultiplier(0.5);
                uncheckOtherItems(speedMenu, slowItem);
            } else if (config.getSpeedMultiplier() == 0.5) {
                slowItem.setSelected(true);
            }
        });
        JCheckBoxMenuItem normalItem = new JCheckBoxMenuItem(i18n.get("menu.speed.normal"), speed == 1.0);
        normalItem.addItemListener(e -> {
            if (normalItem.isSelected()) {
                config.setSpeedMultiplier(1.0);
                uncheckOtherItems(speedMenu, normalItem);
            } else if (config.getSpeedMultiplier() == 1.0) {
                normalItem.setSelected(true);
            }
        });
        JCheckBoxMenuItem fastItem = new JCheckBoxMenuItem(i18n.get("menu.speed.fast"), speed == 2.0);
        fastItem.addItemListener(e -> {
            if (fastItem.isSelected()) {
                config.setSpeedMultiplier(2.0);
                uncheckOtherItems(speedMenu, fastItem);
            } else if (config.getSpeedMultiplier() == 2.0) {
                fastItem.setSelected(true);
            }
        });
        speedMenu.add(slowItem);
        speedMenu.add(normalItem);
        speedMenu.add(fastItem);
    }

    private void addTopProcessItems(JMenu topProcessMenu) {
        for (int count : new int[]{3, 5, 8}) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(
                    count + " " + I18nManager.getInstance().get("menu.settings.topProcesses.suffix"),
                    config.getTopProcessCount() == count);
            item.addItemListener(e -> {
                if (item.isSelected()) {
                    config.setTopProcessCount(count);
                    uncheckOtherItems(topProcessMenu, item);
                } else if (config.getTopProcessCount() == count) {
                    item.setSelected(true);
                }
            });
            topProcessMenu.add(item);
        }
    }

    private void addRefreshItems(JMenu refreshMenu) {
        int[] refreshValues = {1000, 2000, 5000};
        for (int refreshValue : refreshValues) {
            String label = (refreshValue / 1000) + "s";
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(label, config.getDashboardRefreshMs() == refreshValue);
            item.addItemListener(e -> {
                if (item.isSelected()) {
                    config.setDashboardRefreshMs(refreshValue);
                    uncheckOtherItems(refreshMenu, item);
                } else if (config.getDashboardRefreshMs() == refreshValue) {
                    item.setSelected(true);
                }
            });
            refreshMenu.add(item);
        }
    }

    private void addPetClickItems(JMenu petClickMenu) {
        String current = config.getDesktopPetClickAction();
        for (String action : new String[]{"bounce", "dashboard"}) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(
                    I18nManager.getInstance().get("menu.settings.petClickAction." + action),
                    action.equals(current));
            item.addItemListener(e -> {
                if (item.isSelected()) {
                    config.setDesktopPetClickAction(action);
                    uncheckOtherItems(petClickMenu, item);
                } else if (action.equals(config.getDesktopPetClickAction())) {
                    item.setSelected(true);
                }
            });
            petClickMenu.add(item);
        }
    }

    /**
     * Show popup menu at the tray icon position.
     * Uses a JWindow invoker and starts an auto-dismiss mechanism.
     *
     * Auto-dismiss strategy (multi-layered):
     * 1. PopupMenuListener — catches normal menu close (item selected, ESC pressed)
     * 2. FocusLoss detection — when invokerWindow loses focus, dismiss
     * 3. MouseInfo polling — every 100ms check if mouse moved far from menu
     *    and the menu no longer has any sub-menu visible
     * 4. WindowDeactivation — when invokerWindow is deactivated, dismiss
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

            popupMenu.show(invokerWindow, x - mouseLoc.x, y - mouseLoc.y);
            popupMenu.requestFocus();

            menuShownTime = System.currentTimeMillis();

            // PopupMenuListener: clean up when menu closes normally
            popupMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {}

                @Override
                public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                    cleanupAfterMenuClose();
                    onMenuClosed();
                }

                @Override
                public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
                    cleanupAfterMenuClose();
                    onMenuClosed();
                }
            });

            // WindowDeactivation: dismiss when invoker window is deactivated (focus goes elsewhere)
            invokerWindow.addWindowListener(new WindowAdapter() {
                @Override
                public void windowDeactivated(WindowEvent e) {
                    // Only dismiss if menu is still visible and more than 200ms since show
                    // (avoid dismissing during initial focus transition)
                    if (popupMenu.isVisible() && System.currentTimeMillis() - menuShownTime > 200) {
                        SwingUtilities.invokeLater(() -> {
                            if (popupMenu.isVisible()) {
                                popupMenu.setVisible(false);
                            }
                            cleanupAfterMenuClose();
                        });
                    }
                }
            });

            // Polling: check every 100ms if mouse is far from menu and no sub-menus active
            if (autoDismissTimer != null) autoDismissTimer.stop();
            autoDismissTimer = new Timer(100, e -> {
                if (!popupMenu.isVisible()) {
                    autoDismissTimer.stop();
                    return;
                }
                // Skip for first 300ms after showing (user might still be moving to menu)
                if (System.currentTimeMillis() - menuShownTime < 300) return;

                Point currentMouse = MouseInfo.getPointerInfo().getLocation();
                try {
                    Rectangle menuBounds = new Rectangle(
                            popupMenu.getLocationOnScreen(), popupMenu.getSize());

                    // If mouse is inside menu bounds, keep open
                    if (menuBounds.contains(currentMouse)) return;

                    // Check if any sub-menu is currently showing
                    // (if so, mouse might be moving between parent and child menu)
                    for (MenuElement elem : MenuSelectionManager.defaultManager().getSelectedPath()) {
                        if (elem instanceof JPopupMenu subMenu && subMenu.isVisible()) {
                            try {
                                Rectangle subBounds = new Rectangle(
                                        subMenu.getLocationOnScreen(), subMenu.getSize());
                                if (subBounds.contains(currentMouse)) return;
                            } catch (Exception ex) { /* ignore */ }
                        }
                    }

                    // Mouse is outside all menu bounds and no sub-menus hovered
                    // Check if mouse has been outside for >1 second (user clearly moved away)
                    // Or if mouse position changed significantly since last check
                    dismissMenu();
                    autoDismissTimer.stop();
                } catch (Exception ex) {
                    // Menu location unavailable — likely already closed
                    autoDismissTimer.stop();
                }
            });
            autoDismissTimer.start();
        });
    }

    private void dismissMenu() {
        SwingUtilities.invokeLater(() -> {
            if (popupMenu.isVisible()) popupMenu.setVisible(false);
            cleanupAfterMenuClose();
        });
    }

    private void cleanupAfterMenuClose() {
        invokerWindow.setVisible(false);
        // Remove all temporary listeners
        for (WindowListener wl : invokerWindow.getWindowListeners()) {
            invokerWindow.removeWindowListener(wl);
        }
        for (javax.swing.event.PopupMenuListener pml : popupMenu.getPopupMenuListeners()) {
            popupMenu.removePopupMenuListener(pml);
        }
        if (autoDismissTimer != null) {
            autoDismissTimer.stop();
            autoDismissTimer = null;
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
        UpdateChecker.checkForUpdates();
    }

    public void stop() {
        systemTray.remove(trayIcon);
        if (invokerWindow != null) invokerWindow.dispose();
        if (autoDismissTimer != null) autoDismissTimer.stop();
    }

    private Image createDefaultIcon() {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
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
