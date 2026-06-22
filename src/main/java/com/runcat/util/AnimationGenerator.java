package com.runcat.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * Generates animation frame PNG files on first run
 * Creates simple but recognizable pixel art for each built-in animation
 */
public class AnimationGenerator {

    private static final String RES_DIR = "src/main/resources/animations";
    private static final int SIZE = 16;
    private static final int FRAMES = 5;

    public static void main(String[] args) {
        generateCat();
        generateDog();
        generateHorse();
        generateParrot();
        System.out.println("All animations generated!");
    }

    private static void generateCat() {
        for (int f = 0; f < FRAMES; f++) {
            BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Body - orange
            g.setColor(new Color(255, 165, 0));
            g.fillOval(1, 5, 9, 7);

            // Head
            g.fillOval(8, 2, 7, 7);

            // Ears
            g.setColor(new Color(255, 140, 0));
            int[] earX1 = {9, 10, 12};
            int[] earY1 = {2, 0, 2};
            g.fillPolygon(earX1, earY1, 3);
            int[] earX2 = {12, 14, 14};
            int[] earY2 = {2, 0, 2};
            g.fillPolygon(earX2, earY2, 3);

            // Inner ears
            g.setColor(new Color(255, 200, 150));
            int[] iearX1 = {10, 11, 12};
            int[] iearY1 = {2, 1, 2};
            g.fillPolygon(iearX1, iearY1, 3);
            int[] iearX2 = {13, 13, 14};
            int[] iearY2 = {2, 1, 2};
            g.fillPolygon(iearX2, iearY2, 3);

            // Eyes
            g.setColor(Color.WHITE);
            g.fillOval(10, 4, 3, 2);
            g.setColor(Color.BLACK);
            g.fillOval(11, 4, 2, 2);

            // Nose
            g.setColor(new Color(255, 150, 150));
            g.fillOval(14, 5, 1, 1);

            // Whiskers
            g.setColor(new Color(200, 100, 0));
            g.drawLine(14, 5, 16, 4);
            g.drawLine(14, 6, 16, 6);

            // Legs (animated)
            g.setColor(new Color(255, 140, 0));
            int legPhase = f % FRAMES;
            double leftLegOffset = Math.sin(legPhase * 2 * Math.PI / FRAMES) * 2;
            double rightLegOffset = Math.sin((legPhase + FRAMES/2) * 2 * Math.PI / FRAMES) * 2;
            g.fillRect(2, 12, 2, 3 + (int)leftLegOffset);
            g.fillRect(7, 12, 2, 3 + (int)rightLegOffset);

            // Tail
            g.setColor(new Color(255, 140, 0));
            int tailY = 3 + (int)(Math.sin(legPhase * Math.PI / 2) * 2);
            g.drawArc(-2, tailY, 5, 5, 0, 90);

            g.dispose();
            saveFrame(img, "cat", f);
        }
    }

    private static void generateDog() {
        for (int f = 0; f < FRAMES; f++) {
            BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Body - brown
            g.setColor(new Color(139, 90, 43));
            g.fillOval(1, 5, 10, 7);

            // Head
            g.fillOval(9, 2, 6, 7);

            // Floppy ears
            g.setColor(new Color(100, 60, 20));
            g.fillOval(8, 3, 3, 5);
            g.fillOval(13, 3, 3, 5);

            // Eye
            g.setColor(Color.WHITE);
            g.fillOval(11, 4, 2, 2);
            g.setColor(Color.BLACK);
            g.fillOval(11, 4, 1, 2);

            // Nose
            g.setColor(Color.BLACK);
            g.fillOval(14, 5, 2, 2);

            // Tongue (animated)
            if (f % 2 == 0) {
                g.setColor(new Color(255, 100, 100));
                g.fillOval(14, 7, 1, 2);
            }

            // Legs
            g.setColor(new Color(120, 75, 35));
            int legPhase = f % FRAMES;
            double leftLegOffset = Math.sin(legPhase * 2 * Math.PI / FRAMES) * 2;
            double rightLegOffset = Math.sin((legPhase + FRAMES/2) * 2 * Math.PI / FRAMES) * 2;
            g.fillRect(2, 12, 2, 3 + (int)leftLegOffset);
            g.fillRect(8, 12, 2, 3 + (int)rightLegOffset);

            // Tail (wagging)
            g.setColor(new Color(160, 100, 50));
            int tailAngle = (int)(Math.sin(f * Math.PI / 2) * 15);
            g.rotate(Math.toRadians(tailAngle), 1, 6);
            g.fillRect(-1, 5, 3, 2);
            g.dispose();
            saveFrame(img, "dog", f);
        }
    }

