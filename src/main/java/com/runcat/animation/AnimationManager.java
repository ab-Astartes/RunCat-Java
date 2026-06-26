package com.runcat.animation;

import com.runcat.config.AppConfig;
import com.runcat.i18n.I18nManager;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/**
 * Manages animation frames - built-in and custom
 * Built-in animations: cat, cat_sleep, dog, horse, parrot
 * Custom animations: user-imported PNG sequences
 * Frame caching: pre-scaled BufferedImage to avoid repeated scaling
 */
public class AnimationManager {

    private static final String ANIMATIONS_DIR = System.getProperty("user.home") + "\\.java-runcat\\animations";
    private static final int ICON_SIZE = 16;

    private final AppConfig config;
    private final Map<String, List<Image>> builtInAnimations;
    private final Map<String, List<Image>> customAnimations;
    private final Map<String, List<Image>> hiResCache; // 64x64 frames for desktop pet

    public AnimationManager(AppConfig config) {
        this.config = config;
        this.builtInAnimations = new LinkedHashMap<>();
        this.customAnimations = new LinkedHashMap<>();
        this.hiResCache = new LinkedHashMap<>();
        loadBuiltInAnimations();
        loadHiResAnimations();
        loadCustomAnimations();
        setCurrentAnimation(config.getCurrentAnimation());
    }

    private void loadBuiltInAnimations() {
        String[] names = {"cat", "cat_sleep", "dog", "horse", "parrot", "rabbit", "penguin"};
        for (String name : names) {
            List<Image> frames = loadResourceAnimation(name);
            if (!frames.isEmpty()) {
                builtInAnimations.put(name, frames);
            }
        }
    }

    private List<Image> loadResourceAnimation(String name) {
        List<Image> frames = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            String path = "/animations/" + name + "/" + name + "_" + i + ".png";
            try (InputStream is = getClass().getResourceAsStream(path)) {
                if (is == null) break;
                BufferedImage img = ImageIO.read(is);
                if (img != null) {
                    // Cache as pre-scaled BufferedImage for smooth rendering
                    BufferedImage scaled = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = scaled.createGraphics();
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.drawImage(img, 0, 0, ICON_SIZE, ICON_SIZE, null);
                    g.dispose();
                    frames.add(scaled);
                }
            } catch (IOException e) {
                break;
            }
        }

