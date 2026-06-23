package com.runcat.ui;

import com.runcat.RunCatApp;
import com.runcat.animation.AnimationManager;
import com.runcat.config.AppConfig;
import com.runcat.core.SystemMonitor;
import com.runcat.i18n.I18nManager;
import com.runcat.util.AutoStartManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;

/**
 * System tray icon manager - the heart of the UI
 *
 * Uses JPopupMenu (Swing) instead of AWT PopupMenu to avoid
 * native encoding issues on Windows where native.encoding=GBK
 * causes CJK characters to display as garbled text in AWT menus.
 */
public class TrayIconManager {

    private final TrayIcon trayIcon;
    private final SystemTray systemTray;
    private final AppConfig config;
    private final AnimationManager animationManager;
    private final SystemMonitor systemMonitor;
    private JPopupMenu popupMenu;

    public TrayIconManager(AppConfig config, AnimationManager animationManager,
                           SystemMonitor systemMonitor) throws AWTException {
        this.config = config;
        this.animationManager = animationManager;
        this.systemMonitor = systemMonitor;
        this.systemTray = SystemTray.getSystemTray();

        // Create JPopupMenu (Swing - encoding safe)
        this.popupMenu = createPopupMenu();

        // Create tray icon WITHOUT AWT PopupMenu
        Image initialImage = animationManager.getCurrentFrame();
        if (initialImage == null) {
            initialImage = createDefaultIcon();
        }
        this.trayIcon = new TrayIcon(initialImage, "Java RunCat");
        this.trayIcon.setImageAutoSize(true);
        this.trayIcon.addMouseListener(new TrayIconMouseListener());
    }

