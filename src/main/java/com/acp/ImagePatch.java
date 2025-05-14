package com.acp;

/**
 * Représente un patch d'image avec sa position
 */
public class ImagePatch {
    private final int[][] data;
    private final int x;
    private final int y;
    
    public ImagePatch(int[][] data, int x, int y) {
        this.data = data;
        this.x = x;
        this.y = y;
    }

    public int[][] getData() {
        return this.data;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

}
