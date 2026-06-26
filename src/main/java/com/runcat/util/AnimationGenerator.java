package com.runcat.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Generates high-quality animation frame PNG files.
 * Two separate coordinate systems:
 *   - 16x16: compact taskbar icons (simple, recognizable)
 *   - 64x64: desktop pet (cute, rounded, expressive)
 *
 * Usage: run with no args to generate all.
 */
public class AnimationGenerator {

    private static final int FRAMES = 8;

    record TailMotion(int startX, int startY, int ctrlX, int ctrlY, int tipX, int tipY) {}

    public static void main(String[] args) {
        generateAll16();
        generateAll64();
        System.out.println("All animations generated!");
    }

    public static void generateAll16() {
        String dir = "src/main/resources/animations";
        new Cat16(dir).generate();
        new CatSleep16(dir).generate();
        new Dog16(dir).generate();
        new Horse16(dir).generate();
        new Parrot16(dir).generate();
        new Rabbit16(dir).generate();
        new Penguin16(dir).generate();
    }

    public static void generateAll64() {
        String dir = "src/main/resources/animations_hd";
        new Cat64(dir).generate();
        new CatSleep64(dir).generate();
        new Dog64(dir).generate();
        new Horse64(dir).generate();
        new Parrot64(dir).generate();
        new Rabbit64(dir).generate();
        new Penguin64(dir).generate();
    }

    // ======================== HELPERS ========================

    private static void enableAA(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    private static void saveFrame(BufferedImage img, String dir, String animal, int frame) {
        String d = dir + "/" + animal;
        new File(d).mkdirs();
        try { ImageIO.write(img, "png", new File(d + "/" + animal + "_" + frame + ".png")); }
        catch (IOException e) { System.err.println("Failed: " + e.getMessage()); }
    }

    private static double phase(int f) { return f * 2 * Math.PI / FRAMES; }

    static TailMotion catTailMotion16(int frame, int bodyY) {
        double p = phase(frame);
        int lift = (int) Math.round(2.6 * Math.sin(p));
        return new TailMotion(
                4, bodyY + 2,
                2, bodyY - 1 - lift / 2,
                2, 2 - lift
        );
    }

    static TailMotion catTailMotion64(int frame) {
        double p = phase(frame);
        int sway = (int) Math.round(5 * Math.sin(p));
        return new TailMotion(
                18, 32,
                10 - sway, 22,
                4 - sway, 16
        );
    }

    // ======================== 16x16 TASKBAR ANIMATIONS ========================

    private static abstract class Anim16 {
        protected final String dir;
        Anim16(String dir) { this.dir = dir; }
        abstract void drawFrame(Graphics2D g, int f);
        abstract String name();
        void generate() {
            for (int f = 0; f < FRAMES; f++) {
                BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = img.createGraphics();
                enableAA(g);
                drawFrame(g, f);
                g.dispose();
                saveFrame(img, dir, name(), f);
            }
        }
    }

    private static class Cat16 extends Anim16 {
        Cat16(String d) { super(d); }
        String name() { return "cat"; }
        void drawFrame(Graphics2D g, int f) {
            double p = phase(f);
            int bodyY = 5 + (int)(0.4 * Math.sin(p * 2));
            int headY = 1 + (int)(0.5 * Math.sin(p * 2 + 0.25));

            // Tail: diagonal up/down sway from the rear hip
            g.setColor(new Color(205, 112, 24));
            g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            TailMotion tail = catTailMotion16(f, bodyY);
            g.draw(new QuadCurve2D.Float(
                    tail.startX(), tail.startY(),
                    tail.ctrlX(), tail.ctrlY(),
                    tail.tipX(), tail.tipY()));

            // Body
            g.setColor(new Color(251, 156, 61));
            g.fillOval(3, bodyY, 9, 7);
            g.setColor(new Color(255, 216, 165));
            g.fillOval(5, bodyY + 2, 5, 3);

            // Head
            g.setColor(new Color(251, 156, 61));
            g.fillOval(8, headY, 6, 6);
            // Ears
            g.setColor(new Color(251, 156, 61));
            g.fillPolygon(new int[]{9,10,11}, new int[]{headY + 1, headY - 1, headY + 1}, 3);
            g.fillPolygon(new int[]{12,13,14}, new int[]{headY + 1, headY - 1, headY + 1}, 3);
            g.setColor(new Color(255,178,178));
            g.fillPolygon(new int[]{10,10,11}, new int[]{headY + 1, headY, headY + 1}, 3);
            g.fillPolygon(new int[]{13,13,14}, new int[]{headY + 1, headY, headY + 1}, 3);
            // Eye
            boolean blink = (f == 3 || f == 7);
            if (blink) {
                g.setColor(new Color(120,60,10));
                g.drawLine(11, headY + 3, 13, headY + 3);
            } else {
                g.setColor(Color.WHITE);
                g.fillOval(10, headY + 2, 4, 3);
                g.setColor(new Color(62,156,230));
                g.fillOval(11, headY + 2, 2, 3);
                g.setColor(new Color(20,30,50));
                g.fillOval(11, headY + 3, 1, 2);
                g.setColor(Color.WHITE);
                g.fillOval(11, headY + 2, 1, 1);
            }
            // Nose
            g.setColor(new Color(255,130,140));
            g.fillPolygon(new int[]{13,14,13}, new int[]{headY + 4, headY + 5, headY + 5}, 3);
            // Legs
            int l1 = (int)(3 * Math.sin(p));
            int l2 = (int)(3 * Math.sin(p + Math.PI));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(205,112,24));
            g.drawLine(5, bodyY + 5, 5 + l1, 13);
            g.drawLine(7, bodyY + 5, 7 + l2, 13);
            g.drawLine(9, bodyY + 5, 9 - l1, 13);
            g.drawLine(10, bodyY + 5, 10 - l2, 13);
        }
    }

