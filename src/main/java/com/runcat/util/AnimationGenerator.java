package com.runcat.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Generates high-quality animation frame PNG files for taskbar tray icon.
 * Each animation has 8 frames for smoother motion.
 * Animations: cat (running), cat_sleep (sleeping), dog, horse, parrot
 */
public class AnimationGenerator {

    private static final String RES_DIR = "src/main/resources/animations";
    private static final int SIZE = 16;
    private static final int FRAMES = 8;

    public static void main(String[] args) {
        generateRunningCat();
        generateSleepingCat();
        generateDog();
        generateHorse();
        generateParrot();
        System.out.println("All animations generated!");
    }

    // ======================== CAT (RUNNING) ========================
    private static void generateRunningCat() {
        // Classic orange tabby - bright and visible on both light/dark taskbars
        Color body = new Color(255, 165, 50);
        Color bodyDark = new Color(220, 130, 20);
        Color belly = new Color(255, 220, 160);
        Color eye = new Color(60, 200, 255);
        Color eyePupil = new Color(20, 20, 40);
        Color earInner = new Color(255, 170, 170);
        Color nose = new Color(255, 130, 140);
        Color paw = new Color(240, 150, 40);

        for (int f = 0; f < FRAMES; f++) {
            BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            double phase = f * 2 * Math.PI / FRAMES;

            // --- Tail (behind body) ---
            g.setColor(bodyDark);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int tailWave = (int)(Math.sin(phase) * 2);
            g.drawArc(-1, 2 + tailWave, 6, 5, 50, 90);
            // Tail tip
            g.setColor(body);
            g.drawArc(0, 1 + tailWave, 4, 3, 50, 60);

            // --- Body ---
            g.setColor(body);
            g.fillOval(2, 5, 9, 6);
            // Belly
            g.setColor(belly);
            g.fillOval(3, 7, 6, 3);

            // --- Head ---
            g.setColor(body);
            g.fillOval(9, 1, 6, 6);
            // Forehead stripe
            g.setColor(bodyDark);
            g.fillOval(10, 2, 4, 2);

            // --- Ears ---
            g.setColor(body);
            g.fillPolygon(new int[]{10, 11, 13}, new int[]{1, -1, 1}, 3);
            g.fillPolygon(new int[]{13, 14, 15}, new int[]{1, -1, 1}, 3);
            // Inner ears
            g.setColor(earInner);
            g.fillPolygon(new int[]{11, 11, 12}, new int[]{1, 0, 1}, 3);
            g.fillPolygon(new int[]{14, 14, 15}, new int[]{1, 0, 1}, 3);

            // --- Eyes ---
            if (f == 3 || f == 7) {
                // Blink
                g.setColor(eyePupil);
                g.setStroke(new BasicStroke(1f));
                g.drawLine(11, 4, 14, 4);
            } else {
                g.setColor(Color.WHITE);
                g.fillOval(11, 3, 4, 2);
                g.setColor(eye);
                g.fillOval(12, 3, 2, 2);
                g.setColor(eyePupil);
                g.fillOval(12, 3, 1, 2);
                // Eye shine
                g.setColor(Color.WHITE);
                g.fillOval(12, 3, 1, 1);
            }

            // --- Nose ---
            g.setColor(nose);
            int[] noseX = {14, 15, 14};
            int[] noseY = {5, 6, 6};
            g.fillPolygon(noseX, noseY, 3);

            // --- Whiskers ---
            g.setColor(new Color(200, 150, 100));
            g.setStroke(new BasicStroke(0.5f));
            g.drawLine(14, 6, 16, 5);
            g.drawLine(14, 6, 16, 7);

            // --- Legs (running cycle) ---
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(bodyDark);
            // Front legs
            double fl1 = Math.sin(phase) * 3;
            double fl2 = Math.sin(phase + Math.PI) * 3;
            int fl1x = 4 + (int)fl1, fl1y = 11 + (int)Math.abs(fl1 * 0.3);
            int fl2x = 6 + (int)fl2, fl2y = 11 + (int)Math.abs(fl2 * 0.3);
            g.drawLine(4, 10, fl1x, fl1y);
            g.drawLine(6, 10, fl2x, fl2y);
            // Hind legs
            double hl1 = Math.sin(phase + Math.PI * 0.5) * 3;
            double hl2 = Math.sin(phase + Math.PI * 1.5) * 3;
            int hl1x = 8 + (int)hl1, hl1y = 11 + (int)Math.abs(hl1 * 0.3);
            int hl2x = 9 + (int)hl2, hl2y = 11 + (int)Math.abs(hl2 * 0.3);
            g.drawLine(8, 10, hl1x, hl1y);
            g.drawLine(9, 10, hl2x, hl2y);

            // Paws
            g.setColor(paw);
            g.fillOval(fl1x - 1, fl1y - 1, 3, 2);
            g.fillOval(fl2x - 1, fl2y - 1, 3, 2);

            g.dispose();
            saveFrame(img, "cat", f);
        }
    }