    private static void generateHorse() {
        for (int f = 0; f < FRAMES; f++) {
            BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Body - dark brown
            g.setColor(new Color(101, 67, 33));
            g.fillOval(0, 4, 11, 8);

            // Head / neck
            g.setColor(new Color(120, 80, 40));
            g.fillOval(9, 0, 6, 6);
            g.fillRect(10, 4, 4, 3);

            // Mane
            g.setColor(new Color(50, 30, 10));
            g.fillRect(9, 0, 2, 5);

            // Eye
            g.setColor(Color.WHITE);
            g.fillOval(12, 2, 2, 1);
            g.setColor(Color.BLACK);
            g.fillOval(12, 2, 1, 1);

            // Nostril
            g.fillOval(14, 3, 1, 1);

            // Legs (galloping)
            g.setColor(new Color(90, 55, 25));
            int legPhase = f % FRAMES;
            double[] legOffsets = new double[4];
            for (int i = 0; i < 4; i++) {
                legOffsets[i] = Math.sin((legPhase + i) * 2 * Math.PI / FRAMES) * 2;
            }
            g.fillRect(1, 12, 2, (int)(3 + legOffsets[0]));
            g.fillRect(4, 12, 2, (int)(3 + legOffsets[1]));
            g.fillRect(7, 12, 2, (int)(3 + legOffsets[2]));
            g.fillRect(9, 12, 2, (int)(3 + legOffsets[3]));

            // Tail
            g.setColor(new Color(50, 30, 10));
            int tailSwing = (int)(Math.sin(f * Math.PI) * 2);
            g.drawArc(-3, 4 + tailSwing, 4, 4, 90, 90);

            g.dispose();
            saveFrame(img, "horse", f);
        }
    }

    private static void generateParrot() {
        for (int f = 0; f < FRAMES; f++) {
            BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Body - green
            g.setColor(new Color(0, 180, 0));
            g.fillOval(3, 4, 8, 8);

            // Head
            g.setColor(new Color(0, 200, 0));
            g.fillOval(8, 2, 6, 6);

            // Eye ring
            g.setColor(Color.WHITE);
            g.fillOval(11, 3, 3, 3);

            // Eye
            g.setColor(Color.BLACK);
            g.fillOval(12, 4, 1, 1);

            // Beak
            g.setColor(new Color(255, 200, 0));
            int[] beakX = {14, 16, 14};
            int[] beakY = {4, 5, 6};
            g.fillPolygon(beakX, beakY, 3);

            // Wing (animated flapping)
            g.setColor(new Color(0, 150, 0));
            int wingOffset = (int)(Math.sin(f * 2 * Math.PI / FRAMES) * 3);
            g.fillOval(2, 4 + wingOffset, 5, 4);

            // Tail feathers
            g.setColor(new Color(255, 0, 0));
            g.fillRect(3, 12, 2, 3);
            g.setColor(new Color(0, 0, 200));
            g.fillRect(5, 12, 2, 2);
            g.setColor(new Color(255, 255, 0));
            g.fillRect(7, 12, 2, 3);

            // Feet
            g.setColor(new Color(200, 150, 0));
            g.fillRect(5, 14, 1, 1);
            g.fillRect(8, 14, 1, 1);

            g.dispose();
            saveFrame(img, "parrot", f);
        }
    }

    private static void saveFrame(BufferedImage img, String animal, int frame) {
        String dir = RES_DIR + "/" + animal;
        new File(dir).mkdirs();
        String path = dir + "/" + animal + "_" + frame + ".png";
        try {
            ImageIO.write(img, "png", new File(path));
            System.out.println("Generated: " + path);
        } catch (IOException e) {
            System.err.println("Failed to save " + path + ": " + e.getMessage());
        }
    }
}
