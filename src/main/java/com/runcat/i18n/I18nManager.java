package com.runcat.i18n;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Multi-language support manager
 * Supports: zh_CN, zh_TW, en, ja
 * 
 * Fix: Clear ResourceBundle cache on locale change to avoid stale cached bundles.
 * Fix: Ensure UTF8Control is used for all bundle lookups including parent chain.
 * Fix: Handle null/invalid locale strings gracefully with zh_CN as default.
 */
public class I18nManager {

    private static final I18nManager INSTANCE = new I18nManager();

    private ResourceBundle bundle;
    private String currentLocale;
    private static final UTF8Control UTF8_CONTROL = new UTF8Control();

    private I18nManager() {
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
        // Only accept known locales
        Set<String> valid = Set.of("zh_CN", "zh_TW", "en", "ja");
        if (!valid.contains(localeStr)) {
            localeStr = "zh_CN";
        }
        
        this.currentLocale = localeStr;
        Locale locale = switch (localeStr) {
            case "zh_TW" -> Locale.TRADITIONAL_CHINESE;
            case "en" -> Locale.ENGLISH;
            case "ja" -> Locale.JAPANESE;
            default -> Locale.SIMPLIFIED_CHINESE;
        };

        // Clear ResourceBundle cache to avoid stale cached bundles from previous locale
        ResourceBundle.clearCache();

        try {
            this.bundle = ResourceBundle.getBundle("i18n.messages", locale, UTF8_CONTROL);
        } catch (MissingResourceException e) {
            // Fallback: try loading zh_CN directly
            try {
                this.bundle = ResourceBundle.getBundle("i18n.messages", 
                    Locale.SIMPLIFIED_CHINESE, UTF8_CONTROL);
            } catch (MissingResourceException e2) {
                // Last resort: load default bundle
                this.bundle = ResourceBundle.getBundle("i18n.messages", 
                    Locale.ROOT, UTF8_CONTROL);
            }
        }
    }

    public String get(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            // Try default bundle as fallback for missing keys
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
     * Custom ResourceBundle.Control to handle UTF-8 properties files.
     * Overrides getCandidateLocales to ensure proper fallback chain.
     */
    private static class UTF8Control extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format,
                                         ClassLoader loader, boolean reload) throws IOException {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");
            
            // Try to find the specific locale file first
            URL url = loader.getResource(resourceName);
            if (url == null) return null;

            try (InputStream is = url.openStream();
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                return new PropertyResourceBundle(reader);
            }
        }
        
        @Override
        public List<Locale> getCandidateLocales(String baseName, Locale locale) {
            // Custom candidate list to ensure proper fallback:
            // zh_CN -> zh -> root (default bundle)
            // en -> root
            // ja -> root
            List<Locale> candidates = super.getCandidateLocales(baseName, locale);
            // Ensure root locale is always in the chain
            if (!candidates.contains(Locale.ROOT)) {
                candidates = new ArrayList<>(candidates);
                candidates.add(Locale.ROOT);
            }
            return candidates;
        }
    }
}
