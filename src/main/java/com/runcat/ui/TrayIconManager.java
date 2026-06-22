package com.runcat.ui;

import com.runcat.RunCatApp;
import com.runcat.animation.AnimationManager;
import com.runcat.config.AppConfig;
import com.runcat.core.SystemMonitor;
import com.runcat.i18n.I18nManager;
import com.runcat.util.AutoStartManager;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.*;
import java.util.Map;

/**
 * System tray icon manager - the heart of the UI
 */
public class TrayIconManager {

    private final TrayIcon trayIcon;
    private final SystemTray systemTray;
    private final AppConfig config;
    private final AnimationManager animationManager;
    private final SystemMonitor systemMonitor;
    private final PopupMenu popupMenu;

    public TrayIconManager(AppConfig config, AnimationManager animationManager,
                           SystemMonitor systemMonitor) throws AWTException {
        this.config = config;
        this.animationManager = animationManager;
        this.systemMonitor = systemMonitor;
        this.systemTray = SystemTray.getSystemTray();

        // Create popup menu
        this.popupMenu = createPopupMenu();

        // Create tray icon
        Image initialImage = animationManager.getCurrentFrame();
        if (initialImage == null) {
            initialImage = createDefaultIcon();
        }
        this.trayIcon = new TrayIcon(initialImage, "Java RunCat", popupMenu);
        this.trayIcon.setImageAutoSize(true);
        this.trayIcon.addMouseListener(new TrayIconMouseListener());
    }

