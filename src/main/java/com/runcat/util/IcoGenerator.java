package com.runcat.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Generate ICO file from PNG images for Windows application icon
 * ICO format: header + directory entries + image data
 */
public class IcoGenerator {

    public static void main(String[] args) throws Exception {
        String inputDir = args.length > 0 ? args[0] : "src/main/resources/icons";
        String outputFile = args.length > 1 ? args[1] : inputDir + "/app-icon.ico";

        // Load PNG images at different sizes
        int[] sizes = {16, 32, 48, 64, 128, 256};
        BufferedImage[] images = new BufferedImage[sizes.length];

        // Load the 256x256 source and scale to all sizes
        File sourceFile = new File(inputDir + "/app-icon-256.png");
        BufferedImage source;
        if (sourceFile.exists()) {
            source = ImageIO.read(sourceFile);
        } else {
            // Generate the icon first
            IconGenerator.main(new String[]{inputDir});
            source = ImageIO.read(sourceFile);
        }

        for (int i = 0; i < sizes.length; i++) {
            int s = sizes[i];
            BufferedImage scaled = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
            scaled.getGraphics().drawImage(source.getScaledInstance(s, s, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
            images[i] = scaled;
        }

        // Write ICO file
        writeIco(images, sizes, outputFile);
        System.out.println("ICO file generated: " + outputFile);
    }

    private static void writeIco(BufferedImage[] images, int[] sizes, String outputFile) throws IOException {
        ByteArrayOutputStream[] pngData = new ByteArrayOutputStream[images.length];
        int[] dataSizes = new int[images.length];

        for (int i = 0; i < images.length; i++) {
            pngData[i] = new ByteArrayOutputStream();
            ImageIO.write(images[i], "PNG", pngData[i]);
            dataSizes[i] = pngData[i].size();
        }

        int imageCount = images.length;
        int headerSize = 6;
        int dirEntrySize = 16;
        int dirSize = dirEntrySize * imageCount;
        int dataOffset = headerSize + dirSize;

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            ByteBuffer buf;

            // ICO Header
            buf = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN);
            buf.putShort((short) 0);      // Reserved
            buf.putShort((short) 1);      // Type: ICO
            buf.putShort((short) imageCount);  // Number of images
            fos.write(buf.array());

            // Directory entries
            int offset = dataOffset;
            for (int i = 0; i < imageCount; i++) {
                buf = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
                int s = sizes[i];
                buf.put((byte) (s >= 256 ? 0 : s));  // Width (0 = 256)
                buf.put((byte) (s >= 256 ? 0 : s));  // Height (0 = 256)
                buf.put((byte) 0);      // Color palette
                buf.put((byte) 0);      // Reserved
                buf.putShort((short) 1);    // Color planes
                buf.putShort((short) 32);   // Bits per pixel
                buf.putInt(dataSizes[i]);   // Image data size
                buf.putInt(offset);         // Image data offset
                fos.write(buf.array());
                offset += dataSizes[i];
            }

            // Image data (PNG format)
            for (int i = 0; i < imageCount; i++) {
                fos.write(pngData[i].toByteArray());
            }
        }
    }
}
