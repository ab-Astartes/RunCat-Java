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

    // Theme: "light" or "dark" for tray icon
    private String iconTheme = "dark";

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
    public String getLanguage() { return language; }
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
}