    private JPopupMenu createPopupMenu() {
        I18nManager i18n = I18nManager.getInstance();
        JPopupMenu menu = new JPopupMenu();

        // Dashboard item (top)
        JMenuItem dashboardItem = new JMenuItem(i18n.get("dashboard.title"));
        dashboardItem.addActionListener(e -> DashboardWindow.showOrFocus());
        menu.add(dashboardItem);

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
            boolean enabled = autoStartItem.isSelected();
            config.setAutoStart(enabled);
            AutoStartManager.setAutoStart(enabled);
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

        settingsMenu.addSeparator();

        // CPU alert
        JCheckBoxMenuItem cpuAlertItem = new JCheckBoxMenuItem(
                i18n.get("menu.settings.cpuAlert"), config.isCpuAlertEnabled());
        cpuAlertItem.addItemListener(e -> config.setCpuAlertEnabled(cpuAlertItem.isSelected()));
        settingsMenu.add(cpuAlertItem);

        settingsMenu.addSeparator();

        // Icon theme
        JMenu themeMenu = new JMenu(i18n.get("menu.settings.iconTheme"));
        JCheckBoxMenuItem lightItem = new JCheckBoxMenuItem(
                i18n.get("menu.settings.iconTheme.light"), "light".equals(config.getIconTheme()));
        lightItem.addItemListener(e -> {
            config.setIconTheme("light");
            RunCatApp.restart();
        });
        JCheckBoxMenuItem darkItem = new JCheckBoxMenuItem(
                i18n.get("menu.settings.iconTheme.dark"), "dark".equals(config.getIconTheme()));
        darkItem.addItemListener(e -> {
            config.setIconTheme("dark");
            RunCatApp.restart();
        });
        themeMenu.add(lightItem);
        themeMenu.add(darkItem);
        settingsMenu.add(themeMenu);

        menu.add(settingsMenu);

        menu.addSeparator();

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
        for (Map.Entry<String, java.util.List<Image>> entry :
                animationManager.getBuiltInAnimations().entrySet()) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(
                    animationManager.getAnimationDisplayName(entry.getKey()),
                    entry.getKey().equals(config.getCurrentAnimation()));
            item.addItemListener(e -> {
                animationManager.setCurrentAnimation(entry.getKey());
                rebuildMenu();
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
                    animationManager.setCurrentAnimation(entry.getKey());
                    rebuildMenu();
                });
                animationMenu.add(item);
            }
        }
    }

    private void addSpeedItems(JMenu speedMenu) {
        I18nManager i18n = I18nManager.getInstance();
        double speed = config.getSpeedMultiplier();

        JCheckBoxMenuItem slowItem = new JCheckBoxMenuItem(i18n.get("menu.speed.slow"), speed == 0.5);
        slowItem.addItemListener(e -> { config.setSpeedMultiplier(0.5); updateSpeedMenu(speedMenu); });
        JCheckBoxMenuItem normalItem = new JCheckBoxMenuItem(i18n.get("menu.speed.normal"), speed == 1.0);
        normalItem.addItemListener(e -> { config.setSpeedMultiplier(1.0); updateSpeedMenu(speedMenu); });
        JCheckBoxMenuItem fastItem = new JCheckBoxMenuItem(i18n.get("menu.speed.fast"), speed == 2.0);
        fastItem.addItemListener(e -> { config.setSpeedMultiplier(2.0); updateSpeedMenu(speedMenu); });

        speedMenu.add(slowItem);
        speedMenu.add(normalItem);
        speedMenu.add(fastItem);
    }

    private void updateSpeedMenu(JMenu speedMenu) {
        for (int i = 0; i < speedMenu.getItemCount(); i++) {
            JMenuItem item = speedMenu.getItem(i);
            if (item instanceof JCheckBoxMenuItem) {
                ((JCheckBoxMenuItem) item).setSelected(false);
            }
        }
        double speed = config.getSpeedMultiplier();
        int idx = speed == 0.5 ? 0 : speed == 2.0 ? 2 : 1;
        if (idx < speedMenu.getItemCount() && speedMenu.getItem(idx) instanceof JCheckBoxMenuItem cb) {
            cb.setSelected(true);
        }
    }

    private void rebuildMenu() {
        SwingUtilities.invokeLater(() -> {
            popupMenu = createPopupMenu();
        });
    }

    /**
     * Show JPopupMenu at the tray icon location.
     * We need an invisible JFrame to act as the parent for JPopupMenu.show().
     */
    private void showPopupMenu() {
        // Get mouse position on screen
        Point mouseLoc = MouseInfo.getPointerInfo().getLocation();
        // Use the invisible frame approach for reliable positioning
        JDialog invoker = new JDialog();
        invoker.setUndecorated(true);
        invoker.setAlwaysOnTop(true);
        // Position at mouse, size 0x0
        invoker.setLocation(mouseLoc.x, mouseLoc.y);
        invoker.setSize(0, 0);
        invoker.setVisible(true);

        // Show popup menu at (0,0) relative to the invisible dialog
        popupMenu.show(invoker, 0, 0);

        // After popup becomes invisible, dispose the invoker
        popupMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {}
            @Override
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                invoker.dispose();
            }
            @Override
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
                invoker.dispose();
            }
        });
    }

    public void updateIcon() {
        Image frame = animationManager.getNextFrame();
        if (frame != null) {
            trayIcon.setImage(frame);
        }
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
        trayIcon.setToolTip(sb.length() > 0 ? sb.toString() : "Java RunCat");
    }

    public void start() throws AWTException {
        systemTray.add(trayIcon);
        AutoStartManager.setAutoStart(config.isAutoStart());
    }

    public void stop() {
        systemTray.remove(trayIcon);
    }

    private Image createDefaultIcon() {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.ORANGE);
        g.fillOval(2, 2, 12, 12);
        g.dispose();
        return img;
    }

    private void showAboutDialog() {
        I18nManager i18n = I18nManager.getInstance();
        JOptionPane.showMessageDialog(null,
                i18n.get("about.description", "1.0.0"),
                i18n.get("about.title"),
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showCustomAnimationDialog() {
        CustomAnimationDialog dialog = new CustomAnimationDialog(animationManager);
        dialog.setVisible(true);
        if (dialog.isApplied()) {
            rebuildMenu();
        }
    }

    private class TrayIconMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                DashboardWindow.showOrFocus();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON3 || e.isPopupTrigger()) {
                showPopupMenu();
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON3 || e.isPopupTrigger()) {
                showPopupMenu();
            }
        }
    }
}
