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
 * Built-in animations: cat, dog, horse, parrot
 * Custom animations: user-imported PNG sequences
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
        String[] names = {"cat", "dog", "horse", "parrot"};
        for (String name : names) {
            List<Image> frames = loadResourceAnimation(name);
            if (!frames.isEmpty()) {
                builtInAnimations.put(name, frames);
            }
        }
    }

    private List<Image> loadResourceAnimation(String name) {
        List<Image> frames = new ArrayList<>();
        // Try to load frames from resources: /animations/cat/cat_0.png, cat_1.png, ...
        for (int i = 0; i < 20; i++) {  // max 20 frames per animation
            String path = "/animations/" + name + "/" + name + "_" + i + ".png";
            try (InputStream is = getClass().getResourceAsStream(path)) {
                if (is == null) break;
                BufferedImage img = ImageIO.read(is);
                if (img != null) {
                    frames.add(img.getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH));
                }
            } catch (IOException e) {
                break;
            }
        }

        // Fallback: generate simple placeholder frames if no resource images
        if (frames.isEmpty()) {
            frames = generatePlaceholderFrames(name);
        }
        return frames;
    }

    /**
     * Generate simple placeholder animation frames programmatically
     * when real image resources aren't available
     */
    private List<Image> generatePlaceholderFrames(String name) {
        List<Image> frames = new ArrayList<>();
        Color color = switch (name) {
            case "cat" -> Color.ORANGE;
            case "dog" -> new Color(139, 90, 43);
            case "horse" -> new Color(101, 67, 33);
            case "parrot" -> Color.GREEN;
            default -> Color.GRAY;
        };

        // Create 4 animation frames with simple pixel art
        for (int f = 0; f < 4; f++) {
            BufferedImage img = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color);

            // Body
            g.fillOval(2, 4, 10, 8);

            // Head
            g.fillOval(9, 2, 6, 6);

            // Eye
            g.setColor(Color.WHITE);
            g.fillOval(12, 3, 2, 2);

            // Legs - animate position based on frame
            g.setColor(color.darker());
            int legOffset = (f % 2 == 0) ? 0 : 2;
            g.fillRect(3 + legOffset, 12, 2, 3);
            g.fillRect(8 - legOffset, 12, 2, 3);

            // Tail for cat
            if (name.equals("cat")) {
                g.setColor(color);
                int tailY = (f % 2 == 0) ? 3 : 5;
                g.drawArc(0, tailY, 4, 4, 90, 90);
            }

            // Ears for cat
            if (name.equals("cat")) {
                g.setColor(color);
                g.fillPolygon(new int[]{10, 11, 13}, new int[]{2, 0, 2}, 3);
                g.fillPolygon(new int[]{13, 14, 16}, new int[]{2, 0, 2}, 3);
            }

            // Beak for parrot
            if (name.equals("parrot")) {
                g.setColor(Color.YELLOW);
                g.fillPolygon(new int[]{14, 16, 14}, new int[]{4, 5, 6}, 3);
            }

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
            // Sort by filename for correct order
            pngFiles.sort(Comparator.comparing(p -> p.getFileName().toString()));

            for (Path pngFile : pngFiles) {
                BufferedImage img = ImageIO.read(pngFile.toFile());
                if (img != null) {
                    frames.add(img.getScaledInstance(ICON_SIZE, ICON_SIZE, Image.SCALE_SMOOTH));
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
            // Fallback to cat
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

    /**
     * Import custom animation from a directory of PNG files
     */
    public boolean importCustomAnimation(String name, Path sourceDir) {
        if (!Files.isDirectory(sourceDir)) return false;

        List<Image> frames = loadFramesFromDirectory(sourceDir);
        if (frames.isEmpty()) return false;

        // Copy to animations directory
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

    /**
     * Remove a custom animation
     */
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
            case "cat" -> "🐱 Cat";
            case "dog" -> "🐕 Dog";
            case "horse" -> "🐴 Horse";
            case "parrot" -> "🦜 Parrot";
            default -> "📦 " + name;
        };
    }
}
