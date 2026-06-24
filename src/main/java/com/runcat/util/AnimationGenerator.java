package com.runcat.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Generates high-quality animation frame PNG files.
 * Supports arbitrary sizes via scale factor �?all coordinates are proportional to SIZE.
 *
 * Usage: run with no args to generate 16x16 taskbar icons.
 *        Call generateAllAtSize(size) programmatically for other sizes.
 *
 * Animations: cat, cat_sleep, dog, horse, parrot, rabbit, penguin
 */
public class AnimationGenerator {

    private static final int BASE = 16;  // all coords are designed around 16x16
    private static final int FRAMES = 8;

    public static void main(String[] args) {
        // Generate standard 16x16 taskbar icons
        int size = 16;
        if (args.length > 0) {
            try { size = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }
        generateAllAtSize(size, "src/main/resources/animations");
        // Always also generate 64x64 HD for desktop pet
        generateAllAtSize(64, "src/main/resources/animations_hd");
        System.out.println("All animations generated!");
    }

    /**
     * Generate all animations at a given pixel size.
     */
    public static void generateAllAtSize(int size, String resDir) {
        float scale = size / (float) BASE;

        new CatGenerator(scale, size, resDir).generate();
        new SleepingCatGenerator(scale, size, resDir).generate();
        new DogGenerator(scale, size, resDir).generate();
        new HorseGenerator(scale, size, resDir).generate();
        new ParrotGenerator(scale, size, resDir).generate();
        new RabbitGenerator(scale, size, resDir).generate();
        new PenguinGenerator(scale, size, resDir).generate();
    }

    // Base class for individual animation generators
    private static abstract class AnimGen {
        protected final float scale;
        protected final int size;
        protected final String resDir;

        AnimGen(float scale, int size, String resDir) {
            this.scale = scale;
            this.size = size;
            this.resDir = resDir;
        }

        protected void enableAntialiasing(Graphics2D g) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        }

        protected void saveFrame(BufferedImage img, String animal, int frame) {
            String dir = resDir + "/" + animal;
            new File(dir).mkdirs();
            String path = dir + "/" + animal + "_" + frame + ".png";
            try {
                ImageIO.write(img, "png", new File(path));
            } catch (IOException e) {
                System.err.println("Failed to save " + path + ": " + e.getMessage());
            }
        }

        protected int sc(int v) { return Math.round(v * scale); }
        protected float scf(float v) { return v * scale; }

        protected BufferedImage newImage() {
            return new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        }

        protected abstract void generateFrame(Graphics2D g, int f);
        protected abstract String animalName();

        public void generate() {
            for (int f = 0; f < FRAMES; f++) {
                BufferedImage img = newImage();
                Graphics2D g = img.createGraphics();
                enableAntialiasing(g);
                generateFrame(g, f);
                g.dispose();
                saveFrame(img, animalName(), f);
            }
        }
    }

    // ======================== CAT ========================

    private static class CatGenerator extends AnimGen {
        CatGenerator(float scale, int size, String resDir) { super(scale, size, resDir); }
        protected String animalName() { return "cat"; }

