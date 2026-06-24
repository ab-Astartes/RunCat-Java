package com.runcat.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.*;

/**
 * Application configuration with persistence
 */
public class AppConfig {

    private static final String CONFIG_DIR = System.getProperty("user.home") + "\\.java-runcat";
    private static final String CONFIG_FILE = CONFIG_DIR + "\\config.json";

    // Language setting
    private String language = "zh_CN";

    // Animation setting
    private String currentAnimation = "cat";

    // Auto-start with Windows
    private boolean autoStart = false;

    // Show CPU in tooltip
    private boolean showCpuTooltip = true;

    // Show memory in tooltip
    private boolean showMemoryTooltip = true;

    // Animation speed multiplier (1.0 = normal)
    private double speedMultiplier = 1.0;

    // Theme: "auto", "light" or "dark" for tray icon
    private String iconTheme = "auto";

    // Show time in tooltip
    private boolean showTimeTooltip = false;

    // CPU alert settings
    private boolean cpuAlertEnabled = true;
    private double cpuAlertThreshold = 90.0;  // percent
    private long cpuAlertCooldown = 300;       // seconds between alerts

    // Dashboard window position (persisted)
    private int dashboardX = -1;
    private int dashboardY = -1;

    // Desktop pet mode (floating window like QQ pet)
    private boolean desktopPetEnabled = true;
    private int desktopPetSize = 64;           // pixel size of the pet
    private int desktopPetX = -1;
    private int desktopPetY = -1;
    private double desktopPetOpacity = 1.0;    // 0.0 ~ 1.0

    public AppConfig() {}

    public static AppConfig load() {
        Gson gson = new Gson();
        Path path = Paths.get(CONFIG_FILE);
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                AppConfig config = gson.fromJson(reader, AppConfig.class);
                if (config != null) return config;
            } catch (Exception e) {
                // fallback to default
            }
        }
        AppConfig defaultConfig = new AppConfig();
        defaultConfig.save();
        return defaultConfig;
    }

    public void save() {
        try {
            Files.createDirectories(Paths.get(CONFIG_DIR));
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (Writer writer = Files.newBufferedWriter(Paths.get(CONFIG_FILE))) {
                gson.toJson(this, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Getters and Setters
    public String getLanguage() { 
        return (language == null || language.isBlank()) ? "zh_CN" : language; 
    }
    public void setLanguage(String language) { this.language = language; save(); }

    public String getCurrentAnimation() { return currentAnimation; }
    public void setCurrentAnimation(String currentAnimation) { this.currentAnimation = currentAnimation; save(); }

    public boolean isAutoStart() { return autoStart; }
    public void setAutoStart(boolean autoStart) { this.autoStart = autoStart; save(); }

    public boolean isShowCpuTooltip() { return showCpuTooltip; }
    public void setShowCpuTooltip(boolean showCpuTooltip) { this.showCpuTooltip = showCpuTooltip; save(); }

    public boolean isShowMemoryTooltip() { return showMemoryTooltip; }
    public void setShowMemoryTooltip(boolean showMemoryTooltip) { this.showMemoryTooltip = showMemoryTooltip; save(); }

    public double getSpeedMultiplier() { return speedMultiplier; }
    public void setSpeedMultiplier(double speedMultiplier) { this.speedMultiplier = speedMultiplier; save(); }

    public String getIconTheme() { return iconTheme; }
    public void setIconTheme(String iconTheme) { this.iconTheme = iconTheme; save(); }

    public boolean isCpuAlertEnabled() { return cpuAlertEnabled; }
    public void setCpuAlertEnabled(boolean cpuAlertEnabled) { this.cpuAlertEnabled = cpuAlertEnabled; save(); }

    public double getCpuAlertThreshold() { return cpuAlertThreshold; }
    public void setCpuAlertThreshold(double cpuAlertThreshold) { this.cpuAlertThreshold = cpuAlertThreshold; save(); }

    public long getCpuAlertCooldown() { return cpuAlertCooldown; }
    public void setCpuAlertCooldown(long cpuAlertCooldown) { this.cpuAlertCooldown = cpuAlertCooldown; save(); }

    public boolean isShowTimeTooltip() { return showTimeTooltip; }
    public void setShowTimeTooltip(boolean showTimeTooltip) { this.showTimeTooltip = showTimeTooltip; save(); }

    public int getDashboardX() { return dashboardX; }
    public void setDashboardX(int dashboardX) { this.dashboardX = dashboardX; }

    public int getDashboardY() { return dashboardY; }
    public void setDashboardY(int dashboardY) { this.dashboardY = dashboardY; }

    public void saveDashboardPosition(int x, int y) {
        this.dashboardX = x;
        this.dashboardY = y;
        save();
    }

    public boolean isDesktopPetEnabled() { return desktopPetEnabled; }
    public void setDesktopPetEnabled(boolean desktopPetEnabled) { this.desktopPetEnabled = desktopPetEnabled; save(); }

    public int getDesktopPetSize() { return desktopPetSize; }
    public void setDesktopPetSize(int desktopPetSize) { this.desktopPetSize = desktopPetSize; save(); }

    public int getDesktopPetX() { return desktopPetX; }
    public void setDesktopPetX(int desktopPetX) { this.desktopPetX = desktopPetX; }

    public int getDesktopPetY() { return desktopPetY; }
    public void setDesktopPetY(int desktopPetY) { this.desktopPetY = desktopPetY; }

    public double getDesktopPetOpacity() { return desktopPetOpacity; }
    public void setDesktopPetOpacity(double desktopPetOpacity) { this.desktopPetOpacity = desktopPetOpacity; save(); }

    public void saveDesktopPetPosition(int x, int y) {
        this.desktopPetX = x;
        this.desktopPetY = y;
        save();
    }
}