    private static class CatSleep16 extends Anim16 {
        CatSleep16(String d) { super(d); }
        String name() { return "cat_sleep"; }
        void drawFrame(Graphics2D g, int f) {
            double p = phase(f);
            int by = (int)(0.5 * Math.sin(p));
            // Curled body
            g.setColor(new Color(255,150,50));
            g.fillOval(1, 4 + by, 13, 9);
            g.fillOval(1, 2 + by, 8, 7);
            g.setColor(new Color(255,210,150));
            g.fillOval(3, 6 + by, 9, 5);
            // Ears
            g.setColor(new Color(255,150,50));
            g.fillPolygon(new int[]{2,3,5}, new int[]{2+by,0+by,2+by}, 3);
            g.fillPolygon(new int[]{5,6,7}, new int[]{2+by,0+by,2+by}, 3);
            // Closed eyes
            g.setColor(new Color(120,60,10));
            g.setStroke(new BasicStroke(0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(3, 5 + by, 2, 1, 0, 180);
            g.drawArc(6, 5 + by, 2, 1, 0, 180);
            // Tail
            g.setColor(new Color(200,100,20));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(10, 8 + by, 5, 4, 90, 120);
            // Zzz
            if (f % 4 >= 1) { g.setColor(new Color(80,160,255,180)); g.setFont(g.getFont().deriveFont(5f)); g.drawString("z", 10, 3); }
            if (f % 4 >= 2) { g.setFont(g.getFont().deriveFont(6f)); g.drawString("Z", 12, 1); }
        }
    }

    private static class Dog16 extends Anim16 {
        Dog16(String d) { super(d); }
        String name() { return "dog"; }
        void drawFrame(Graphics2D g, int f) {
            double p = phase(f);
            g.setColor(new Color(200,150,80));
            g.fillOval(2, 5, 10, 7);
            g.setColor(new Color(240,200,130));
            g.fillOval(3, 7, 7, 4);
            g.setColor(new Color(200,150,80));
            g.fillOval(9, 1, 6, 7);
            g.setColor(new Color(240,200,130));
            g.fillOval(12, 3, 4, 4);
            // Ears
            g.setColor(new Color(140,90,50));
            g.fillOval(8, 2, 4, 6);
            g.fillOval(12, 3, 4, 5);
            // Eye
            g.setColor(Color.WHITE);
            g.fillOval(11, 2, 3, 3);
            g.setColor(new Color(60,40,20));
            g.fillOval(12, 2, 2, 3);
            g.setColor(Color.WHITE);
            g.fillOval(12, 2, 1, 1);
            // Nose
            g.setColor(new Color(30,30,30));
            g.fillOval(14, 4, 2, 2);
            // Tail
            int wag = (int)(25 * Math.sin(p * 3));
            g.setColor(new Color(140,90,50));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.rotate(Math.toRadians(wag), 2, 6);
            g.drawLine(2, 6, -1, 3);
            g.rotate(-Math.toRadians(wag), 2, 6);
            // Legs
            int l1 = (int)(3 * Math.sin(p));
            int l2 = (int)(3 * Math.sin(p + Math.PI));
            g.setColor(new Color(140,90,50));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(3, 11, 3 + l1, 13);
            g.drawLine(5, 11, 5 + l2, 13);
            g.drawLine(8, 11, 8 - l1, 13);
            g.drawLine(10, 11, 10 - l2, 13);
        }
    }

    private static class Horse16 extends Anim16 {
        Horse16(String d) { super(d); }
        String name() { return "horse"; }
        void drawFrame(Graphics2D g, int f) {
            double p = phase(f);
            g.setColor(new Color(170,120,70));
            g.fillOval(1, 4, 12, 8);
            g.setColor(new Color(210,170,120));
            g.fillOval(2, 6, 8, 4);
            // Neck + Head
            g.setColor(new Color(170,120,70));
            g.fillPolygon(new int[]{10,12,13,11}, new int[]{4,0,1,5}, 4);
            g.fillOval(10, -1, 6, 5);
            g.setColor(new Color(210,170,120));
            g.fillOval(11, 0, 4, 2);
            // Eye
            g.setColor(Color.WHITE);
            g.fillOval(12, 0, 2, 2);
            g.setColor(new Color(40,25,10));
            g.fillOval(12, 0, 1, 2);
            // Tail
            g.setColor(new Color(60,40,25));
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int ts = (int)(2 * Math.sin(p));
            g.drawArc(-3, 3 + ts, 5, 5, 70, 110);
            // Legs
            int bounce = (int)(2 * Math.abs(Math.sin(p * 2)));
            g.setColor(new Color(140,95,55));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int fl = (int)(4 * Math.sin(p));
            g.drawLine(3, 10, 3 + fl, 13 - bounce);
            g.drawLine(5, 10, 5 - fl, 13 - bounce);
            g.drawLine(8, 10, 8 + fl/2, 13 - bounce);
            g.drawLine(10, 10, 10 - fl/2, 13 - bounce);
        }
    }

    private static class Parrot16 extends Anim16 {
        Parrot16(String d) { super(d); }
        String name() { return "parrot"; }
        void drawFrame(Graphics2D g, int f) {
            double p = phase(f);
            int wf = (int)(4 * Math.sin(p));
            // Body
            g.setColor(new Color(30,180,90));
            g.fillOval(4, 4, 9, 9);
            g.setColor(new Color(100,230,140));
            g.fillOval(5, 6, 6, 5);
            // Head
            g.setColor(new Color(50,200,100));
            g.fillOval(10, 1, 6, 6);
            // Wing
            g.setColor(new Color(20,140,60));
            g.fillOval(0, 4 + wf, 6, 5);
            // Eye
            g.setColor(Color.WHITE);
            g.fillOval(13, 2, 3, 3);
            g.setColor(Color.BLACK);
            g.fillOval(14, 3, 1, 1);
            // Beak
            g.setColor(new Color(255,180,50));
            g.fillPolygon(new int[]{15,17,15}, new int[]{4,5,7}, 3);
            // Tail
            g.setColor(new Color(230,60,60));
            g.fillOval(1, 11, 4, 5);
            g.setColor(new Color(40,100,230));
            g.fillOval(3, 11, 4, 4);
        }
    }

    private static class Rabbit16 extends Anim16 {
        Rabbit16(String d) { super(d); }
        String name() { return "rabbit"; }
        void drawFrame(Graphics2D g, int f) {
            double p = phase(f);
            int hop = (int)(2 * Math.abs(Math.sin(p * 2)));
            int ew = (int)(1 * Math.sin(p));
            // Body
            g.setColor(new Color(240,240,245));
            g.fillOval(1, 4 - hop, 10, 9);
            g.setColor(new Color(200,200,210));
            g.fillOval(2, 6 - hop, 7, 5);
            // Head
            g.setColor(new Color(240,240,245));
            g.fillOval(6, 2 - hop, 8, 8);
            // Ears connected to the top of the head
            g.setColor(new Color(240,240,245));
            g.fillOval(7 + ew, -3 - hop, 3, 6);
            g.fillOval(10 - ew, -3 - hop, 3, 6);
            g.setColor(new Color(255,180,180));
            g.fillOval(8 + ew, -1 - hop, 1, 4);
            g.fillOval(11 - ew, -1 - hop, 1, 4);
            // Eye
            g.setColor(Color.WHITE);
            g.fillOval(9, 4 - hop, 3, 3);
            g.setColor(new Color(60,40,30));
            g.fillOval(10, 4 - hop, 2, 3);
            g.setColor(Color.WHITE);
            g.fillOval(10, 4 - hop, 1, 1);
            // Nose
            int nt = (f % 2 == 0) ? 0 : 1;
            g.setColor(new Color(255,150,160));
            g.fillOval(12 + nt, 6 - hop, 2, 2);
            // Tail
            g.setColor(Color.WHITE);
            g.fillOval(-1, 6 - hop, 4, 4);
            // Legs
            g.setColor(new Color(200,200,210));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int le = (int)(2 * Math.sin(p));
            g.drawLine(3, 12 - hop, 3 + le, 14 - hop);
            g.drawLine(5, 12 - hop, 5 - le, 14 - hop);
            g.drawLine(8, 12 - hop, 8 + le, 14 - hop);
        }
    }

    private static class Penguin16 extends Anim16 {
        Penguin16(String d) { super(d); }
        String name() { return "penguin"; }
        void drawFrame(Graphics2D g, int f) {
            double p = phase(f);
            int wd = (int)(1 * Math.sin(p * 2));
            // Body
            g.setColor(new Color(30,35,40));
            g.fillOval(2, 0, 12, 16);
            g.setColor(new Color(250,250,252));
            g.fillOval(4, 2, 8, 12);
            // Head
            g.setColor(new Color(30,35,40));
            g.fillOval(3, 0, 10, 7);
            g.setColor(new Color(250,250,252));
            g.fillOval(4, 1, 8, 5);
            // Eyes
            g.setColor(new Color(40,40,45));
            g.fillOval(5, 2, 2, 2);
            g.fillOval(9, 2, 2, 2);
            g.setColor(Color.WHITE);
            g.fillOval(5, 2, 1, 1);
            g.fillOval(9, 2, 1, 1);
            // Beak
            int bo = (f % 2 == 0) ? 0 : 1;
            g.setColor(new Color(255,160,50));
            g.fillPolygon(new int[]{7,8,9}, new int[]{5,7+bo,5}, 3);
            // Flippers
            g.setColor(new Color(30,35,40));
            g.fillOval(0 + wd, 4, 4, 8);
            g.fillOval(12 - wd, 4, 4, 8);
            // Feet
            g.setColor(new Color(255,160,50));
            g.fillOval(4 + wd, 14, 3, 2);
            g.fillOval(9 - wd, 14, 3, 2);
        }
    }

    // ======================== 64x64 DESKTOP PET ANIMATIONS ========================
    // Cute, rounded, expressive style with proper anatomy

    private static abstract class Anim64 {
        protected final String dir;
        Anim64(String dir) { this.dir = dir; }
        abstract void drawFrame(Graphics2D g, int f);
        abstract String name();
        void generate() {
            for (int f = 0; f < FRAMES; f++) {
                BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = img.createGraphics();
                enableAA(g);
                drawFrame(g, f);
                g.dispose();
                saveFrame(img, dir, name(), f);
            }
        }
    }

    // Orange cat with round head, big eyes, natural leg movement, swishing tail
    private static class Cat64 extends Anim64 {
        Cat64(String d) { super(d); }
        String name() { return "cat"; }
        void drawFrame(Graphics2D g, int f) {
            double p = phase(f);

            int bodyBounce = (int)(1.0 * Math.sin(p * 2));
            int headBob = (int)(1.1 * Math.sin(p * 2 + 0.3));
            TailMotion tail = catTailMotion64(f);

            // Body (drawn FIRST so tail overlays on top)
            g.setColor(new Color(250, 155, 60));
            g.fillOval(15, 30 + bodyBounce, 27, 18);
            g.setColor(new Color(255, 218, 166));
            g.fillOval(19, 34 + bodyBounce, 18, 11);

            // Back stripe
            g.setColor(new Color(222, 114, 27));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(18, 31 + bodyBounce, 11, 6, 0, 180);
            g.drawArc(21, 35 + bodyBounce, 12, 5, 0, 180);

            // Tail: swishing from rear hip, drawn AFTER body so it connects visually
            g.setColor(new Color(223, 118, 28));
            g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new QuadCurve2D.Float(
                    tail.startX(), tail.startY() + bodyBounce,
                    tail.ctrlX(), tail.ctrlY() + bodyBounce,
                    tail.tipX(), tail.tipY() + bodyBounce));
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 194, 114, 160));
            g.draw(new QuadCurve2D.Float(
                    tail.startX(), tail.startY() + bodyBounce,
                    tail.ctrlX() + 1, tail.ctrlY() + 1 + bodyBounce,
                    tail.tipX(), tail.tipY() + 2 + bodyBounce));

