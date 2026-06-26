package com.runcat.ui;

import com.runcat.animation.AnimationManager;
import com.runcat.config.AppConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomAnimationDialogSupportTest {

    @Test
    void sortsPngFramesNaturally() throws Exception {
        Path dir = Files.createTempDirectory("runcat-frames");
        Files.createFile(dir.resolve("frame_10.png"));
        Files.createFile(dir.resolve("frame_2.png"));
        Files.createFile(dir.resolve("frame_1.png"));

        AnimationManager manager = new AnimationManager(new AppConfig());
        List<String> names = manager.listAnimationFrames(dir).stream()
                .map(path -> path.getFileName().toString())
                .toList();

        assertEquals(List.of("frame_1.png", "frame_2.png", "frame_10.png"), names);
    }
}
