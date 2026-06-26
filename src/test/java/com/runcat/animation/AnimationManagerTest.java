package com.runcat.animation;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnimationManagerTest {

    @Test
    void playbacksAdvanceIndependently() {
        BufferedImage img1 = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        BufferedImage img2 = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        AnimationPlayback tray = new AnimationPlayback(() -> List.of(img1, img2));
        AnimationPlayback pet = new AnimationPlayback(() -> List.of(img1, img2));

        tray.nextFrame();

        assertEquals(1, tray.getCurrentIndex());
        assertEquals(0, pet.getCurrentIndex());
    }
}