    // ======================== SLEEPING CAT ========================
    private static void generateSleepingCat() {
        Color body = new Color(255, 165, 50);
        Color bodyDark = new Color(220, 130, 20);
        Color belly = new Color(255, 220, 160);
        Color earInner = new Color(255, 170, 170);

        for (int f = 0; f < FRAMES; f++) {
            BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            double phase = f * 2 * Math.PI / FRAMES;

            // Curled up body
            g.setColor(body);
            g.fillOval(2, 5, 12, 8);
            g.setColor(belly);
            g.fillOval(4, 6, 8, 4);

            // Head resting
            g.setColor(body);
            g.fillOval(1, 3, 7, 6);
            g.setColor(belly);
            g.fillOval(2, 5, 4, 3);

            // Ears
            g.setColor(body);
            g.fillPolygon(new int[]{2, 3, 5}, new int[]{3, 1, 3}, 3);
            g.fillPolygon(new int[]{5, 6, 7}, new int[]{3, 1, 3}, 3);
            g.setColor(earInner);
            g.fillPolygon(new int[]{3, 3, 4}, new int[]{3, 2, 3}, 3);
            g.fillPolygon(new int[]{6, 6, 7}, new int[]{3, 2, 3}, 3);

            // Closed eyes - gentle curves with breathing
            g.setColor(bodyDark);
            g.setStroke(new BasicStroke(0.8f));
            int breathOffset = (int)(Math.sin(phase) * 0.5);
            g.drawArc(2, 5 + breathOffset, 2, 1, 0, 180);
            g.drawArc(5, 5 + breathOffset, 2, 1, 0, 180);

            // Zzz bubbles
            int alpha = 150 + (int)(Math.sin(phase) * 80);
            g.setColor(new Color(100, 180, 255, alpha));
            g.setFont(g.getFont().deriveFont(Font.BOLD, 6f));
            int zzzPhase = f % 3;
            if (zzzPhase >= 1) g.drawString("z", 9, 4);
            if (zzzPhase >= 2) g.drawString("Z", 12, 2);

            // Tail wrapped around
            g.setColor(bodyDark);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int tailY = 8 + (int)(Math.sin(phase) * 0.5);
            g.drawArc(10, tailY, 5, 4, 90, 120);

            // Paws
            g.setColor(belly);
            g.fillOval(3, 9, 3, 2);
            g.fillOval(7, 9, 3, 2);

            g.dispose();
            saveFrame(img, "cat_sleep", f);
        }
    }