        protected void generateFrame(Graphics2D g, int f) {
            double phase = f * 2 * Math.PI / FRAMES;

            // Tail
            g.setColor(new Color(200, 95, 20));
            g.setStroke(new BasicStroke(scf(2f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int tailWave = Math.round(scf(2) * (float) Math.sin(phase * 2));
            g.drawArc(sc(-2), sc(1) + tailWave, sc(7), sc(6), 60, 100);

            // Body
            g.setColor(new Color(255, 140, 40));
            g.fillOval(sc(2), sc(5), sc(10), sc(7));
            g.setColor(new Color(255, 200, 130));
            g.fillOval(sc(3), sc(7), sc(7), sc(4));

            // Head
            g.setColor(new Color(255, 140, 40));
            g.fillOval(sc(9), sc(1), sc(6), sc(7));
            g.setColor(new Color(200, 95, 20));
            g.fillOval(sc(10), sc(2), sc(4), sc(3));

            // Ears
            g.setColor(new Color(255, 140, 40));
            g.fillPolygon(new int[]{sc(10), sc(11), sc(13)}, new int[]{sc(1), sc(-1), sc(1)}, 3);
            g.fillPolygon(new int[]{sc(13), sc(14), sc(15)}, new int[]{sc(1), sc(-1), sc(1)}, 3);
            g.setColor(new Color(255, 160, 160));
            g.fillPolygon(new int[]{sc(11), sc(11), sc(12)}, new int[]{sc(1), sc(0), sc(1)}, 3);
            g.fillPolygon(new int[]{sc(14), sc(14), sc(15)}, new int[]{sc(1), sc(0), sc(1)}, 3);

            // Eyes
            boolean blink = (f == 3 || f == 7);
            if (blink) {
                g.setColor(new Color(200, 95, 20));
                g.setStroke(new BasicStroke(scf(1f)));
                g.drawLine(sc(11), sc(4), sc(14), sc(4));
            } else {
                g.setColor(Color.WHITE);
                g.fillOval(sc(11), sc(3), sc(4), sc(3));
                g.setColor(new Color(40, 180, 255));
                g.fillOval(sc(12), sc(3), sc(2), sc(3));
                g.setColor(new Color(20, 40, 60));
                g.fillOval(sc(12), sc(4), sc(1), sc(2));
                g.setColor(Color.WHITE);
                g.fillOval(sc(12), sc(3), sc(1), sc(1));
            }

            // Nose
            g.setColor(new Color(255, 120, 140));
            g.fillPolygon(new int[]{sc(14), sc(15), sc(14)}, new int[]{sc(5), sc(6), sc(6)}, 3);

            // Whiskers
            g.setColor(new Color(200, 150, 100));
            g.setStroke(new BasicStroke(scf(0.5f)));
            g.drawLine(sc(14), sc(6), sc(16), sc(5));
            g.drawLine(sc(14), sc(6), sc(16), sc(7));

            // Legs
            int legPhase = Math.round(scf(3) * (float) Math.sin(phase));
            int legPhase2 = Math.round(scf(3) * (float) Math.sin(phase + Math.PI));
            g.setStroke(new BasicStroke(scf(2f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(200, 95, 20));

            int fl1y = sc(11) + Math.abs(legPhase);
            int fl2y = sc(11) + Math.abs(legPhase2);
            g.drawLine(sc(4), sc(10), sc(4) + legPhase, fl1y);
            g.drawLine(sc(6), sc(10), sc(6) + legPhase2, fl2y);

            int hl1 = Math.round(scf(3) * (float) Math.sin(phase + Math.PI * 0.5));
            int hl2 = Math.round(scf(3) * (float) Math.sin(phase + Math.PI * 1.5));
            g.drawLine(sc(8), sc(10), sc(8) + hl1, sc(11) + Math.abs(hl1));
            g.drawLine(sc(9), sc(10), sc(9) + hl2, sc(11) + Math.abs(hl2));

            g.setColor(new Color(255, 200, 130));
            g.fillOval(sc(3) + legPhase, fl1y - sc(1), sc(3), sc(2));
            g.fillOval(sc(5) + legPhase2, fl2y - sc(1), sc(3), sc(2));
        }
    }

    // ======================== SLEEPING CAT ========================

    private static class SleepingCatGenerator extends AnimGen {
        SleepingCatGenerator(float scale, int size, String resDir) { super(scale, size, resDir); }
        protected String animalName() { return "cat_sleep"; }

        protected void generateFrame(Graphics2D g, int f) {
            double phase = f * 2 * Math.PI / FRAMES;
            int breathY = Math.round(scf(0.5f) * (float) Math.sin(phase));

            // Curled body
            g.setColor(new Color(255, 140, 40));
            g.fillOval(sc(1), sc(4) + breathY, sc(13), sc(9));
            g.fillOval(sc(1), sc(2) + breathY, sc(8), sc(7));
            g.setColor(new Color(255, 200, 130));
            g.fillOval(sc(3), sc(6) + breathY, sc(9), sc(5));
            g.fillOval(sc(2), sc(4) + breathY, sc(5), sc(4));

            // Ears
            g.setColor(new Color(255, 140, 40));
            g.fillPolygon(new int[]{sc(2), sc(3), sc(5)}, new int[]{sc(2) + breathY, sc(0) + breathY, sc(2) + breathY}, 3);
            g.fillPolygon(new int[]{sc(5), sc(6), sc(7)}, new int[]{sc(2) + breathY, sc(0) + breathY, sc(2) + breathY}, 3);
            g.setColor(new Color(255, 160, 160));
            g.fillPolygon(new int[]{sc(3), sc(3), sc(4)}, new int[]{sc(2) + breathY, sc(1) + breathY, sc(2) + breathY}, 3);
            g.fillPolygon(new int[]{sc(6), sc(6), sc(7)}, new int[]{sc(2) + breathY, sc(1) + breathY, sc(2) + breathY}, 3);

            // Closed eyes
            g.setColor(new Color(200, 95, 20));
            g.setStroke(new BasicStroke(scf(0.8f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(sc(3), sc(5) + breathY, sc(2), sc(1), 0, 180);
            g.drawArc(sc(6), sc(5) + breathY, sc(2), sc(1), 0, 180);

            // Zzz
            int zAlpha = 180 + (int)(Math.sin(phase * 2) * 60);
            g.setColor(new Color(80, 160, 255, zAlpha));
            float baseFontSize = Math.max(5, 5 * scale);
            g.setFont(g.getFont().deriveFont(Font.BOLD, baseFontSize));
            int zzzFrame = f % 4;
            if (zzzFrame >= 1) g.drawString("z", sc(10), sc(3));
            if (zzzFrame >= 2) { g.setFont(g.getFont().deriveFont(Font.BOLD, baseFontSize * 1.2f)); g.drawString("Z", sc(12), sc(1)); }
            if (zzzFrame >= 3) { g.setFont(g.getFont().deriveFont(Font.BOLD, baseFontSize * 1.4f)); g.drawString("Z", sc(14), sc(-1)); }

            // Tail
            g.setColor(new Color(200, 95, 20));
            g.setStroke(new BasicStroke(scf(2f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int tailY = sc(8) + breathY + Math.round(scf(0.5f) * (float) Math.sin(phase));
            g.drawArc(sc(10), tailY, sc(5), sc(4), 90, 120);

            // Paws
            g.setColor(new Color(255, 200, 130));
            g.fillOval(sc(3), sc(10) + breathY, sc(3), sc(2));
            g.fillOval(sc(7), sc(10) + breathY, sc(3), sc(2));
        }
    }

    // ======================== DOG ========================

    private static class DogGenerator extends AnimGen {
        DogGenerator(float scale, int size, String resDir) { super(scale, size, resDir); }
        protected String animalName() { return "dog"; }

        protected void generateFrame(Graphics2D g, int f) {
            double phase = f * 2 * Math.PI / FRAMES;

            // Tail wagging
            g.setColor(new Color(140, 90, 50));
            g.setStroke(new BasicStroke(scf(2f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int wagAngle = Math.round(scf(25) * (float) Math.sin(phase * 3));
            g.rotate(Math.toRadians(wagAngle), sc(2), sc(6));
            g.drawLine(sc(2), sc(6), sc(-1), sc(3));
            g.rotate(-Math.toRadians(wagAngle), sc(2), sc(6));

            // Body
            g.setColor(new Color(200, 150, 80));
            g.fillOval(sc(2), sc(5), sc(10), sc(7));
            g.setColor(new Color(240, 200, 130));
            g.fillOval(sc(3), sc(7), sc(7), sc(4));

            // Head
            g.setColor(new Color(200, 150, 80));
            g.fillOval(sc(9), sc(1), sc(6), sc(7));
            g.setColor(new Color(240, 200, 130));
            g.fillOval(sc(12), sc(3), sc(4), sc(4));

            // Floppy ears
            g.setColor(new Color(140, 90, 50));
            g.fillOval(sc(8), sc(2), sc(4), sc(6));
            g.fillOval(sc(12), sc(3), sc(4), sc(5));

            // Eye
            g.setColor(Color.WHITE);
            g.fillOval(sc(11), sc(2), sc(3), sc(3));
            g.setColor(new Color(60, 40, 20));
            g.fillOval(sc(12), sc(2), sc(2), sc(3));
            g.setColor(Color.WHITE);
            g.fillOval(sc(12), sc(2), sc(1), sc(1));

            // Nose
            g.setColor(new Color(30, 30, 30));
            g.fillOval(sc(14), sc(4), sc(2), sc(2));

            // Tongue
            if (f % 2 == 0) {
                g.setColor(new Color(255, 100, 110));
                int tongueLen = 1 + (f % 3);
                g.fillOval(sc(15), sc(7), sc(1), sc(tongueLen + 1));
            }

            // Legs
            int leg1 = Math.round(scf(3) * (float) Math.sin(phase));
            int leg2 = Math.round(scf(3) * (float) Math.sin(phase + Math.PI));
            int leg3 = Math.round(scf(3) * (float) Math.sin(phase + Math.PI * 0.5));
            int leg4 = Math.round(scf(3) * (float) Math.sin(phase + Math.PI * 1.5));
            g.setStroke(new BasicStroke(scf(2f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(140, 90, 50));
            g.drawLine(sc(3), sc(11), sc(3) + leg1, sc(12) + Math.abs(leg1) / 3);
            g.drawLine(sc(5), sc(11), sc(5) + leg2, sc(12) + Math.abs(leg2) / 3);
            g.drawLine(sc(8), sc(11), sc(8) + leg3, sc(12) + Math.abs(leg3) / 3);
            g.drawLine(sc(10), sc(11), sc(10) + leg4, sc(12) + Math.abs(leg4) / 3);

            g.setColor(new Color(240, 200, 130));
            g.fillOval(sc(2) + leg1, sc(12), sc(3), sc(2));
            g.fillOval(sc(4) + leg2, sc(12), sc(3), sc(2));
        }
    }

    // ======================== HORSE ========================

    private static class HorseGenerator extends AnimGen {
        HorseGenerator(float scale, int size, String resDir) { super(scale, size, resDir); }
        protected String animalName() { return "horse"; }

        protected void generateFrame(Graphics2D g, int f) {
            double phase = f * 2 * Math.PI / FRAMES;

            // Tail
            g.setColor(new Color(60, 40, 25));
            g.setStroke(new BasicStroke(scf(1.5f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int tailSwing = Math.round(scf(2) * (float) Math.sin(phase));
            g.drawArc(sc(-3), sc(3) + tailSwing, sc(5), sc(5), 70, 110);

            // Body
            g.setColor(new Color(170, 120, 70));
            g.fillOval(sc(1), sc(4), sc(12), sc(8));
            g.setColor(new Color(210, 170, 120));
            g.fillOval(sc(2), sc(6), sc(8), sc(4));

            // Neck
            g.setColor(new Color(170, 120, 70));
            g.fillPolygon(new int[]{sc(10), sc(12), sc(13), sc(11)}, new int[]{sc(4), sc(0), sc(1), sc(5)}, 4);

            // Head
            g.fillOval(sc(10), sc(-1), sc(6), sc(5));
            g.setColor(new Color(210, 170, 120));
            g.fillOval(sc(11), sc(0), sc(4), sc(2));
            g.setColor(new Color(150, 100, 60));
            g.fillOval(sc(14), sc(1), sc(2), sc(2));

            // Mane
            g.setColor(new Color(60, 40, 25));
            g.setStroke(new BasicStroke(scf(1.5f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int m = 0; m < 3; m++) {
                int maneWave = Math.round(scf(2) * (float) Math.sin(phase + m * 0.5));
                g.drawLine(sc(10) + m * sc(1), sc(1) + m * sc(2), sc(8) + maneWave, sc(2) + m * sc(2));
            }

            // Eye
            g.setColor(Color.WHITE);
            g.fillOval(sc(12), sc(0), sc(2), sc(2));
            g.setColor(new Color(40, 25, 10));
            g.fillOval(sc(12), sc(0), sc(1), sc(2));

            // Nostril
            g.setColor(new Color(40, 25, 10));
            g.fillOval(sc(15), sc(2), sc(1), sc(1));

            // Legs
            int bounce = Math.round(scf(2) * (float) Math.abs(Math.sin(phase * 2)));
            g.setStroke(new BasicStroke(scf(2f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(140, 95, 55));

            int fl1 = Math.round(scf(4) * (float) Math.sin(phase));
            int fl2 = Math.round(scf(4) * (float) Math.sin(phase + Math.PI));
            int hl1 = Math.round(scf(4) * (float) Math.sin(phase + Math.PI));
            int hl2 = Math.round(scf(4) * (float) Math.sin(phase));

            g.drawLine(sc(3), sc(10), sc(3) + fl1, sc(13) - bounce);
            g.drawLine(sc(5), sc(10), sc(5) + fl2, sc(13) - bounce);
            g.drawLine(sc(8), sc(10), sc(8) + hl1, sc(13) - bounce);
            g.drawLine(sc(10), sc(10), sc(10) + hl2, sc(13) - bounce);

            g.setColor(new Color(50, 35, 20));
            g.fillOval(sc(2) + fl1, sc(12) - bounce, sc(2), sc(2));
            g.fillOval(sc(4) + fl2, sc(12) - bounce, sc(2), sc(2));
            g.fillOval(sc(7) + hl1, sc(12) - bounce, sc(2), sc(2));
            g.fillOval(sc(9) + hl2, sc(12) - bounce, sc(2), sc(2));
        }
    }

    // ======================== PARROT ========================

    private static class ParrotGenerator extends AnimGen {
        ParrotGenerator(float scale, int size, String resDir) { super(scale, size, resDir); }
        protected String animalName() { return "parrot"; }

        protected void generateFrame(Graphics2D g, int f) {
            double phase = f * 2 * Math.PI / FRAMES;
            int wingFlap = Math.round(scf(4) * (float) Math.sin(phase));

            // Tail feathers
            g.setColor(new Color(230, 60, 60));
            g.fillOval(sc(1), sc(11), sc(4), sc(5));
            g.setColor(new Color(40, 100, 230));
            g.fillOval(sc(3), sc(11), sc(4), sc(4));
            g.setColor(new Color(255, 220, 50));
            g.fillOval(sc(5), sc(11), sc(4), sc(5));

            // Wing
            g.setColor(new Color(20, 140, 60));
            g.fillOval(sc(0), sc(4) + wingFlap, sc(6), sc(5));
            g.setColor(new Color(100, 230, 140));
            g.fillOval(sc(1), sc(5) + wingFlap, sc(3), sc(2));

            // Body
            g.setColor(new Color(30, 180, 90));
            g.fillOval(sc(4), sc(4), sc(9), sc(9));
            g.setColor(new Color(100, 230, 140));
            g.fillOval(sc(5), sc(6), sc(6), sc(5));

            // Head
            g.setColor(new Color(50, 200, 100));
            g.fillOval(sc(10), sc(1), sc(6), sc(6));
            g.setColor(new Color(100, 230, 140));
            g.fillOval(sc(11), sc(2), sc(4), sc(3));

            // Eye
            g.setColor(Color.WHITE);
            g.fillOval(sc(13), sc(2), sc(3), sc(3));
            g.setColor(Color.BLACK);
            g.fillOval(sc(14), sc(3), sc(1), sc(1));
            g.setColor(new Color(200, 200, 200));
            g.setStroke(new BasicStroke(scf(0.5f)));
            g.drawOval(sc(13), sc(2), sc(3), sc(3));

            // Beak
            g.setColor(new Color(255, 180, 50));
            g.fillPolygon(new int[]{sc(15), sc(17), sc(15)}, new int[]{sc(4), sc(5), sc(7)}, 3);
            g.setColor(new Color(200, 140, 30));
            g.setStroke(new BasicStroke(scf(0.8f)));
            g.drawLine(sc(15), sc(5), sc(17), sc(5));

            // Feet
            g.setColor(new Color(160, 120, 40));
            g.setStroke(new BasicStroke(scf(0.8f)));
            g.drawLine(sc(6), sc(13), sc(5), sc(15));
            g.drawLine(sc(5), sc(15), sc(4), sc(15));
            g.drawLine(sc(5), sc(15), sc(6), sc(15));
            g.drawLine(sc(10), sc(13), sc(9), sc(15));
            g.drawLine(sc(9), sc(15), sc(8), sc(15));
            g.drawLine(sc(9), sc(15), sc(10), sc(15));
        }
    }

    // ======================== RABBIT ========================

    private static class RabbitGenerator extends AnimGen {
        RabbitGenerator(float scale, int size, String resDir) { super(scale, size, resDir); }
        protected String animalName() { return "rabbit"; }

        protected void generateFrame(Graphics2D g, int f) {
            double phase = f * 2 * Math.PI / FRAMES;
            int hopBounce = Math.round(scf(2) * (float) Math.abs(Math.sin(phase * 2)));
            int earWiggle = Math.round(scf(1) * (float) Math.sin(phase));

            // Ears
            g.setColor(new Color(240, 240, 245));
            g.fillOval(sc(2) + earWiggle, sc(-2), sc(3), sc(7));
            g.fillOval(sc(5) - earWiggle, sc(-2), sc(3), sc(7));
            g.setColor(new Color(255, 180, 180));
            g.fillOval(sc(3) + earWiggle, sc(0), sc(1), sc(4));
            g.fillOval(sc(6) - earWiggle, sc(0), sc(1), sc(4));

            // Tail
            g.setColor(Color.WHITE);
            g.fillOval(sc(-1), sc(6) - hopBounce, sc(4), sc(4));

            // Body
            g.setColor(new Color(240, 240, 245));
            g.fillOval(sc(1), sc(4) - hopBounce, sc(10), sc(9));
            g.setColor(new Color(200, 200, 210));
            g.fillOval(sc(2), sc(6) - hopBounce, sc(7), sc(5));

            // Head
            g.setColor(new Color(240, 240, 245));
            g.fillOval(sc(7), sc(2) - hopBounce, sc(8), sc(8));
            g.setColor(new Color(200, 200, 210));
            g.fillOval(sc(8), sc(4) - hopBounce, sc(5), sc(4));

            // Eyes
            g.setColor(Color.WHITE);
            g.fillOval(sc(10), sc(4) - hopBounce, sc(3), sc(3));
            g.setColor(new Color(60, 40, 30));
            g.fillOval(sc(11), sc(4) - hopBounce, sc(2), sc(3));
            g.setColor(Color.WHITE);
            g.fillOval(sc(11), sc(4) - hopBounce, sc(1), sc(1));

            // Nose
            int noseTwitch = (f % 2 == 0) ? 0 : sc(1);
            g.setColor(new Color(255, 150, 160));
            g.fillOval(sc(13) + noseTwitch, sc(6) - hopBounce, sc(2), sc(2));

            // Whiskers
            g.setColor(new Color(180, 180, 190));
            g.setStroke(new BasicStroke(scf(0.5f)));
            g.drawLine(sc(14), sc(7) - hopBounce, sc(16), sc(6) - hopBounce);
            g.drawLine(sc(14), sc(7) - hopBounce, sc(16), sc(8) - hopBounce);

            // Legs
            g.setColor(new Color(200, 200, 210));
            g.setStroke(new BasicStroke(scf(2f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int legExt = Math.round(scf(2) * (float) Math.sin(phase));
            int hindExt = Math.round(scf(3) * (float) Math.sin(phase + Math.PI));
            g.drawLine(sc(3), sc(12) - hopBounce, sc(3) + legExt, sc(14) - hopBounce);
            g.drawLine(sc(5), sc(12) - hopBounce, sc(5) - legExt, sc(14) - hopBounce);
            g.drawLine(sc(8), sc(12) - hopBounce, sc(8) + hindExt, sc(14) - hopBounce);
        }
    }

    // ======================== PENGUIN ========================

    private static class PenguinGenerator extends AnimGen {
        PenguinGenerator(float scale, int size, String resDir) { super(scale, size, resDir); }
        protected String animalName() { return "penguin"; }

        protected void generateFrame(Graphics2D g, int f) {
            double phase = f * 2 * Math.PI / FRAMES;
            int waddle = Math.round(scf(1) * (float) Math.sin(phase * 2));

            // Flippers
            g.setColor(new Color(30, 35, 40));
            g.fillOval(sc(0) + waddle, sc(4), sc(4), sc(8));
            g.fillOval(sc(12) - waddle, sc(4), sc(4), sc(8));

            // Body
            g.fillOval(sc(2), sc(0), sc(12), sc(16));
            g.setColor(new Color(250, 250, 252));
            g.fillOval(sc(4), sc(2), sc(8), sc(12));
            g.setColor(new Color(255, 255, 240));
            g.fillOval(sc(5), sc(3), sc(6), sc(10));

            // Head
            g.setColor(new Color(30, 35, 40));
            g.fillOval(sc(3), sc(0), sc(10), sc(7));
            g.setColor(new Color(250, 250, 252));
            g.fillOval(sc(4), sc(1), sc(8), sc(5));

            // Eyes
            g.setColor(new Color(40, 40, 45));
            g.fillOval(sc(5), sc(2), sc(2), sc(2));
            g.fillOval(sc(9), sc(2), sc(2), sc(2));
            g.setColor(Color.WHITE);
            g.fillOval(sc(5), sc(2), sc(1), sc(1));
            g.fillOval(sc(9), sc(2), sc(1), sc(1));

            // Beak
            int beakOff = (f % 2 == 0) ? 0 : sc(1);
            g.setColor(new Color(255, 160, 50));
            g.fillPolygon(new int[]{sc(7), sc(8), sc(9)}, new int[]{sc(5), sc(7) + beakOff, sc(5)}, 3);

            // Feet
            g.fillOval(sc(4) + waddle, sc(14), sc(3), sc(2));
            g.fillOval(sc(9) - waddle, sc(14), sc(3), sc(2));
        }
    }
}