            // Head
            g.setColor(new Color(250, 155, 60));
            g.fillOval(28, 10 + headBob, 24, 22);
            g.setColor(new Color(255, 218, 166));
            g.fillOval(33, 15 + headBob, 15, 13);

            // Ears
            g.setColor(new Color(250, 155, 60));
            g.fillPolygon(new int[]{32, 31, 37}, new int[]{11 + headBob, 4 + headBob, 7 + headBob}, 3);
            g.fillPolygon(new int[]{43, 46, 49}, new int[]{11 + headBob, 4 + headBob, 7 + headBob}, 3);
            g.setColor(new Color(255, 178, 178));
            g.fillPolygon(new int[]{33, 32, 36}, new int[]{10 + headBob, 5 + headBob, 8 + headBob}, 3);
            g.fillPolygon(new int[]{44, 46, 47}, new int[]{10 + headBob, 5 + headBob, 8 + headBob}, 3);

            boolean blink = (f == 3 || f == 7);
            if (blink) {
                g.setColor(new Color(100, 60, 20));
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawArc(35, 20 + headBob, 7, 4, 0, 180);
                g.drawArc(44, 20 + headBob, 7, 4, 0, 180);
            } else {
                g.setColor(Color.WHITE);
                g.fillOval(34, 17 + headBob, 9, 10);
                g.fillOval(43, 17 + headBob, 9, 10);
                g.setColor(new Color(66, 157, 235));
                g.fillOval(36, 18 + headBob, 6, 8);
                g.fillOval(45, 18 + headBob, 6, 8);
                g.setColor(new Color(22, 30, 48));
                g.fillOval(37, 20 + headBob, 4, 6);
                g.fillOval(46, 20 + headBob, 4, 6);
                g.setColor(Color.WHITE);
                g.fillOval(36, 18 + headBob, 2, 2);
                g.fillOval(45, 18 + headBob, 2, 2);
            }

