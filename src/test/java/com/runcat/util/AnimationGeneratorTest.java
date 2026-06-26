package com.runcat.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationGeneratorTest {

    @Test
    void catTailMotionSwingsVisibly() {
        AnimationGenerator.TailMotion frame0 = AnimationGenerator.catTailMotion64(0);
        AnimationGenerator.TailMotion frame2 = AnimationGenerator.catTailMotion64(2);

        // Tail should have visible movement in either axis
        int horizontalDelta = Math.abs(frame0.tipX() - frame2.tipX());
        int verticalDelta = Math.abs(frame0.tipY() - frame2.tipY());
        int totalDelta = horizontalDelta + verticalDelta;

        assertTrue(totalDelta >= 4, "tail should visibly swing (total delta >= 4, got h=" + horizontalDelta + " v=" + verticalDelta + ")");
        assertTrue(horizontalDelta <= 14, "tail should not swing wildly sideways (h delta <= 14)");
    }
}
