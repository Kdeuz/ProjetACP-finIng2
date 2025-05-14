package com.acp;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe pour l'extraction et la reconstruction de patchs
 */
public class PatchUtils {
    
    /**
     * Extrait des patchs d'une image
     * @param img Image source
     * @param patchSize Taille des patchs
     * @param stride Pas de déplacement
     * @return Liste de patchs
     */
    public static List<ImagePatch> extractPatches(BufferedImage img, int patchSize, int stride) {
        List<ImagePatch> patches = new ArrayList<>();
        
        for (int y = 0; y <= img.getHeight() - patchSize; y += stride) {
            for (int x = 0; x <= img.getWidth() - patchSize; x += stride) {
                int[][] data = new int[patchSize][patchSize];
                for (int j = 0; j < patchSize; j++) {
                    for (int i = 0; i < patchSize; i++) {
                        data[j][i] = img.getRaster().getSample(x + i, y + j, 0);
                    }
                }
                patches.add(new ImagePatch(data, x, y));
            }
        }
        return patches;
    }
    
    /**
     * Reconstruit une image à partir de patchs
     * @param patches Liste de patchs
     * @param width Largeur de l'image
     * @param height Hauteur de l'image
     * @return Image reconstruite
     */
    public static BufferedImage reconstruct(List<VectorizedPatch> patches, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        int[][] sum = new int[height][width];
        int[][] count = new int[height][width];
        
        for (VectorizedPatch vp : patches) {
            int idx = 0;
            for (int j = 0; j < vp.getSize(); j++) {
                for (int i = 0; i < vp.getSize(); i++) {
                    int px = vp.getX() + i;
                    int py = vp.getY() + j;
                    if (px < width && py < height) {
                        sum[py][px] += vp.getVector()[idx];
                        count[py][px]++;
                    }
                    idx++;
                }
            }
        }
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (count[y][x] > 0) {
                    int value = Math.max(0, Math.min(255, sum[y][x] / count[y][x]));
                    img.getRaster().setSample(x, y, 0, value);
                }
            }
        }
        return img;
    }

    public static ArrayList<VectorizedPatch> vectorPatches(List<ImagePatch> patches) {
        ArrayList<VectorizedPatch> vectorPatches = new ArrayList<>();
        for (ImagePatch patch : patches) {
            double[] vector = new double[8 * 8];
            int idx = 0;
            for (int[] row : patch.getData()) {
                for (int val : row) {
                    vector[idx++] = val;
                }
            }
            vectorPatches.add(new VectorizedPatch(vector, patch.getX(), patch.getY(), 8));
        }
        return vectorPatches;
    }
}