package com.runcat.i18n;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Multi-language support manager
 * Supports: zh_CN, zh_TW, en, ja
 */
public class I18nManager {

    private static final I18nManager INSTANCE = new I18nManager();

    private ResourceBundle bundle;
    private String currentLocale;

    private I18nManager() {
        setLocale("zh_CN");
    }

    public static I18nManager getInstance() {
        return INSTANCE;
    }

    public void setLocale(String localeStr) {
        this.currentLocale = localeStr;
        Locale locale = switch (localeStr) {
            case "zh_TW" -> Locale.TRADITIONAL_CHINESE;
            case "en" -> Locale.ENGLISH;
            case "ja" -> Locale.JAPANESE;
            default -> Locale.SIMPLIFIED_CHINESE;
        };
        try {
            this.bundle = ResourceBundle.getBundle("i18n.messages", locale,
                    new UTF8Control());
        } catch (MissingResourceException e) {
            this.bundle = ResourceBundle.getBundle("i18n.messages", Locale.SIMPLIFIED_CHINESE,
                    new UTF8Control());
        }
    }

    public String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
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

    /**
     * Custom ResourceBundle.Control to handle UTF-8 properties files
     */
    private static class UTF8Control extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                         ClassLoader loader, boolean reload) throws IOException {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            URL url = loader.getResource(resourceName);
            if (url == null) return null;

            InputStream is = url.openStream();
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                return new PropertyResourceBundle(reader);
            }
        }
    }
}
