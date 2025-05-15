package com.acp;

import java.awt.image.BufferedImage;

public class MseCalculator {

    /**
     * Calcule la MSE entre deux images
     * @param img1 Image 1
     * @param img2 Image 2
     * @return MSE
     */
    public static double calculateMSE(BufferedImage img1, BufferedImage img2) {
        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
            throw new IllegalArgumentException("Les images doivent avoir la même taille");
        }

        double mse = 0.0;
        for (int y = 0; y < img1.getHeight(); y++) {
            for (int x = 0; x < img1.getWidth(); x++) {
                int pixel1 = img1.getRGB(x, y);
                int pixel2 = img2.getRGB(x, y);
                int gray1 = (pixel1 >> 16) & 0xff; // Noir et blanc: R=G=B
                int gray2 = (pixel2 >> 16) & 0xff; 

                double diff = gray1 - gray2;

                mse += diff * diff;
            }
        }
        return mse / (img1.getWidth() * img1.getHeight());
    }
    /**
     * Calcule le PSNR entre deux images
     * @param img1 Image 1
     * @param img2 Image 2
     * @return PSNR
     */
    public static double calculatePSNR(BufferedImage img1, BufferedImage img2) {
        double mse = calculateMSE(img1, img2);
        if (mse == 0) {
            return Double.POSITIVE_INFINITY;
        }
        return 10 * Math.log10(255*255 / mse);
    }
}