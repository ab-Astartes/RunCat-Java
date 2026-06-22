package com.runcat.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Generate application icon (multi-resolution PNG for jpackage / .ico conversion)
 */
public class IconGenerator {

    public static void main(String[] args) {
        String outputDir = args.length > 0 ? args[0] : "src/main/resources/icons";

        // Generate 256x256 icon (will be used as app icon)
        BufferedImage icon256 = generateIcon(256);
        savePng(icon256, outputDir + "/app-icon-256.png");

        // Generate 64x64
        BufferedImage icon64 = generateIcon(64);
        savePng(icon64, outputDir + "/app-icon-64.png");

        // Generate 32x32
        BufferedImage icon32 = generateIcon(32);
        savePng(icon32, outputDir + "/app-icon-32.png");

        // Generate 16x16
        BufferedImage icon16 = generateIcon(16);
        savePng(icon16, outputDir + "/app-icon-16.png");

        System.out.println("Application icons generated in " + outputDir);
    }

    private static BufferedImage generateIcon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        double s = size / 256.0;  // scale factor

        // Background circle
        g.setColor(new Color(255, 200, 50));
        g.fillOval(
                (int)(10 * s), (int)(10 * s),
                (int)(236 * s), (int)(236 * s));

        // Orange cat body
        g.setColor(new Color(255, 165, 0));
        g.fillOval(
                (int)(50 * s), (int)(90 * s),
                (int)(170 * s), (int)(120 * s));

        // Head
        g.fillOval(
                (int)(60 * s), (int)(30 * s),
                (int)(140 * s), (int)(110 * s));

        // Ears
        int[] leftEarX = {(int)(70 * s), (int)(90 * s), (int)(110 * s)};
        int[] leftEarY = {(int)(40 * s), (int)(5 * s), (int)(40 * s)};
        g.fillPolygon(leftEarX, leftEarY, 3);

        int[] rightEarX = {(int)(150 * s), (int)(170 * s), (int)(190 * s)};
        int[] rightEarY = {(int)(40 * s), (int)(5 * s), (int)(40 * s)};
        g.fillPolygon(rightEarX, rightEarY, 3);

        // Inner ears
        g.setColor(new Color(255, 200, 150));
        int[] ileftEarX = {(int)(80 * s), (int)(90 * s), (int)(105 * s)};
        int[] ileftEarY = {(int)(42 * s), (int)(15 * s), (int)(42 * s)};
        g.fillPolygon(ileftEarX, ileftEarY, 3);

        int[] irightEarX = {(int)(155 * s), (int)(170 * s), (int)(185 * s)};
        int[] irightEarY = {(int)(42 * s), (int)(15 * s), (int)(42 * s)};
        g.fillPolygon(irightEarX, irightEarY, 3);

        // Eyes
        g.setColor(Color.WHITE);
        g.fillOval((int)(95 * s), (int)(60 * s), (int)(40 * s), (int)(30 * s));
        g.fillOval((int)(150 * s), (int)(60 * s), (int)(40 * s), (int)(30 * s));

        // Pupils
        g.setColor(new Color(0, 100, 0));
        g.fillOval((int)(108 * s), (int)(65 * s), (int)(20 * s), (int)(25 * s));
        g.fillOval((int)(158 * s), (int)(65 * s), (int)(20 * s), (int)(25 * s));

        // Eye highlights
        g.setColor(Color.WHITE);
        g.fillOval((int)(112 * s), (int)(67 * s), (int)(8 * s), (int)(8 * s));
        g.fillOval((int)(162 * s), (int)(67 * s), (int)(8 * s), (int)(8 * s));

        // Nose
        g.setColor(new Color(255, 130, 130));
        int[] noseX = {(int)(123 * s), (int)(128 * s), (int)(133 * s)};
        int[] noseY = {(int)(85 * s), (int)(92 * s), (int)(85 * s)};
        g.fillPolygon(noseX, noseY, 3);

        // Mouth
        g.setColor(new Color(200, 100, 0));
        g.setStroke(new BasicStroke((float)(2 * s)));
        g.drawArc((int)(110 * s), (int)(90 * s), (int)(18 * s), (int)(15 * s), 0, -180);
        g.drawArc((int)(128 * s), (int)(90 * s), (int)(18 * s), (int)(15 * s), 0, -180);

        // Whiskers
        g.setColor(new Color(180, 100, 0));
        g.setStroke(new BasicStroke((float)(1.5 * s)));
        g.drawLine((int)(60 * s), (int)(80 * s), (int)(100 * s), (int)(88 * s));
        g.drawLine((int)(60 * s), (int)(95 * s), (int)(100 * s), (int)(95 * s));
        g.drawLine((int)(160 * s), (int)(88 * s), (int)(200 * s), (int)(80 * s));
        g.drawLine((int)(160 * s), (int)(95 * s), (int)(200 * s), (int)(95 * s));

        // Front legs (running pose)
        g.setColor(new Color(255, 140, 0));
        g.fillRect((int)(80 * s), (int)(190 * s), (int)(25 * s), (int)(40 * s));
        g.fillRect((int)(165 * s), (int)(195 * s), (int)(25 * s), (int)(35 * s));

        // Paws
        g.setColor(new Color(255, 200, 150));
        g.fillOval((int)(78 * s), (int)(225 * s), (int)(28 * s), (int)(12 * s));
        g.fillOval((int)(163 * s), (int)(225 * s), (int)(28 * s), (int)(12 * s));

        // Tail
        g.setColor(new Color(255, 140, 0));
        g.setStroke(new BasicStroke((float)(12 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc((int)(180 * s), (int)(120 * s), (int)(60 * s), (int)(60 * s), 90, -120);

        g.dispose();
        return img;
    }

    private static void savePng(BufferedImage img, String path) {
        try {
            new File(path).getParentFile().mkdirs();
            ImageIO.write(img, "png", new File(path));
            System.out.println("Generated: " + path);
        } catch (IOException e) {
            System.err.println("Failed: " + path + " - " + e.getMessage());
        }
    }
}
