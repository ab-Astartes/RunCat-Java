package com.runcat.animation;

import java.awt.Image;
import java.util.List;
import java.util.function.Supplier;

/**
 * Independent animation cursor for tray and desktop pet playback.
 */
public class AnimationPlayback {

    private final Supplier<List<Image>> frameSupplier;
    private int currentIndex;
    private PetAnimationState state = PetAnimationState.RUN;

    public AnimationPlayback(Supplier<List<Image>> frameSupplier) {
        this.frameSupplier = frameSupplier;
    }

    public synchronized Image nextFrame() {
        List<Image> frames = frameSupplier.get();
        if (frames == null || frames.isEmpty()) {
            return null;
        }
        if (currentIndex >= frames.size()) {
            currentIndex = 0;
        }
        Image image = frames.get(currentIndex);
        currentIndex = (currentIndex + 1) % frames.size();
        return image;
    }

    public synchronized Image peekFrame() {
        List<Image> frames = frameSupplier.get();
        if (frames == null || frames.isEmpty()) {
            return null;
        }
        if (currentIndex >= frames.size()) {
            currentIndex = 0;
        }
        return frames.get(currentIndex);
    }

    public synchronized void reset() {
        currentIndex = 0;
    }

    public synchronized int getCurrentIndex() {
        return currentIndex;
    }

    public synchronized PetAnimationState getState() {
        return state;
    }

    public synchronized void setState(PetAnimationState state) {
        this.state = state == null ? PetAnimationState.RUN : state;
    }
}
