package com.runcat.i18n;

import java.io.*;
import java.util.*;

/**
 * Multi-language support manager.
 * Supports: zh_CN, zh_TW, en, ja.
 *
 * Properties files use Unicode escape sequences for non-ASCII characters,
 * ensuring correct display in AWT PopupMenu regardless of the JVM's
 * file.encoding setting (which may be GBK on Chinese Windows when launched
 * via jpackage).
 */
public class I18nManager {

    private static final I18nManager INSTANCE = new I18nManager();

    private Properties messages;
    private Properties fallbackMessages;
    private String currentLocale;

    private I18nManager() {
        fallbackMessages = loadProperties("i18n/messages.properties");
        setLocale("zh_CN");
    }

    public static I18nManager getInstance() {
        return INSTANCE;
    }

    public void setLocale(String localeStr) {
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
            if (!localeStr.equals("zh_CN")) {
                Properties zhCn = loadProperties("i18n/messages_zh_CN.properties");
                if (zhCn != null && !zhCn.isEmpty()) {
                    this.messages = zhCn;
                    this.currentLocale = "zh_CN";
                    return;
                }
            }
            this.messages = new Properties(fallbackMessages);
        }
    }

    /**
     * Load properties file from classpath.
     * Uses Properties.load(InputStream) which natively handles Unicode escapes.
     * This is encoding-safe regardless of the JVM's file.encoding setting.
     */
    private Properties loadProperties(String resourcePath) {
        Properties props = new Properties();
        InputStream is = null;
        try {
            is = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (is == null) {
                is = ClassLoader.getSystemResourceAsStream(resourcePath);
            }
            if (is == null) {
                ClassLoader ctx = Thread.currentThread().getContextClassLoader();
                if (ctx != null) {
                    is = ctx.getResourceAsStream(resourcePath);
                }
            }
            if (is != null) {
                props.load(is);
                return props;
            }
        } catch (IOException e) {
            // ignore
        } finally {
            if (is != null) {
                try { is.close(); } catch (IOException e) { /* ignore */ }
            }
        }
        return props;
    }

    public String get(String key) {
        String value = messages.getProperty(key);
        if (value != null) return value;
        value = fallbackMessages.getProperty(key);
        if (value != null) return value;
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
        // Use code points to avoid source-level Unicode escape issues
        return switch (localeCode) {
            case "zh_CN" -> new String(new int[]{0x7B80, 0x4F53, 0x4E2D, 0x6587}, 0, 4);
            case "zh_TW" -> new String(new int[]{0x7E41, 0x9AD4, 0x4E2D, 0x6587}, 0, 4);
            case "en" -> "English";
            case "ja" -> new String(new int[]{0x65E5, 0x672C, 0x8A9E}, 0, 3);
            default -> localeCode;
        };
    }
}
