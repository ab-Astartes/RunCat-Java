package com.runcat.ui;

import org.junit.jupiter.api.Test;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DesktopPetWindowTest {

    @Test
    void hitShapeCoversWindowCenterAndCorners() {
        Shape shape = DesktopPetWindow.createHitShape(120, 132);
        Rectangle2D bounds = shape.getBounds2D();

        assertTrue(bounds.contains(60, 66));
        assertTrue(bounds.contains(8, 8));
        assertTrue(bounds.contains(112, 124));
    }
}