            g.setColor(new Color(255, 140, 150));
            int[] nx = {42, 44, 40};
            int[] ny = {26 + headBob, 27 + headBob, 27 + headBob};
            g.fillPolygon(nx, ny, 3);

            g.setColor(new Color(200, 100, 80));
            g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(38, 28 + headBob, 5, 3, 0, 180);
            g.drawArc(43, 28 + headBob, 5, 3, 0, 180);

            g.setColor(new Color(200, 160, 120));
            g.setStroke(new BasicStroke(0.8f));
            int wh = (int)(1 * Math.sin(p));
            g.drawLine(37, 25 + headBob + wh, 30, 23 + headBob + wh);
            g.drawLine(37, 26 + headBob + wh, 30, 28 + headBob + wh);
            g.drawLine(49, 25 + headBob - wh, 57, 23 + headBob - wh);
            g.drawLine(49, 26 + headBob - wh, 57, 28 + headBob - wh);

            g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(230, 120, 30));

            int fl1 = (int)(6 * Math.sin(p));
            int fl2 = (int)(6 * Math.sin(p + Math.PI));
            g.drawLine(21, 46 + bodyBounce, 21 + fl1, 55 + Math.abs(fl1)/3);
            g.drawLine(27, 46 + bodyBounce, 27 + fl2, 55 + Math.abs(fl2)/3);
            int bl1 = (int)(5 * Math.sin(p + Math.PI * 0.5));
            int bl2 = (int)(5 * Math.sin(p + Math.PI * 1.5));
            g.drawLine(32, 46 + bodyBounce, 32 + bl1, 55 + Math.abs(bl1)/3);
            g.drawLine(37, 46 + bodyBounce, 37 + bl2, 55 + Math.abs(bl2)/3);

