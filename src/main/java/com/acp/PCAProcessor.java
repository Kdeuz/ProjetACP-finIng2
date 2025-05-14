package com.acp;

import java.util.List;

import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 * Classe pour le traitement ACP
 */
public class PCAProcessor {
    
    /**
     * Calcule la moyenne des vecteurs
     * @param vectors Liste de vecteurs
     * @return Vecteur moyen
     */
    public static double[] computeMean(List<double[]> vectors) {
        int dim = vectors.get(0).length;
        double[] mean = new double[dim];
        
        for (double[] v : vectors) {
            for (int i = 0; i < dim; i++) {
                mean[i] += v[i];
            }
        }
        
        for (int i = 0; i < dim; i++) {
            mean[i] /= vectors.size();
        }
        return mean;
    }
    
    /**
     * Effectue l'ACP sur les vecteurs
     * @param vectors Liste de vecteurs
     * @return Matrice de projection
     */
    public static RealMatrix performPCA(List<double[]> vectors) {
        RealMatrix covMatrix = computeCovariance(vectors);
        EigenDecomposition eig = new EigenDecomposition(covMatrix);
        return eig.getV();
    }
    
    private static RealMatrix computeCovariance(List<double[]> vectors) {
        double[] mean = computeMean(vectors);
        RealMatrix covMatrix = MatrixUtils.createRealMatrix(mean.length, mean.length);
        
        for (double[] v : vectors) {
            RealVector centered = MatrixUtils.createRealVector(v).subtract(MatrixUtils.createRealVector(mean));
            covMatrix = covMatrix.add(centered.outerProduct(centered));
        }
        
        return covMatrix.scalarMultiply(1.0 / vectors.size());
    }

    /**
     * Centre les données avec le vecteur moyen
     */
    public static List<double[]> centerData(List<double[]> vectors, double[] mean) {
        return vectors.stream()
            .map(v -> {
                double[] centered = new double[v.length];
                for (int i = 0; i < v.length; i++) {
                    centered[i] = v[i] - mean[i];
                }
                return centered;
            })
            .toList();
    }

    /**
     * Projette les vecteurs centrés dans l’espace des composantes principales
     */
    public static List<double[]> project(List<double[]> centeredVectors, RealMatrix projectionMatrix) {
        return centeredVectors.stream()
            .map(v -> projectionMatrix.preMultiply(v))
            .toList();
    }

    /**
     * Reconstitue les vecteurs d’origine à partir des coefficients ACP seuillés
     */
    public static List<double[]> reconstruct(List<double[]> projectedVectors, RealMatrix projectionMatrix, double[] mean) {
        RealMatrix inverse = projectionMatrix.transpose();
        return projectedVectors.stream()
            .map(v -> {
                double[] reconstructed = inverse.preMultiply(v);
                for (int i = 0; i < reconstructed.length; i++) {
                    reconstructed[i] += mean[i];
                }
                return reconstructed;
            })
            .toList();
    }

}