        if (frames.isEmpty()) {
            frames = generatePlaceholderFrames(name);
        }
        return frames;
    }

    private List<Image> generatePlaceholderFrames(String name) {
        List<Image> frames = new ArrayList<>();
        Color color = switch (name) {
            case "cat", "cat_sleep" -> new Color(255, 140, 40);
            case "dog" -> new Color(200, 150, 80);
            case "horse" -> new Color(170, 120, 70);
            case "parrot" -> new Color(30, 180, 90);
            case "rabbit" -> new Color(240, 240, 245);
            case "penguin" -> new Color(30, 35, 40);
            default -> Color.GRAY;
        };

        for (int f = 0; f < 4; f++) {
            BufferedImage img = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color);
            g.fillOval(2, 4, 10, 8);
            g.fillOval(9, 2, 6, 6);
            g.setColor(Color.WHITE);
            g.fillOval(12, 3, 2, 2);
            g.setColor(color.darker());
            int offset = (f % 2 == 0) ? 0 : 2;
            g.fillRect(3 + offset, 12, 2, 3);
            g.fillRect(8 - offset, 12, 2, 3);
            g.dispose();
            frames.add(img);
        }
        return frames;
    }

    private void loadHiResAnimations() {
        String[] names = {"cat", "cat_sleep", "dog", "horse", "parrot", "rabbit", "penguin"};
        for (String name : names) {
            List<Image> frames = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                String path = "/animations_hd/" + name + "/" + name + "_" + i + ".png";
                try (InputStream is = getClass().getResourceAsStream(path)) {
                    if (is == null) break;
                    BufferedImage img = ImageIO.read(is);
                    if (img != null) frames.add(img);
                } catch (IOException e) {
                    break;
                }
            }
            if (!frames.isEmpty()) {
                hiResCache.put(name, frames);
            }
        }
    }

    private void loadCustomAnimations() {
        customAnimations.clear();
        Path animDir = Paths.get(ANIMATIONS_DIR);
        if (!Files.exists(animDir)) return;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(animDir)) {
            for (Path dir : stream) {
                if (Files.isDirectory(dir)) {
                    String animName = dir.getFileName().toString();
                    List<Image> frames = loadFramesFromDirectory(dir);
                    if (!frames.isEmpty()) {
                        customAnimations.put(animName, frames);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<Image> loadFramesFromDirectory(Path dir) {
        List<Image> frames = new ArrayList<>();
        try {
            List<Path> pngFiles = listAnimationFrames(dir);
            for (Path pngFile : pngFiles) {
                BufferedImage img = ImageIO.read(pngFile.toFile());
                if (img != null) {
                    // Pre-scale and cache
                    BufferedImage scaled = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = scaled.createGraphics();
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.drawImage(img, 0, 0, ICON_SIZE, ICON_SIZE, null);
                    g.dispose();
                    frames.add(scaled);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return frames;
    }

    public void setCurrentAnimation(String name) {
        if (!builtInAnimations.containsKey(name) && !customAnimations.containsKey(name)) {
            name = "cat";
        }
        config.setCurrentAnimation(name);
    }

    public Image getNextFrame() {
        return createTrayPlayback().nextFrame();
    }

    public Image getCurrentFrame() {
        return getTrayFrames().stream().findFirst().orElse(null);
    }

    public boolean importCustomAnimation(String name, Path sourceDir) {
        if (!Files.isDirectory(sourceDir)) return false;
        List<Image> frames = loadFramesFromDirectory(sourceDir);
        if (frames.size() < 2) return false;

        Path targetDir = Paths.get(ANIMATIONS_DIR, name);
        try {
            Files.createDirectories(targetDir);
            for (Path src : listAnimationFrames(sourceDir)) {
                Path dest = targetDir.resolve(src.getFileName());
                Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
            }
            customAnimations.put(name, frames);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeCustomAnimation(String name) {
        if (!customAnimations.containsKey(name)) return false;
        Path dir = Paths.get(ANIMATIONS_DIR, name);
        try {
            deleteDirectory(dir);
            customAnimations.remove(name);
            if (config.getCurrentAnimation().equals(name)) {
                setCurrentAnimation("cat");
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                for (Path p : stream) {
                    if (Files.isDirectory(p)) deleteDirectory(p);
                    else Files.delete(p);
                }
            }
            Files.delete(dir);
        }
    }

    public Map<String, List<Image>> getBuiltInAnimations() {
        return Collections.unmodifiableMap(builtInAnimations);
    }

    public Map<String, List<Image>> getCustomAnimations() {
        return Collections.unmodifiableMap(customAnimations);
    }

    public String getCurrentAnimationName() {
        return config.getCurrentAnimation();
    }

    public AnimationPlayback createTrayPlayback() {
        return new AnimationPlayback(this::getTrayFrames);
    }

    public AnimationPlayback createPetPlayback() {
        return new AnimationPlayback(this::getPetFrames);
    }

    public String getAnimationDisplayName(String name) {
        I18nManager i18n = I18nManager.getInstance();
        String key = "anim." + name;
        String value = i18n.get(key);
        // If the key was not found (returns the key itself), fall back to raw name
        if (value.equals(key)) {
            return "\uD83D\uDCE6 " + name;
        }
        return value;
    }

    /**
     * Get the next hi-res (64x64) frame for the desktop pet.
     * Uses the same animation as the tray icon.
     */
    public Image getNextHiResFrame() {
        return createPetPlayback().nextFrame();
    }

    public List<String> getAllAnimationNames() {
        List<String> names = new ArrayList<>(builtInAnimations.keySet());
        names.addAll(customAnimations.keySet());
        return names;
    }

    public boolean isBuiltInAnimation(String name) {
        return builtInAnimations.containsKey(name);
    }

    public ImageIcon createPreviewIcon(String animationName, int size) {
        List<Image> frames = customAnimations.containsKey(animationName)
                ? customAnimations.get(animationName)
                : builtInAnimations.get(animationName);
        if (frames == null || frames.isEmpty()) {
            return null;
        }
        Image frame = frames.get(0);
        BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(frame, 0, 0, size, size, null);
        g.dispose();
        return new ImageIcon(scaled);
    }

    public List<Path> listAnimationFrames(Path dir) {
        List<Path> pngFiles = new ArrayList<>();
        if (dir == null || !Files.isDirectory(dir)) {
            return pngFiles;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.png")) {
            for (Path path : stream) {
                pngFiles.add(path);
            }
        } catch (IOException ignored) {
            return List.of();
        }
        pngFiles.sort(Comparator.comparing(path -> naturalSortKey(path.getFileName().toString())));
        return pngFiles;
    }

    public boolean renameCustomAnimation(String oldName, String newName) {
        if (!customAnimations.containsKey(oldName) || newName == null || newName.isBlank()) {
            return false;
        }
        Path source = Paths.get(ANIMATIONS_DIR, oldName);
        Path target = Paths.get(ANIMATIONS_DIR, newName);
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            loadCustomAnimations();
            if (config.getCurrentAnimation().equals(oldName)) {
                setCurrentAnimation(newName);
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private List<Image> getTrayFrames() {
        return getFrames(config.getCurrentAnimation(), false);
    }

    private List<Image> getPetFrames() {
        List<Image> frames = getFrames(config.getCurrentAnimation(), true);
        if (frames.isEmpty()) {
            return getFrames("cat", true);
        }
        return frames;
    }

    private List<Image> getFrames(String name, boolean hiRes) {
        List<Image> frames = hiRes ? hiResCache.get(name) : builtInAnimations.get(name);
        if (!hiRes && customAnimations.containsKey(name)) {
            frames = customAnimations.get(name);
        }
        if (frames != null && !frames.isEmpty()) {
            return frames;
        }
        if (hiRes) {
            List<Image> lowRes = getFrames(name, false);
            if (lowRes.isEmpty()) {
                return List.of();
            }
            List<Image> scaled = new ArrayList<>();
            for (Image frame : lowRes) {
                BufferedImage scaledFrame = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = scaledFrame.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.drawImage(frame, 0, 0, 64, 64, null);
                g.dispose();
                scaled.add(scaledFrame);
            }
            return scaled;
        }
        return builtInAnimations.getOrDefault("cat", generatePlaceholderFrames("cat"));
    }

    private String naturalSortKey(String fileName) {
        StringBuilder key = new StringBuilder();
        StringBuilder digits = new StringBuilder();
        for (char c : fileName.toCharArray()) {
            if (Character.isDigit(c)) {
                digits.append(c);
            } else {
                if (digits.length() > 0) {
                    key.append(String.format("%08d", Integer.parseInt(digits.toString())));
                    digits.setLength(0);
                }
                key.append(Character.toLowerCase(c));
            }
        }
        if (digits.length() > 0) {
            key.append(String.format("%08d", Integer.parseInt(digits.toString())));
        }
        return key.toString();
    }
}
