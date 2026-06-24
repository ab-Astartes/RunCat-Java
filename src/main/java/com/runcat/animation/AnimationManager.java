package com.runcat.animation;

import com.runcat.config.AppConfig;

import javax.imageio.ImageIO;
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
    private List<Image> currentFrames;
    private int currentFrameIndex = 0;

    private final Map<String, List<Image>> builtInAnimations;
    private final Map<String, List<Image>> customAnimations;

    public AnimationManager(AppConfig config) {
        this.config = config;
        this.builtInAnimations = new LinkedHashMap<>();
        this.customAnimations = new LinkedHashMap<>();
        loadBuiltInAnimations();
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

    private void loadCustomAnimations() {
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
            List<Path> pngFiles = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.png")) {
                for (Path p : stream) pngFiles.add(p);
            }
            pngFiles.sort(Comparator.comparing(p -> p.getFileName().toString()));

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
        List<Image> frames = null;
        if (builtInAnimations.containsKey(name)) {
            frames = builtInAnimations.get(name);
        } else if (customAnimations.containsKey(name)) {
            frames = customAnimations.get(name);
        }

        if (frames == null || frames.isEmpty()) {
            frames = builtInAnimations.getOrDefault("cat", generatePlaceholderFrames("cat"));
        }

        this.currentFrames = frames;
        this.currentFrameIndex = 0;
        config.setCurrentAnimation(name);
    }

    public Image getNextFrame() {
        if (currentFrames == null || currentFrames.isEmpty()) return null;
        Image frame = currentFrames.get(currentFrameIndex);
        currentFrameIndex = (currentFrameIndex + 1) % currentFrames.size();
        return frame;
    }

    public Image getCurrentFrame() {
        if (currentFrames == null || currentFrames.isEmpty()) return null;
        return currentFrames.get(currentFrameIndex);
    }

    public boolean importCustomAnimation(String name, Path sourceDir) {
        if (!Files.isDirectory(sourceDir)) return false;
        List<Image> frames = loadFramesFromDirectory(sourceDir);
        if (frames.isEmpty()) return false;

        Path targetDir = Paths.get(ANIMATIONS_DIR, name);
        try {
            Files.createDirectories(targetDir);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDir, "*.png")) {
                for (Path src : stream) {
                    Path dest = targetDir.resolve(src.getFileName());
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                }
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

    public String getAnimationDisplayName(String name) {
        return switch (name) {
            case "cat" -> "\uD83D\uDC31 Cat";
            case "cat_sleep" -> "\uD83D\uDE34 Sleepy Cat";
            case "dog" -> "\uD83D\uDC15 Dog";
            case "horse" -> "\uD83D\uDC0E Horse";
            case "parrot" -> "\uD83E\uDD9C Parrot";
            case "rabbit" -> "\uD83D\uDC30 Rabbit";
            case "penguin" -> "\uD83D\uDC27 Penguin";
            default -> "\uD83D\uDCE6 " + name;
        };
    }
}
