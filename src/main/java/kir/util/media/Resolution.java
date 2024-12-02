package kir.util.media;

import lombok.Getter;

@Getter
public enum Resolution {
    ORIGIN(-1, -1),
    FULL_HD(1920, 1080),
    HD(1280, 720);

    private final int width;
    private final int height;

    Resolution(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