    // ======================== DOG ========================
    private static void generateDog() {
        Color body = new Color(180, 130, 70);
        Color belly = new Color(220, 180, 120);
        Color earDark = new Color(120, 75, 35);
        Color nose = new Color(30, 30, 30);
        Color tongue = new Color(240, 100, 100);
        Color paw = new Color(150, 110, 55);

        for (int f = 0; f < FRAMES; f++) {
            BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            double phase = f * 2 * Math.PI / FRAMES;

            // Tail (behind)
            g.setColor(earDark);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int tailWag = (int)(Math.sin(phase * 2) * 20);
            g.rotate(Math.toRadians(tailWag), 1, 6);
            g.drawLine(1, 6, -1, 3);
            g.rotate(-Math.toRadians(tailWag), 1, 6);

            // Body
            g.setColor(body);
            g.fillOval(2, 5, 9, 6);
            g.setColor(belly);
            g.fillOval(3, 7, 6, 3);

            // Head
            g.setColor(body);
            g.fillOval(9, 2, 6, 6);

            // Snout
            g.setColor(belly);
            g.fillOval(13, 4, 3, 3);

            // Floppy ears
            g.setColor(earDark);
            g.fillOval(8, 3, 3, 5);
            g.fillOval(12, 4, 3, 4);

            // Eye
            g.setColor(Color.WHITE);
            g.fillOval(11, 3, 2, 2);
            g.setColor(new Color(50, 30, 10));
            g.fillOval(12, 3, 1, 2);

            // Nose
            g.setColor(nose);
            g.fillOval(14, 5, 2, 2);
            // Nose highlight
            g.setColor(new Color(80, 80, 80));
            g.fillOval(14, 5, 1, 1);

            // Tongue (panting)
            if (f % 3 != 0) {
                g.setColor(tongue);
                int tongueLen = 1 + (int)(Math.sin(phase) * 1);
                g.fillOval(15, 7, 1, tongueLen + 1);
            }

            // Legs
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(earDark);
            double fl1 = Math.sin(phase) * 3;
            double fl2 = Math.sin(phase + Math.PI) * 3;
            int fl1x = 3 + (int)fl1, fl1y = 11 + (int)Math.abs(fl1 * 0.3);
            int fl2x = 5 + (int)fl2, fl2y = 11 + (int)Math.abs(fl2 * 0.3);
            g.drawLine(3, 10, fl1x, fl1y);
            g.drawLine(5, 10, fl2x, fl2y);
            double hl1 = Math.sin(phase + Math.PI * 0.5) * 3;
            double hl2 = Math.sin(phase + Math.PI * 1.5) * 3;
            int hl1x = 8 + (int)hl1, hl1y = 11 + (int)Math.abs(hl1 * 0.3);
            int hl2x = 9 + (int)hl2, hl2y = 11 + (int)Math.abs(hl2 * 0.3);
            g.drawLine(8, 10, hl1x, hl1y);
            g.drawLine(9, 10, hl2x, hl2y);

            // Paws
            g.setColor(paw);
            g.fillOval(fl1x - 1, fl1y - 1, 3, 2);
            g.fillOval(fl2x - 1, fl2y - 1, 3, 2);

            g.dispose();
            saveFrame(img, "dog", f);
        }
    }

