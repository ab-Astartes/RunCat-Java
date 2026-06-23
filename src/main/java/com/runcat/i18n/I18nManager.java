package com.runcat.i18n;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Multi-language support manager
 * Supports: zh_CN, zh_TW, en, ja
 * 
 * Uses manual properties loading instead of ResourceBundle to avoid
 * jpackage classloader issues where ResourceBundle.getBundle() falls
 * back to the default bundle in packaged applications.
 */
public class I18nManager {

    private static final I18nManager INSTANCE = new I18nManager();

    private Properties messages;
    private Properties fallbackMessages;
    private String currentLocale;

    private I18nManager() {
        // Load fallback (default English) first
        fallbackMessages = loadProperties("i18n/messages.properties");
        setLocale("zh_CN");
    }

    public static I18nManager getInstance() {
        return INSTANCE;
    }

    public void setLocale(String localeStr) {
        // Normalize: treat null/empty/invalid as zh_CN
        if (localeStr == null || localeStr.isBlank()) {
            localeStr = "zh_CN";
        }
        Set<String> valid = Set.of("zh_CN", "zh_TW", "en", "ja");
        if (!valid.contains(localeStr)) {
            localeStr = "zh_CN";
        }

        this.currentLocale = localeStr;

        String resourcePath = switch (localeStr) {
            case "zh_TW" -> "i18n/messages_zh_TW.properties";
            case "en" -> "i18n/messages_en.properties";
            case "ja" -> "i18n/messages_ja.properties";
            default -> "i18n/messages_zh_CN.properties";
        };

        Properties loaded = loadProperties(resourcePath);
        if (loaded != null && !loaded.isEmpty()) {
            this.messages = loaded;
        } else {
            // Fallback: try zh_CN
            if (!localeStr.equals("zh_CN")) {
                Properties zhCn = loadProperties("i18n/messages_zh_CN.properties");
                if (zhCn != null && !zhCn.isEmpty()) {
                    this.messages = zhCn;
                    this.currentLocale = "zh_CN";
                    return;
                }
            }
            // Last resort: use default (English)
            this.messages = new Properties(fallbackMessages);
        }
    }

    /**
     * Load properties file from classpath with UTF-8 encoding
     */
    private Properties loadProperties(String resourcePath) {
        Properties props = new Properties();
        try {
            // Try ClassLoader.getResourceAsStream — works reliably in jpackage
            InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (is == null) {
                // Try system classloader
                is = ClassLoader.getSystemResourceAsStream(resourcePath);
            }
            if (is == null) {
                // Try context classloader
                ClassLoader ctx = Thread.currentThread().getContextClassLoader();
                if (ctx != null) {
                    is = ctx.getResourceAsStream(resourcePath);
                }
            }
            if (is != null) {
                try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    props.load(reader);
                }
                return props;
            }
        } catch (IOException e) {
            // ignore
        }

        // Fallback: try ResourceBundle mechanism
        try {
            String baseName = resourcePath.replace("/", ".").replace(".properties", "");
            Locale locale = switch (currentLocale != null ? currentLocale : "zh_CN") {
                case "zh_TW" -> Locale.TRADITIONAL_CHINESE;
                case "en" -> Locale.ENGLISH;
                case "ja" -> Locale.JAPANESE;
                default -> Locale.SIMPLIFIED_CHINESE;
            };
            ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale);
            Enumeration<String> keys = bundle.getKeys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                props.setProperty(key, bundle.getString(key));
            }
        } catch (Exception e) {
            // ignore
        }

        return props;
    }

    public String get(String key) {
        // Try current locale first
        String value = messages.getProperty(key);
        if (value != null) return value;
        // Fallback to default
        value = fallbackMessages.getProperty(key);
        if (value != null) return value;
        // Return key as last resort
        return key;
    }

    public String get(String key, Object... args) {
        String template = get(key);
        return String.format(template, args);
    }

    public String getCurrentLocale() {
        return currentLocale;
    }

    public String[] getSupportedLocales() {
        return new String[]{"zh_CN", "zh_TW", "en", "ja"};
    }

    public String getLocaleDisplayName(String localeCode) {
        return switch (localeCode) {
            case "zh_CN" -> "简体中文";
            case "zh_TW" -> "繁體中文";
            case "en" -> "English";
            case "ja" -> "日本語";
            default -> localeCode;
        };
    }
}