            g.setColor(new Color(255, 215, 155));
            g.fillOval(19 + fl1, 53 + Math.abs(fl1)/3, 7, 4);
            g.fillOval(25 + fl2, 53 + Math.abs(fl2)/3, 7, 4);
            g.fillOval(30 + bl1, 53 + Math.abs(bl1)/3, 7, 4);
            g.fillOval(35 + bl2, 53 + Math.abs(bl2)/3, 7, 4);

            // Head stripes
            g.setColor(new Color(222, 114, 27));
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(35, 10 + headBob, 9, 4, 0, 180);
        }
    }

    // Sleeping cat - curled up, breathing, Zzz
    private static class CatSleep64 extends Anim64 {
        CatSleep64(String d) { super(d); }
        String name() { return "cat_sleep"; }
        void drawFrame(Graphics2D g, int f) {
            double p = phase(f);
            int breath = (int)(2 * Math.sin(p));

            // Curled body
            g.setColor(new Color(255, 155, 55));
            g.fillOval(6, 24 + breath, 42, 28);
            g.fillOval(6, 12 + breath, 28, 22);
            // Belly
            g.setColor(new Color(255, 215, 155));
            g.fillOval(12, 32 + breath, 30, 16);
            g.fillOval(10, 18 + breath, 18, 14);

            // Tail wrapped around
            g.setColor(new Color(230, 120, 30));
            g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int tailW = (int)(3 * Math.sin(p));
            g.drawArc(38, 30 + breath + tailW, 18, 14, 90, 150);

            // Ears
            g.setColor(new Color(255, 155, 55));
            int[] e1x = {12, 10, 18}; int[] e1y = {14 + breath, 6 + breath, 10 + breath};
            g.fillPolygon(e1x, e1y, 3);
            int[] e2x = {22, 20, 28}; int[] e2y = {14 + breath, 6 + breath, 10 + breath};
            g.fillPolygon(e2x, e2y, 3);
            // Inner ears
            g.setColor(new Color(255, 175, 175));
            int[] ie1x = {13, 12, 17}; int[] ie1y = {13 + breath, 8 + breath, 11 + breath};
            g.fillPolygon(ie1x, ie1y, 3);
            int[] ie2x = {23, 22, 27}; int[] ie2y = {13 + breath, 8 + breath, 11 + breath};
            g.fillPolygon(ie2x, ie2y, 3);

            // Closed eyes - happy sleeping arcs
            g.setColor(new Color(100, 60, 20));
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(14, 20 + breath, 7, 4, 0, 180);
            g.drawArc(24, 20 + breath, 7, 4, 0, 180);

            // Nose
            g.setColor(new Color(255, 140, 150));
            int[] nx = {22, 24, 20};
            int[] ny = {24 + breath, 26 + breath, 26 + breath};
            g.fillPolygon(nx, ny, 3);

            // Paws
            g.setColor(new Color(255, 215, 155));
            g.fillOval(14, 48 + breath, 10, 6);
            g.fillOval(30, 48 + breath, 10, 6);

            // Zzz
            int zAlpha = 160 + (int)(Math.sin(p * 2) * 60);
            g.setColor(new Color(80, 160, 255, zAlpha));
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            int zf = f % 4;
            if (zf >= 1) g.drawString("z", 34, 14);
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            if (zf >= 2) g.drawString("Z", 40, 8);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            if (zf >= 3) g.drawString("Z", 48, 2);

            // Stripes
            g.setColor(new Color(220, 110, 25));
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(14, 28 + breath, 14, 5, 0, 180);
            g.drawArc(16, 34 + breath, 14, 5, 0, 180);
        }
    }

    // Brown dog with floppy ears, wagging tail, tongue out
    private static class Dog64 extends Anim64 {
        Dog64(String d) { super(d); }
        String name() { return "dog"; }
        void drawFrame(Graphics2D g, int f) {
            double p = phase(f);
            int bounce = (int)(2 * Math.sin(p * 2));

            // Tail wagging
            g.setColor(new Color(140, 90, 50));
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int wagDeg = (int)(20 * Math.sin(p * 3));
            g.rotate(Math.toRadians(wagDeg), 14, 34);
            g.drawLine(14, 34, 4, 22);
            g.rotate(-Math.toRadians(wagDeg), 14, 34);

            // Body
            g.setColor(new Color(200, 150, 80));
            g.fillOval(12, 30 + bounce, 30, 20);
            g.setColor(new Color(240, 200, 130));
            g.fillOval(16, 36 + bounce, 22, 12);

            // Head
            int headBob = (int)(1.5 * Math.sin(p * 2 + 0.3));
            g.setColor(new Color(200, 150, 80));
            g.fillOval(32, 8 + headBob, 24, 24);
            // Face patch
            g.setColor(new Color(240, 200, 130));
            g.fillOval(40, 16 + headBob, 14, 12);

            // Floppy ears
            g.setColor(new Color(140, 90, 50));
            g.fillOval(30, 10 + headBob, 10, 16);
            g.fillOval(46, 12 + headBob, 10, 14);

            // Eye
            g.setColor(Color.WHITE);
            g.fillOval(40, 14 + headBob, 9, 10);
            g.setColor(new Color(60, 40, 20));
            g.fillOval(42, 15 + headBob, 6, 8);
            g.setColor(Color.WHITE);
            g.fillOval(42, 15 + headBob, 3, 3);

            // Nose
            g.setColor(new Color(30, 30, 30));
            g.fillOval(50, 22 + headBob, 6, 5);
            // Nose highlight
            g.setColor(new Color(80, 80, 80));
            g.fillOval(51, 23 + headBob, 2, 2);

            // Tongue
            if (f % 2 == 0) {
                g.setColor(new Color(255, 100, 110));
                int tl = 4 + (f % 3);
                g.fillOval(52, 28 + headBob, 5, tl);
            }

            // Legs
            g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(140, 90, 50));
            int l1 = (int)(6 * Math.sin(p));
            int l2 = (int)(6 * Math.sin(p + Math.PI));
            int l3 = (int)(5 * Math.sin(p + Math.PI * 0.5));
            int l4 = (int)(5 * Math.sin(p + Math.PI * 1.5));
            g.drawLine(18, 47 + bounce, 18 + l1, 56 + Math.abs(l1)/4);
            g.drawLine(24, 47 + bounce, 24 + l2, 56 + Math.abs(l2)/4);
            g.drawLine(32, 47 + bounce, 32 + l3, 56 + Math.abs(l3)/4);
            g.drawLine(38, 47 + bounce, 38 + l4, 56 + Math.abs(l4)/4);

            // Paws
            g.setColor(new Color(240, 200, 130));
            g.fillOval(15 + l1, 53 + Math.abs(l1)/4, 8, 5);
            g.fillOval(21 + l2, 53 + Math.abs(l2)/4, 8, 5);
            g.fillOval(29 + l3, 53 + Math.abs(l3)/4, 8, 5);
            g.fillOval(35 + l4, 53 + Math.abs(l4)/4, 8, 5);
        }
    }

    // Brown horse with mane, galloping legs
    private static class Horse64 extends Anim64 {
        Horse64(String d) { super(d); }
        String name() { return "horse"; }
        void drawFrame(Graphics2D g, int f) {
            double p = phase(f);
            int bounce = (int)(3 * Math.abs(Math.sin(p * 2)));

            // Tail
            g.setColor(new Color(60, 40, 25));
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int ts = (int)(5 * Math.sin(p));
            g.drawArc(2 + ts, 20, 16, 18, 80, 120);
            // Tail hair
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc(4 + ts, 24, 12, 14, 100, 100);
            g.drawArc(6 + ts, 28, 10, 10, 120, 80);

            // Body
            g.setColor(new Color(170, 120, 70));
            g.fillOval(8, 26 - bounce, 34, 22);
            g.setColor(new Color(210, 170, 120));
            g.fillOval(12, 32 - bounce, 26, 14);

            // Neck
            g.setColor(new Color(170, 120, 70));
            int[] nx = {36, 42, 44, 38};
            int[] ny = {26 - bounce, 8 - bounce, 10 - bounce, 28 - bounce};
            g.fillPolygon(nx, ny, 4);

            // Head
            g.setColor(new Color(170, 120, 70));
            g.fillOval(38, 4 - bounce, 22, 16);
            g.setColor(new Color(210, 170, 120));
            g.fillOval(42, 8 - bounce, 14, 8);
            // Muzzle
            g.setColor(new Color(150, 100, 60));
            g.fillOval(52, 10 - bounce, 10, 10);

            // Mane
            g.setColor(new Color(60, 40, 25));
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int m = 0; m < 5; m++) {
                int mw = (int)(3 * Math.sin(p + m * 0.4));
                g.drawLine(38 + m * 2, 8 + m * 4 - bounce, 34 + mw + m, 10 + m * 4 - bounce);
            }

            // Eye
            g.setColor(Color.WHITE);
            g.fillOval(44, 8 - bounce, 7, 7);
            g.setColor(new Color(40, 25, 10));
            g.fillOval(46, 9 - bounce, 4, 5);
            g.setColor(Color.WHITE);
            g.fillOval(46, 9 - bounce, 2, 2);

            // Nostril
            g.setColor(new Color(40, 25, 10));
            g.fillOval(56, 14 - bounce, 3, 3);

            // Legs
            g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(140, 95, 55));
            int fl1 = (int)(8 * Math.sin(p));
            int fl2 = (int)(8 * Math.sin(p + Math.PI));
            int bl1 = (int)(7 * Math.sin(p + Math.PI * 0.5));
            int bl2 = (int)(7 * Math.sin(p + Math.PI * 1.5));
            g.drawLine(14, 45 - bounce, 14 + fl1, 58 - bounce);
            g.drawLine(22, 45 - bounce, 22 + fl2, 58 - bounce);
            g.drawLine(30, 45 - bounce, 30 + bl1, 58 - bounce);
            g.drawLine(36, 45 - bounce, 36 + bl2, 58 - bounce);
            // Hooves
            g.setColor(new Color(50, 35, 20));
            g.fillOval(11 + fl1, 55 - bounce, 8, 5);
            g.fillOval(19 + fl2, 55 - bounce, 8, 5);
            g.fillOval(27 + bl1, 55 - bounce, 8, 5);
            g.fillOval(33 + bl2, 55 - bounce, 8, 5);
        }
    }

    // Green parrot with flapping wings, colorful tail
    private static class Parrot64 extends Anim64 {
        Parrot64(String d) { super(d); }
        String name() { return "parrot"; }
        void drawFrame(Graphics2D g, int f) {
            double p = phase(f);
            int wingFlap = (int)(10 * Math.sin(p));

            // Tail feathers (behind body)
            g.setColor(new Color(230, 60, 60));
            g.fillOval(4, 42, 10, 18);
            g.setColor(new Color(40, 100, 230));
            g.fillOval(10, 44, 10, 16);
            g.setColor(new Color(255, 220, 50));
            g.fillOval(16, 42, 10, 18);

            // Wing
            g.setColor(new Color(20, 140, 60));
            g.fillOval(4, 22 + wingFlap, 20, 16);
            g.setColor(new Color(80, 210, 120));
            g.fillOval(6, 26 + wingFlap, 12, 8);

            // Body
            g.setColor(new Color(30, 180, 90));
            g.fillOval(14, 18, 28, 28);
            g.setColor(new Color(100, 230, 140));
            g.fillOval(18, 24, 20, 18);

            // Head
            g.setColor(new Color(50, 200, 100));
            g.fillOval(32, 4, 24, 22);
            g.setColor(new Color(100, 230, 140));
            g.fillOval(36, 8, 16, 14);

            // Eye ring
            g.setColor(new Color(200, 200, 200));
            g.fillOval(44, 8, 12, 12);
            // Eye white
            g.setColor(Color.WHITE);
            g.fillOval(46, 10, 9, 9);
            // Iris
            g.setColor(new Color(180, 120, 20));
            g.fillOval(48, 11, 6, 7);
            // Pupil
            g.setColor(Color.BLACK);
            g.fillOval(49, 13, 4, 4);
            // Highlight
            g.setColor(Color.WHITE);
            g.fillOval(48, 11, 2, 2);

            // Beak
            g.setColor(new Color(255, 180, 50));
            g.fillPolygon(new int[]{52, 60, 52}, new int[]{18, 22, 28}, 3);
            g.setColor(new Color(220, 150, 30));
            g.setStroke(new BasicStroke(1.5f));
            g.drawLine(52, 22, 60, 22);

            // Feet
            g.setColor(new Color(160, 120, 40));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(22, 44, 20, 56);
            g.drawLine(20, 56, 16, 56);
            g.drawLine(20, 56, 24, 56);
            g.drawLine(30, 44, 28, 56);
            g.drawLine(28, 56, 24, 56);
            g.drawLine(28, 56, 32, 56);
        }
    }

    // White rabbit with long ears, hopping, fluffy tail
    private static class Rabbit64 extends Anim64 {
        Rabbit64(String d) { super(d); }
        String name() { return "rabbit"; }
        void drawFrame(Graphics2D g, int f) {
            double p = phase(f);
            int hop = (int)(5 * Math.abs(Math.sin(p * 2)));
            int earW = (int)(3 * Math.sin(p));

            // Fluffy tail (behind body)
            g.setColor(Color.WHITE);
            g.fillOval(2, 30 - hop, 12, 12);

            // Body
            g.setColor(new Color(240, 240, 245));
            g.fillOval(10, 22 - hop, 30, 26);
            g.setColor(new Color(215, 215, 225));
            g.fillOval(14, 28 - hop, 22, 18);

            // Head on top of body so ears are clearly above
            g.setColor(new Color(240, 240, 245));
            g.fillOval(29, 10 - hop, 24, 23);
            g.setColor(new Color(215, 215, 225));
            g.fillOval(33, 16 - hop, 16, 14);

            // Ears anchored to the top of the head (drawn after head so they extend upward)
            g.setColor(new Color(240, 240, 245));
            g.fillOval(34 + earW, -2 - hop, 6, 18);
            g.fillOval(40 - earW, -2 - hop, 6, 18);
            g.setColor(new Color(255, 185, 185));
            g.fillOval(36 + earW, 0 - hop, 2, 12);
            g.fillOval(42 - earW, 0 - hop, 2, 12);

            // Eyes
            g.setColor(Color.WHITE);
            g.fillOval(38, 16 - hop, 10, 10);
            g.fillOval(48, 16 - hop, 10, 10);
            g.setColor(new Color(80, 50, 40));
            g.fillOval(40, 17 - hop, 7, 8);
            g.fillOval(50, 17 - hop, 7, 8);
            // Pupil highlight
            g.setColor(Color.WHITE);
            g.fillOval(40, 17 - hop, 3, 3);
            g.fillOval(50, 17 - hop, 3, 3);

            // Nose
            int nt = (f % 2 == 0) ? 0 : 2;
            g.setColor(new Color(255, 155, 165));
            g.fillOval(52 + nt, 24 - hop, 5, 4);

            // Whiskers
            g.setColor(new Color(190, 190, 200));
            g.setStroke(new BasicStroke(0.8f));
            int wh = (int)(1 * Math.sin(p));
            g.drawLine(50, 26 - hop + wh, 62, 24 - hop + wh);
            g.drawLine(50, 27 - hop + wh, 62, 30 - hop + wh);
            g.drawLine(38, 26 - hop - wh, 26, 24 - hop - wh);
            g.drawLine(38, 27 - hop - wh, 26, 30 - hop - wh);

            // Front paws (two, small, static)
            g.setColor(new Color(215, 215, 225));
            g.fillOval(24, 44 - hop, 7, 5);
            g.fillOval(32, 44 - hop, 7, 5);

            // Back legs (two, slight movement when hopping)
            g.setColor(new Color(215, 215, 225));
            g.fillOval(14, 44 - hop, 8, 6);
            g.fillOval(26, 44 - hop, 8, 6);
        }
    }

    // Black & white penguin waddling
    private static class Penguin64 extends Anim64 {
        Penguin64(String d) { super(d); }
        String name() { return "penguin"; }
        void drawFrame(Graphics2D g, int f) {
            double p = phase(f);
            int waddle = (int)(2 * Math.sin(p * 2));
            int bodyLift = (int)(1.2 * Math.abs(Math.sin(p * 2)));

            // Feet
            g.setColor(new Color(245, 165, 74));
            g.fillOval(18 + waddle, 53, 10, 5);
            g.fillOval(36 - waddle, 53, 10, 5);

            // Body silhouette: teardrop instead of chick-like oval
            g.setColor(new Color(28, 34, 44));
            g.fillOval(14, 10 - bodyLift, 36, 42);
            g.fillOval(18, 4 - bodyLift, 28, 24);

            // Belly
            g.setColor(new Color(250, 250, 252));
            g.fillOval(20, 16 - bodyLift, 24, 30);
            g.setColor(new Color(255, 255, 247));
            g.fillOval(22, 18 - bodyLift, 20, 25);

            // Face mask, broader and flatter
            g.setColor(new Color(250, 250, 252));
            g.fillOval(20, 8 - bodyLift, 8, 12);
            g.fillOval(36, 8 - bodyLift, 8, 12);
            g.fillOval(24, 8 - bodyLift, 16, 9);

            // Eyes with more penguin-like spacing
            g.setColor(Color.WHITE);
            g.fillOval(22, 9 - bodyLift, 8, 8);
            g.fillOval(34, 9 - bodyLift, 8, 8);
            g.setColor(new Color(20, 24, 32));
            g.fillOval(24, 11 - bodyLift, 4, 5);
            g.fillOval(36, 11 - bodyLift, 4, 5);
            g.setColor(Color.WHITE);
            g.fillOval(24, 11 - bodyLift, 2, 2);
            g.fillOval(36, 11 - bodyLift, 2, 2);

            // Short beak, less chick-like
            int beakOff = (f % 2 == 0) ? 0 : 1;
            g.setColor(new Color(245, 172, 78));
            g.fillPolygon(new int[]{30, 32, 34}, new int[]{18 - bodyLift, 20 + beakOff - bodyLift, 18 - bodyLift}, 3);
            g.setColor(new Color(214, 132, 40));
            g.drawLine(30, 19 - bodyLift, 34, 19 - bodyLift);

            // Flippers
            g.setColor(new Color(28, 34, 44));
            g.fillOval(9 + waddle, 22 - bodyLift, 10, 22);
            g.fillOval(45 - waddle, 22 - bodyLift, 10, 22);
            g.setColor(new Color(45, 52, 64));
            g.fillOval(11 + waddle, 24 - bodyLift, 6, 17);
            g.fillOval(47 - waddle, 24 - bodyLift, 6, 17);

            // Cheeks
            g.setColor(new Color(255, 188, 188, 72));
            g.fillOval(20, 16 - bodyLift, 5, 3);
            g.fillOval(39, 16 - bodyLift, 5, 3);
        }
    }
}