    // ======================== HORSE ========================
    private static void generateHorse() {
        Color body = new Color(160, 110, 60);
        Color belly = new Color(200, 160, 100);
        Color mane = new Color(50, 30, 15);
        Color hoof = new Color(60, 40, 20);
        Color nose = new Color(120, 80, 50);

        for (int f = 0; f < FRAMES; f++) {
            BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            double phase = f * 2 * Math.PI / FRAMES;

            // Tail
            g.setColor(mane);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int tailSwing = (int)(Math.sin(phase) * 2);
            g.drawArc(-3, 4 + tailSwing, 5, 5, 70, 100);

            // Body
            g.setColor(body);
            g.fillOval(1, 4, 11, 7);
            g.setColor(belly);
            g.fillOval(2, 6, 7, 3);

            // Neck
            g.setColor(body);
            g.fillPolygon(new int[]{10, 12, 13, 11}, new int[]{4, 0, 1, 5}, 4);

            // Head
            g.setColor(body);
            g.fillOval(10, -1, 6, 5);
            g.setColor(belly);
            g.fillOval(11, 0, 4, 2);

            // Snout
            g.setColor(nose);
            g.fillOval(14, 1, 2, 2);

            // Mane
            g.setColor(mane);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int m = 0; m < 3; m++) {
                int maneY = 1 + m * 2;
                int maneWave = (int)(Math.sin(phase + m * 0.5) * 1);
                g.drawLine(10 + m, maneY, 9 + maneWave, maneY + 1);
            }

            // Eye
            g.setColor(Color.WHITE);
            g.fillOval(12, 0, 2, 2);
            g.setColor(new Color(40, 20, 5));
            g.fillOval(12, 0, 1, 2);

            // Nostril
            g.setColor(new Color(40, 20, 5));
            g.fillOval(15, 2, 1, 1);

            // Legs (galloping)
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(130, 85, 40));
            double[] legOffsets = new double[4];
            for (int i = 0; i < 4; i++) {
                legOffsets[i] = Math.sin(phase + i * Math.PI / 2) * 3;
            }
            int[][] legPositions = {{2, 10}, {4, 10}, {7, 10}, {9, 10}};
            int[] legX = new int[4], legY = new int[4];
            for (int i = 0; i < 4; i++) {
                legX[i] = legPositions[i][0] + (int)legOffsets[i];
                legY[i] = 14;
                g.drawLine(legPositions[i][0], legPositions[i][1], legX[i], legY[i]);
            }

            // Hooves
            g.setColor(hoof);
            for (int i = 0; i < 4; i++) {
                g.fillOval(legX[i] - 1, 13, 2, 2);
            }

            g.dispose();
            saveFrame(img, "horse", f);
        }
    }

    // ======================== PARROT ========================
    private static void generateParrot() {
        Color body = new Color(0, 180, 80);
        Color belly = new Color(80, 220, 120);
        Color wing = new Color(0, 140, 50);
        Color beak = new Color(255, 180, 0);
        Color tailRed = new Color(220, 40, 40);
        Color tailBlue = new Color(30, 80, 220);
        Color tailYellow = new Color(255, 220, 0);

        for (int f = 0; f < FRAMES; f++) {
            BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            double phase = f * 2 * Math.PI / FRAMES;

            // Tail feathers
            g.setColor(tailRed);
            g.fillOval(2, 11, 3, 5);
            g.setColor(tailBlue);
            g.fillOval(4, 11, 3, 4);
            g.setColor(tailYellow);
            g.fillOval(6, 11, 3, 5);

            // Wing (flapping)
            g.setColor(wing);
            int wingFlap = (int)(Math.sin(phase) * 3);
            g.fillOval(1, 5 + wingFlap, 5, 4);
            g.setColor(belly);
            g.fillOval(2, 5 + wingFlap, 3, 2);

            // Body
            g.setColor(body);
            g.fillOval(4, 4, 8, 8);
            g.setColor(belly);
            g.fillOval(5, 6, 5, 4);

            // Head
            g.setColor(body);
            g.fillOval(9, 2, 6, 5);
            g.setColor(belly);
            g.fillOval(10, 3, 4, 2);

            // Eye ring
            g.setColor(Color.WHITE);
            g.fillOval(12, 3, 3, 3);
            // Eye
            g.setColor(Color.BLACK);
            g.fillOval(13, 4, 1, 1);

            // Beak
            g.setColor(beak);
            g.fillPolygon(new int[]{14, 16, 14}, new int[]{4, 5, 6}, 3);
            g.setColor(new Color(200, 140, 0));
            g.drawLine(14, 5, 16, 5);

            // Feet
            g.setColor(new Color(180, 130, 20));
            g.setStroke(new BasicStroke(0.8f));
            g.drawLine(6, 12, 5, 14);
            g.drawLine(5, 14, 4, 14);
            g.drawLine(5, 14, 6, 14);
            g.drawLine(9, 12, 8, 14);
            g.drawLine(8, 14, 7, 14);
            g.drawLine(8, 14, 9, 14);

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
        } catch (IOException e) {
            System.err.println("Failed to save " + path + ": " + e.getMessage());
        }
    }
}