    private PopupMenu createPopupMenu() {
        I18nManager i18n = I18nManager.getInstance();
        PopupMenu menu = new PopupMenu();

        // Dashboard item (top)
        MenuItem dashboardItem = new MenuItem(i18n.get("dashboard.title"));
        dashboardItem.addActionListener(e -> DashboardWindow.showOrFocus());
        menu.add(dashboardItem);

        menu.addSeparator();

        // Animation submenu
        Menu animationMenu = new Menu(i18n.get("menu.animation"));
        addAnimationItems(animationMenu);
        animationMenu.addSeparator();
        MenuItem customAnimItem = new MenuItem(i18n.get("menu.animation.custom"));
        customAnimItem.addActionListener(e -> showCustomAnimationDialog());
        animationMenu.add(customAnimItem);
        menu.add(animationMenu);

        // Speed submenu
        Menu speedMenu = new Menu(i18n.get("menu.speed"));
        addSpeedItems(speedMenu);
        menu.add(speedMenu);

        // Language submenu
        Menu languageMenu = new Menu(i18n.get("menu.language"));
        for (String locale : i18n.getSupportedLocales()) {
            CheckboxMenuItem langItem = new CheckboxMenuItem(
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
        Menu settingsMenu = new Menu(i18n.get("menu.settings"));

        CheckboxMenuItem autoStartItem = new CheckboxMenuItem(
                i18n.get("menu.settings.autoStart"), config.isAutoStart());
        autoStartItem.addItemListener(e -> {
            boolean enabled = autoStartItem.getState();
            config.setAutoStart(enabled);
            AutoStartManager.setAutoStart(enabled);
        });
        settingsMenu.add(autoStartItem);

        CheckboxMenuItem cpuItem = new CheckboxMenuItem(
                i18n.get("menu.settings.showCpu"), config.isShowCpuTooltip());
        cpuItem.addItemListener(e -> config.setShowCpuTooltip(cpuItem.getState()));
        settingsMenu.add(cpuItem);

        CheckboxMenuItem memItem = new CheckboxMenuItem(
                i18n.get("menu.settings.showMemory"), config.isShowMemoryTooltip());
        memItem.addItemListener(e -> config.setShowMemoryTooltip(memItem.getState()));
        settingsMenu.add(memItem);

        settingsMenu.addSeparator();

        // CPU alert
        CheckboxMenuItem cpuAlertItem = new CheckboxMenuItem(
                i18n.get("menu.settings.cpuAlert"), config.isCpuAlertEnabled());
        cpuAlertItem.addItemListener(e -> config.setCpuAlertEnabled(cpuAlertItem.getState()));
        settingsMenu.add(cpuAlertItem);

        settingsMenu.addSeparator();

        // Icon theme
        Menu themeMenu = new Menu(i18n.get("menu.settings.iconTheme"));
        CheckboxMenuItem lightItem = new CheckboxMenuItem(
                i18n.get("menu.settings.iconTheme.light"), "light".equals(config.getIconTheme()));
        lightItem.addItemListener(e -> {
            config.setIconTheme("light");
            RunCatApp.restart();
        });
        CheckboxMenuItem darkItem = new CheckboxMenuItem(
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
        MenuItem aboutItem = new MenuItem(i18n.get("menu.about"));
        aboutItem.addActionListener(e -> showAboutDialog());
        menu.add(aboutItem);

        // Exit
        MenuItem exitItem = new MenuItem(i18n.get("menu.exit"));
        exitItem.addActionListener(e -> {
            DashboardWindow.hideInstance();
            stop();
            System.exit(0);
        });
        menu.add(exitItem);

        return menu;
    }

    private void addAnimationItems(Menu animationMenu) {
        // Built-in animations
        for (Map.Entry<String, java.util.List<Image>> entry :
                animationManager.getBuiltInAnimations().entrySet()) {
            CheckboxMenuItem item = new CheckboxMenuItem(
                    animationManager.getAnimationDisplayName(entry.getKey()),
                    entry.getKey().equals(config.getCurrentAnimation()));
            item.addItemListener(e -> {
                animationManager.setCurrentAnimation(entry.getKey());
                rebuildMenu();
            });
            animationMenu.add(item);
        }

        // Custom animations
        Map<String, java.util.List<Image>> customAnims = animationManager.getCustomAnimations();
        if (!customAnims.isEmpty()) {
            animationMenu.addSeparator();
            for (Map.Entry<String, java.util.List<Image>> entry : customAnims.entrySet()) {
                CheckboxMenuItem item = new CheckboxMenuItem(
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

    private void addSpeedItems(Menu speedMenu) {
        I18nManager i18n = I18nManager.getInstance();
        double speed = config.getSpeedMultiplier();

        CheckboxMenuItem slowItem = new CheckboxMenuItem(i18n.get("menu.speed.slow"), speed == 0.5);
        slowItem.addItemListener(e -> { config.setSpeedMultiplier(0.5); updateSpeedMenu(speedMenu); });
        CheckboxMenuItem normalItem = new CheckboxMenuItem(i18n.get("menu.speed.normal"), speed == 1.0);
        normalItem.addItemListener(e -> { config.setSpeedMultiplier(1.0); updateSpeedMenu(speedMenu); });
        CheckboxMenuItem fastItem = new CheckboxMenuItem(i18n.get("menu.speed.fast"), speed == 2.0);
        fastItem.addItemListener(e -> { config.setSpeedMultiplier(2.0); updateSpeedMenu(speedMenu); });

        speedMenu.add(slowItem);
        speedMenu.add(normalItem);
        speedMenu.add(fastItem);
    }

    private void updateSpeedMenu(Menu speedMenu) {
        for (int i = 0; i < speedMenu.getItemCount(); i++) {
            MenuItem item = speedMenu.getItem(i);
            if (item instanceof CheckboxMenuItem) {
                ((CheckboxMenuItem) item).setState(false);
            }
        }
        double speed = config.getSpeedMultiplier();
        int idx = speed == 0.5 ? 0 : speed == 2.0 ? 2 : 1;
        if (idx < speedMenu.getItemCount() && speedMenu.getItem(idx) instanceof CheckboxMenuItem cb) {
            cb.setState(true);
        }
    }

    private void rebuildMenu() {
        SwingUtilities.invokeLater(() -> {
            popupMenu.removeAll();
            I18nManager i18n = I18nManager.getInstance();

            MenuItem dashboardItem = new MenuItem(i18n.get("dashboard.title"));
            dashboardItem.addActionListener(e -> DashboardWindow.showOrFocus());
            popupMenu.add(dashboardItem);
            popupMenu.addSeparator();

            Menu animationMenu = new Menu(i18n.get("menu.animation"));
            addAnimationItems(animationMenu);
            animationMenu.addSeparator();
            MenuItem customAnimItem = new MenuItem(i18n.get("menu.animation.custom"));
            customAnimItem.addActionListener(e -> showCustomAnimationDialog());
            animationMenu.add(customAnimItem);
            popupMenu.add(animationMenu);

            Menu speedMenu = new Menu(i18n.get("menu.speed"));
            addSpeedItems(speedMenu);
            popupMenu.add(speedMenu);

            Menu languageMenu = new Menu(i18n.get("menu.language"));
            for (String locale : i18n.getSupportedLocales()) {
                CheckboxMenuItem langItem = new CheckboxMenuItem(
                        i18n.getLocaleDisplayName(locale), locale.equals(config.getLanguage()));
                langItem.addItemListener(e -> { config.setLanguage(locale); RunCatApp.restart(); });
                languageMenu.add(langItem);
            }
            popupMenu.add(languageMenu);

            popupMenu.addSeparator();

            Menu settingsMenu = new Menu(i18n.get("menu.settings"));
            CheckboxMenuItem autoStartItem = new CheckboxMenuItem(
                    i18n.get("menu.settings.autoStart"), config.isAutoStart());
            autoStartItem.addItemListener(e -> {
                config.setAutoStart(autoStartItem.getState());
                AutoStartManager.setAutoStart(autoStartItem.getState());
            });
            settingsMenu.add(autoStartItem);
            CheckboxMenuItem cpuItem = new CheckboxMenuItem(
                    i18n.get("menu.settings.showCpu"), config.isShowCpuTooltip());
            cpuItem.addItemListener(e -> config.setShowCpuTooltip(cpuItem.getState()));
            settingsMenu.add(cpuItem);
            CheckboxMenuItem memItem = new CheckboxMenuItem(
                    i18n.get("menu.settings.showMemory"), config.isShowMemoryTooltip());
            memItem.addItemListener(e -> config.setShowMemoryTooltip(memItem.getState()));
            settingsMenu.add(memItem);

            settingsMenu.addSeparator();
            CheckboxMenuItem cpuAlertItem = new CheckboxMenuItem(
                    i18n.get("menu.settings.cpuAlert"), config.isCpuAlertEnabled());
            cpuAlertItem.addItemListener(e -> config.setCpuAlertEnabled(cpuAlertItem.getState()));
            settingsMenu.add(cpuAlertItem);

            settingsMenu.addSeparator();
            Menu themeMenu = new Menu(i18n.get("menu.settings.iconTheme"));
            CheckboxMenuItem lightItem = new CheckboxMenuItem(
                    i18n.get("menu.settings.iconTheme.light"), "light".equals(config.getIconTheme()));
            lightItem.addItemListener(e -> { config.setIconTheme("light"); RunCatApp.restart(); });
            CheckboxMenuItem darkItem = new CheckboxMenuItem(
                    i18n.get("menu.settings.iconTheme.dark"), "dark".equals(config.getIconTheme()));
            darkItem.addItemListener(e -> { config.setIconTheme("dark"); RunCatApp.restart(); });
            themeMenu.add(lightItem);
            themeMenu.add(darkItem);
            settingsMenu.add(themeMenu);

            popupMenu.add(settingsMenu);
            popupMenu.addSeparator();

            MenuItem aboutItem = new MenuItem(i18n.get("menu.about"));
            aboutItem.addActionListener(e -> showAboutDialog());
            popupMenu.add(aboutItem);

            MenuItem exitItem = new MenuItem(i18n.get("menu.exit"));
            exitItem.addActionListener(e -> { DashboardWindow.hideInstance(); stop(); System.exit(0); });
            popupMenu.add(exitItem);
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
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
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
    }
}
