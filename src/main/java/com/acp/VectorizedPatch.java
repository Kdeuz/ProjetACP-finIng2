package com.acp;

/**
 * Représente un patch vectorisé avec sa position
 */
public class VectorizedPatch {
    private final double[] vector;
    private final int x;
    private final int y;
    private final int size;
    
    public VectorizedPatch(double[] vector, int x, int y, int size) {
        this.vector = vector;
        this.x = x;
        this.y = y;
        this.size = size;
    }

    public double[] getVector() {
        return this.vector;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getSize(){
        return this.size;
    }


}
