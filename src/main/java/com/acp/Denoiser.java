package com.acp;

/**
 * Classe pour les opérations de seuillage
 */
public class Denoiser {
    
    /**
     * Seuillage dur
     * @param value Valeur à seuiller
     * @param threshold Seuil
     * @return Valeur seuillée
     */
    public static double hardThreshold(double value, double threshold) {
        return Math.abs(value) > threshold ? value : 0;
    }
    
    /**
     * Seuillage doux
     * @param value Valeur à seuiller
     * @param threshold Seuil
     * @return Valeur seuillée
     */
    public static double softThreshold(double value, double threshold) {
        if (value > threshold) return value - threshold;
        if (value < -threshold) return value + threshold;
        return 0;
    }
    
    /**
     * Calcule le seuil VisuShrink
     * @param sigma Écart-type du bruit
     * @param n Nombre de pixels
     * @return Seuil calculé
     */
    public static double visuShrink(double sigma, int n) {
        return sigma * Math.sqrt(2 * Math.log(n));
    }
    
    /**
     * Calcule le seuil BayesShrink
     * @param sigmaBruit Variance du bruit
     * @param sigmaSignal Variance du signal
     * @return Seuil calculé
     */
    public static double bayesShrink(double sigmaBruit, double sigmaSignal) {
        return (sigmaBruit * sigmaBruit) / sigmaSignal;
    }
}