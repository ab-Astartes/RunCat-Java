package com.runcat.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Generates high-quality animation frame PNG files for taskbar tray icon.
 * Each animation has 8 frames for smooth motion.
 * 
 * Design philosophy:
 * - 16x16 is tiny - use bold shapes and high contrast
 * - Avoid fine details that blur into noise
 * - Each frame should be recognizable standalone
 * - Motion should read clearly: running, sleeping, flying, galloping
 * 
 * Animations: cat (running), cat_sleep, dog, horse, parrot, rabbit, penguin
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
        generateRabbit();
        generatePenguin();
        System.out.println("All animations generated!");
    }

    // ======================== CAT (RUNNING) ========================
    // Classic orange tabby - bold silhouette with clear motion
    private static void generateRunningCat() {
        // Colors - bright orange that pops on both light and dark taskbars
        Color body = new Color(255, 140, 40);
        Color bodyShade = new Color(200, 95, 20);
        Color belly = new Color(255, 200, 130);
        Color earInner = new Color(255, 160, 160);
        Color eye = new Color(40, 180, 255);
        Color nose = new Color(255, 120, 140);

        for (int f = 0; f < FRAMES; f++) {
            BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            enableAntialiasing(g);

            double phase = f * 2 * Math.PI / FRAMES;
            int legPhase = (int)(Math.sin(phase) * 3);
            int legPhase2 = (int)(Math.sin(phase + Math.PI) * 3);

            // ---- TAIL (behind body) ----
            g.setColor(bodyShade);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int tailWave = (int)(Math.sin(phase * 2) * 2);
            // Tail curves up and waves
            g.drawArc(-2, 1 + tailWave, 7, 6, 60, 100);

            // ---- BODY ----
            g.setColor(body);
            g.fillOval(2, 5, 10, 7);
            // Belly highlight
            g.setColor(belly);
            g.fillOval(3, 7, 7, 4);

            // ---- HEAD ----
            g.setColor(body);
            g.fillOval(9, 1, 6, 7);
            // Forehead
            g.setColor(bodyShade);
            g.fillOval(10, 2, 4, 3);

            // ---- EARS ----
            g.setColor(body);
            // Left ear
            g.fillPolygon(new int[]{10, 11, 13}, new int[]{1, -1, 1}, 3);
            // Right ear
            g.fillPolygon(new int[]{13, 14, 15}, new int[]{1, -1, 1}, 3);
            // Inner ears
            g.setColor(earInner);
            g.fillPolygon(new int[]{11, 11, 12}, new int[]{1, 0, 1}, 3);
            g.fillPolygon(new int[]{14, 14, 15}, new int[]{1, 0, 1}, 3);

            // ---- FACE ----
            // Blink on frames 3 and 7
            boolean blink = (f == 3 || f == 7);
            if (blink) {
                g.setColor(bodyShade);
                g.setStroke(new BasicStroke(1f));
                g.drawLine(11, 4, 14, 4);
            } else {
                // Eyes - big and expressive
                g.setColor(Color.WHITE);
                g.fillOval(11, 3, 4, 3);
                g.setColor(eye);
                g.fillOval(12, 3, 2, 3);
                g.setColor(new Color(20, 40, 60));
                g.fillOval(12, 4, 1, 2);
                // Eye shine
                g.setColor(Color.WHITE);
                g.fillOval(12, 3, 1, 1);
            }

            // Nose
            g.setColor(nose);
            g.fillPolygon(new int[]{14, 15, 14}, new int[]{5, 6, 6}, 3);

            // Whiskers - subtle
            g.setColor(new Color(200, 150, 100));
            g.setStroke(new BasicStroke(0.5f));
            g.drawLine(14, 6, 16, 5);
            g.drawLine(14, 6, 16, 7);

            // ---- LEGS ----
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            
            // Front legs - alternating stride
            int fl1y = 11 + Math.abs(legPhase);
            int fl2y = 11 + Math.abs(legPhase2);
            g.setColor(bodyShade);
            g.drawLine(4, 10, 4 + legPhase, fl1y);
            g.drawLine(6, 10, 6 + legPhase2, fl2y);

            // Hind legs
            int hl1 = (int)(Math.sin(phase + Math.PI * 0.5) * 3);
            int hl2 = (int)(Math.sin(phase + Math.PI * 1.5) * 3);
            g.drawLine(8, 10, 8 + hl1, 11 + Math.abs(hl1));
            g.drawLine(9, 10, 9 + hl2, 11 + Math.abs(hl2));

            // Paws - bright spots
            g.setColor(belly);
            g.fillOval(3 + legPhase, fl1y - 1, 3, 2);
            g.fillOval(5 + legPhase2, fl2y - 1, 3, 2);

            g.dispose();
            saveFrame(img, "cat", f);
        }
    }

    // ======================== SLEEPING CAT ========================
    // Curled up ball with gentle breathing and Zzz
    private static void generateSleepingCat() {
        Color body = new Color(255, 140, 40);
        Color bodyShade = new Color(200, 95, 20);
        Color belly = new Color(255, 200, 130);
        Color earInner = new Color(255, 160, 160);

        for (int f = 0; f < FRAMES; f++) {
            BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            enableAntialiasing(g);

            double phase = f * 2 * Math.PI / FRAMES;
            int breathY = (int)(Math.sin(phase) * 0.5);

            // Curled body
            g.setColor(body);
            g.fillOval(1, 4 + breathY, 13, 9);
            
            // Head tucked in
            g.fillOval(1, 2 + breathY, 8, 7);
            
            // Belly patch
            g.setColor(belly);
            g.fillOval(3, 6 + breathY, 9, 5);
            g.fillOval(2, 4 + breathY, 5, 4);

            // Ears - one visible, one tucked
            g.setColor(body);
            g.fillPolygon(new int[]{2, 3, 5}, new int[]{2 + breathY, 0 + breathY, 2 + breathY}, 3);
            g.fillPolygon(new int[]{5, 6, 7}, new int[]{2 + breathY, 0 + breathY, 2 + breathY}, 3);
            g.setColor(earInner);
            g.fillPolygon(new int[]{3, 3, 4}, new int[]{2 + breathY, 1 + breathY, 2 + breathY}, 3);
            g.fillPolygon(new int[]{6, 6, 7}, new int[]{2 + breathY, 1 + breathY, 2 + breathY}, 3);

            // Closed eyes - happy curved lines
            g.setColor(bodyShade);
            g.setStroke(new BasicStroke(0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(3, 5 + breathY, 2, 1, 0, 180);
            g.drawArc(6, 5 + breathY, 2, 1, 0, 180);

            // Zzz animation
            int zAlpha = 180 + (int)(Math.sin(phase * 2) * 60);
            g.setColor(new Color(80, 160, 255, zAlpha));
            g.setFont(g.getFont().deriveFont(Font.BOLD, 5f));
            int zzzFrame = f % 4;
            if (zzzFrame >= 1) g.drawString("z", 10, 3);
            if (zzzFrame >= 2) { g.setFont(g.getFont().deriveFont(Font.BOLD, 6f)); g.drawString("Z", 12, 1); }
            if (zzzFrame >= 3) { g.setFont(g.getFont().deriveFont(Font.BOLD, 7f)); g.drawString("Z", 14, -1); }

            // Tail wrapped around
            g.setColor(bodyShade);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int tailY = 8 + breathY + (int)(Math.sin(phase) * 0.5);
            g.drawArc(10, tailY, 5, 4, 90, 120);

            // Paws visible
            g.setColor(belly);
            g.fillOval(3, 10 + breathY, 3, 2);
            g.fillOval(7, 10 + breathY, 3, 2);

            g.dispose();
            saveFrame(img, "cat_sleep", f);
        }
    }

    // ======================== DOG ========================
    // Golden retriever style - floppy ears, wagging tail
    private static void generateDog() {
        Color body = new Color(200, 150, 80);
        Color bodyLight = new Color(240, 200, 130);
        Color earDark = new Color(140, 90, 50);
        Color nose = new Color(30, 30, 30);
        Color tongue = new Color(255, 100, 110);

        for (int f = 0; f < FRAMES; f++) {
            BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            enableAntialiasing(g);

            double phase = f * 2 * Math.PI / FRAMES;

            // ---- TAIL (wagging excitedly) ----
            g.setColor(earDark);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int wagAngle = (int)(Math.sin(phase * 3) * 25);
            g.rotate(Math.toRadians(wagAngle), 2, 6);
            g.drawLine(2, 6, -1, 3);
            g.rotate(-Math.toRadians(wagAngle), 2, 6);

            // ---- BODY ----
            g.setColor(body);
            g.fillOval(2, 5, 10, 7);
            g.setColor(bodyLight);
            g.fillOval(3, 7, 7, 4);

            // ---- HEAD ----
            g.setColor(body);
            g.fillOval(9, 1, 6, 7);

            // ---- SNOUT ----
            g.setColor(bodyLight);
            g.fillOval(12, 3, 4, 4);

            // ---- FLOPPY EARS ----
            g.setColor(earDark);
            g.fillOval(8, 2, 4, 6);  // Left ear hanging
            g.fillOval(12, 3, 4, 5); // Right ear

            // ---- EYE ----
            g.setColor(Color.WHITE);
            g.fillOval(11, 2, 3, 3);
            g.setColor(new Color(60, 40, 20));
            g.fillOval(12, 2, 2, 3);
            // Shine
            g.setColor(Color.WHITE);
            g.fillOval(12, 2, 1, 1);

            // ---- NOSE ----
            g.setColor(nose);
            g.fillOval(14, 4, 2, 2);
            // Nose shine
            g.setColor(new Color(70, 70, 70));
            g.fillOval(14, 4, 1, 1);

            // ---- TONGUE (panting) ----
            if (f % 2 == 0) {
                g.setColor(tongue);
                int tongueLen = 1 + (f % 3);
                g.fillOval(15, 7, 1, tongueLen + 1);
            }

            // ---- LEGS ----
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(earDark);
            
            int leg1 = (int)(Math.sin(phase) * 3);
            int leg2 = (int)(Math.sin(phase + Math.PI) * 3);
            int leg3 = (int)(Math.sin(phase + Math.PI * 0.5) * 3);
            int leg4 = (int)(Math.sin(phase + Math.PI * 1.5) * 3);

            g.drawLine(3, 11, 3 + leg1, 12 + Math.abs(leg1) / 3);
            g.drawLine(5, 11, 5 + leg2, 12 + Math.abs(leg2) / 3);
            g.drawLine(8, 11, 8 + leg3, 12 + Math.abs(leg3) / 3);
            g.drawLine(10, 11, 10 + leg4, 12 + Math.abs(leg4) / 3);

            // Paws
            g.setColor(bodyLight);
            g.fillOval(2 + leg1, 12, 3, 2);
            g.fillOval(4 + leg2, 12, 3, 2);

            g.dispose();
            saveFrame(img, "dog", f);
        }
    }

    // ======================== HORSE ========================
    // Galloping horse - powerful stride
    private static void generateHorse() {
        Color body = new Color(170, 120, 70);
        Color bodyLight = new Color(210, 170, 120);
        Color mane = new Color(60, 40, 25);
        Color hoof = new Color(50, 35, 20);

        for (int f = 0; f < FRAMES; f++) {
            BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            enableAntialiasing(g);

            double phase = f * 2 * Math.PI / FRAMES;

            // ---- TAIL ----
            g.setColor(mane);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int tailSwing = (int)(Math.sin(phase) * 2);
            g.drawArc(-3, 3 + tailSwing, 5, 5, 70, 110);

            // ---- BODY ----
            g.setColor(body);
            g.fillOval(1, 4, 12, 8);
            g.setColor(bodyLight);
            g.fillOval(2, 6, 8, 4);

            // ---- NECK ----
            g.setColor(body);
            g.fillPolygon(new int[]{10, 12, 13, 11}, new int[]{4, 0, 1, 5}, 4);

            // ---- HEAD ----
            g.setColor(body);
            g.fillOval(10, -1, 6, 5);
            g.setColor(bodyLight);
            g.fillOval(11, 0, 4, 2);
            // Snout
            g.setColor(new Color(150, 100, 60));
            g.fillOval(14, 1, 2, 2);

            // ---- MANE (flowing) ----
            g.setColor(mane);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int m = 0; m < 3; m++) {
                int maneWave = (int)(Math.sin(phase + m * 0.5) * 2);
                g.drawLine(10 + m, 1 + m * 2, 8 + maneWave, 2 + m * 2);
            }

            // ---- EYE ----
            g.setColor(Color.WHITE);
            g.fillOval(12, 0, 2, 2);
            g.setColor(new Color(40, 25, 10));
            g.fillOval(12, 0, 1, 2);

            // ---- NOSTRIL ----
            g.setColor(new Color(40, 25, 10));
            g.fillOval(15, 2, 1, 1);

            // ---- LEGS (galloping - extended stride) ----
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(140, 95, 55));

            // Front legs - extended forward
            int fl1 = (int)(Math.sin(phase) * 4);
            int fl2 = (int)(Math.sin(phase + Math.PI) * 4);
            // Hind legs - extended backward (opposite phase)
            int hl1 = (int)(Math.sin(phase + Math.PI) * 4);
            int hl2 = (int)(Math.sin(phase) * 4);

            // Draw legs with bounce
            int bounce = (int)(Math.abs(Math.sin(phase * 2)) * 2);

            g.drawLine(3, 10, 3 + fl1, 13 - bounce);
            g.drawLine(5, 10, 5 + fl2, 13 - bounce);
            g.drawLine(8, 10, 8 + hl1, 13 - bounce);
            g.drawLine(10, 10, 10 + hl2, 13 - bounce);

            // Hooves
            g.setColor(hoof);
            g.fillOval(2 + fl1, 12 - bounce, 2, 2);
            g.fillOval(4 + fl2, 12 - bounce, 2, 2);
            g.fillOval(7 + hl1, 12 - bounce, 2, 2);
            g.fillOval(9 + hl2, 12 - bounce, 2, 2);

            g.dispose();
            saveFrame(img, "horse", f);
        }
    }

    // ======================== PARROT ========================
    // Colorful macaw - flapping wings
    private static void generateParrot() {
        Color body = new Color(30, 180, 90);
        Color belly = new Color(100, 230, 140);
        Color wing = new Color(20, 140, 60);
        Color head = new Color(50, 200, 100);
        Color beak = new Color(255, 180, 50);
        Color tailRed = new Color(230, 60, 60);
        Color tailBlue = new Color(40, 100, 230);
        Color tailYellow = new Color(255, 220, 50);

        for (int f = 0; f < FRAMES; f++) {
            BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            enableAntialiasing(g);

            double phase = f * 2 * Math.PI / FRAMES;

            // ---- TAIL FEATHERS (behind) ----
            g.setColor(tailRed);
            g.fillOval(1, 11, 4, 5);
            g.setColor(tailBlue);
            g.fillOval(3, 11, 4, 4);
            g.setColor(tailYellow);
            g.fillOval(5, 11, 4, 5);

            // ---- WING (flapping) ----
            g.setColor(wing);
            int wingFlap = (int)(Math.sin(phase) * 4);
            g.fillOval(0, 4 + wingFlap, 6, 5);
            g.setColor(belly);
            g.fillOval(1, 5 + wingFlap, 3, 2);

            // ---- BODY ----
            g.setColor(body);
            g.fillOval(4, 4, 9, 9);
            g.setColor(belly);
            g.fillOval(5, 6, 6, 5);

            // ---- HEAD ----
            g.setColor(head);
            g.fillOval(10, 1, 6, 6);
            g.setColor(belly);
            g.fillOval(11, 2, 4, 3);

            // ---- EYE ----
            g.setColor(Color.WHITE);
            g.fillOval(13, 2, 3, 3);
            g.setColor(Color.BLACK);
            g.fillOval(14, 3, 1, 1);
            // Eye ring
            g.setColor(new Color(200, 200, 200));
            g.setStroke(new BasicStroke(0.5f));
            g.drawOval(13, 2, 3, 3);

            // ---- BEAK ----
            g.setColor(beak);
            g.fillPolygon(new int[]{15, 17, 15}, new int[]{4, 5, 7}, 3);
            g.setColor(new Color(200, 140, 30));
            g.setStroke(new BasicStroke(0.8f));
            g.drawLine(15, 5, 17, 5);

            // ---- FEET ----
            g.setColor(new Color(160, 120, 40));
            g.setStroke(new BasicStroke(0.8f));
            // Left foot
            g.drawLine(6, 13, 5, 15);
            g.drawLine(5, 15, 4, 15);
            g.drawLine(5, 15, 6, 15);
            // Right foot
            g.drawLine(10, 13, 9, 15);
            g.drawLine(9, 15, 8, 15);
            g.drawLine(9, 15, 10, 15);

            g.dispose();
            saveFrame(img, "parrot", f);
        }
    }

    // ======================== RABBIT ========================
    // Hopping bunny - big ears, fluffy tail
    private static void generateRabbit() {
        Color body = new Color(240, 240, 245);
        Color bodyShade = new Color(200, 200, 210);
        Color earInner = new Color(255, 180, 180);
        Color nose = new Color(255, 150, 160);
        Color eye = new Color(60, 40, 30);

        for (int f = 0; f < FRAMES; f++) {
            BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            enableAntialiasing(g);

            double phase = f * 2 * Math.PI / FRAMES;
            int hopBounce = (int)(Math.abs(Math.sin(phase * 2)) * 2);

            // ---- EARS (long and upright) ----
            int earWiggle = (int)(Math.sin(phase) * 1);
            g.setColor(body);
            // Left ear
            g.fillOval(2 + earWiggle, -2, 3, 7);
            // Right ear
            g.fillOval(5 - earWiggle, -2, 3, 7);
            // Inner ears
            g.setColor(earInner);
            g.fillOval(3 + earWiggle, 0, 1, 4);
            g.fillOval(6 - earWiggle, 0, 1, 4);

            // ---- FLUFFY TAIL (behind) ----
            g.setColor(Color.WHITE);
            g.fillOval(-1, 6 - hopBounce, 4, 4);

            // ---- BODY ----
            g.setColor(body);
            g.fillOval(1, 4 - hopBounce, 10, 9);
            g.setColor(bodyShade);
            g.fillOval(2, 6 - hopBounce, 7, 5);

            // ---- HEAD ----
            g.setColor(body);
            g.fillOval(7, 2 - hopBounce, 8, 8);
            g.setColor(bodyShade);
            g.fillOval(8, 4 - hopBounce, 5, 4);

            // ---- EYES ----
            g.setColor(Color.WHITE);
            g.fillOval(10, 4 - hopBounce, 3, 3);
            g.setColor(eye);
            g.fillOval(11, 4 - hopBounce, 2, 3);
            // Shine
            g.setColor(Color.WHITE);
            g.fillOval(11, 4 - hopBounce, 1, 1);

            // ---- NOSE (twitching) ----
            int noseTwitch = (f % 2 == 0) ? 0 : 1;
            g.setColor(nose);
            g.fillOval(13 + noseTwitch, 6 - hopBounce, 2, 2);

            // ---- WHISKERS ----
            g.setColor(new Color(180, 180, 190));
            g.setStroke(new BasicStroke(0.5f));
            g.drawLine(14, 7 - hopBounce, 16, 6 - hopBounce);
            g.drawLine(14, 7 - hopBounce, 16, 8 - hopBounce);

            // ---- LEGS (hopping) ----
            g.setColor(bodyShade);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            
            // Front paws
            int legExt = (int)(Math.sin(phase) * 2);
            g.drawLine(3, 12 - hopBounce, 3 + legExt, 14 - hopBounce);
            g.drawLine(5, 12 - hopBounce, 5 - legExt, 14 - hopBounce);
            // Hind legs (more extended when hopping)
            int hindExt = (int)(Math.sin(phase + Math.PI) * 3);
            g.drawLine(8, 12 - hopBounce, 8 + hindExt, 14 - hopBounce);

            g.dispose();
            saveFrame(img, "rabbit", f);
        }
    }

    // ======================== PENGUIN ========================
    // Waddling penguin - tuxedo colors
    private static void generatePenguin() {
        Color black = new Color(30, 35, 40);
        Color white = new Color(250, 250, 252);
        Color belly = new Color(255, 255, 240);
        Color orange = new Color(255, 160, 50);
        Color eye = new Color(40, 40, 45);

        for (int f = 0; f < FRAMES; f++) {
            BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            enableAntialiasing(g);

            double phase = f * 2 * Math.PI / FRAMES;
            int waddle = (int)(Math.sin(phase * 2) * 1);

            // ---- FLIPPERS (wings) ----
            g.setColor(black);
            // Left flipper
            g.fillOval(0 + waddle, 4, 4, 8);
            // Right flipper
            g.fillOval(12 - waddle, 4, 4, 8);

            // ---- BODY (black back) ----
            g.setColor(black);
            g.fillOval(2, 0, 12, 16);

            // ---- WHITE BELLY ----
            g.setColor(white);
            g.fillOval(4, 2, 8, 12);
            g.setColor(belly);
            g.fillOval(5, 3, 6, 10);

            // ---- HEAD ----
            g.setColor(black);
            g.fillOval(3, 0, 10, 7);

            // ---- WHITE FACE PATCH ----
            g.setColor(white);
            g.fillOval(4, 1, 8, 5);

            // ---- EYES ----
            g.setColor(eye);
            g.fillOval(5, 2, 2, 2);
            g.fillOval(9, 2, 2, 2);
            // Eye shine
            g.setColor(Color.WHITE);
            g.fillOval(5, 2, 1, 1);
            g.fillOval(9, 2, 1, 1);

            // ---- BEAK ----
            g.setColor(orange);
            int beakOffset = (f % 2 == 0) ? 0 : 1;
            g.fillPolygon(new int[]{7, 8, 9}, new int[]{5, 7 + beakOffset, 5}, 3);

            // ---- FEET ----
            g.setColor(orange);
            g.fillOval(4 + waddle, 14, 3, 2);
            g.fillOval(9 - waddle, 14, 3, 2);

            g.dispose();
            saveFrame(img, "penguin", f);
        }
    }

    // ======================== HELPERS ========================
    
    private static void enableAntialiasing(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
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
