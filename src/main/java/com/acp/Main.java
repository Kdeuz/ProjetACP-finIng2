package com.acp;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.math3.linear.RealMatrix;

public class Main {
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            /*
             * Il faut ajouter l'implémentation de la fonction qui permet d'afficher toutes les images disponibles dans le dossier et laisser le choix à l'utilisateur de choisir une image.
             */
            // 1. Charger l'image
            String sourcePath = "src/main/resources/images/train/";
            List<String> imageNames = ImageUtils.listImg(sourcePath + "originals/");
            System.out.println("Liste des images disponibles : ");
            for (int i = 0; i < imageNames.size(); i++) {
                System.out.println((i + 1) + ": " + imageNames.get(i));
            }
            System.out.print("Choisissez une image (1-" + imageNames.size() + "): ");
            int imageIndex = scanner.nextInt() - 1;
            scanner.nextLine(); // Consommer le caractère de nouvelle ligne restant
            if (imageIndex < 0 || imageIndex >= imageNames.size()) {
                System.out.println("Index d'image invalide.");
                return;
            }
            String imageName = imageNames.get(imageIndex);


            String originalPath = sourcePath + "originals/" + imageName;
            String targetPath = sourcePath + "noisy/";
            BufferedImage original = ImageUtils.loadImage(originalPath);
            
            
            // 2. Ajouter du bruit
            double[] sigmas = {10,20,30};
            List<BufferedImage> noisyImages = ImageUtils.generateNoisyImages(original, sigmas, originalPath,targetPath);
            int imageCounter = 1; // Initialiser un compteur pour les images sauvegardées
            for (BufferedImage noisy : noisyImages) {
                // 3. Extraire les patchs
                List<ImagePatch> patches = PatchUtils.extractPatches(noisy, 8, 4);
                
                // 4. Vectoriser les patchs
                List<VectorizedPatch> vectorPatches = new ArrayList<>();
                vectorPatches.addAll(PatchUtils.vectorPatches(patches));
                
                // 5. ACP et débruitage
                List<double[]> vectors = new ArrayList<>();
                for (VectorizedPatch vp : vectorPatches) vectors.add(vp.getVector());
                
                double[] mean = PCAProcessor.computeMean(vectors);
                List<double[]> centered = PCAProcessor.centerData(vectors, mean);
                RealMatrix projection = PCAProcessor.performPCA(centered);

                // 6. Choisir le type de seuillage
                System.out.println("Choisissez le type de seuillage (hard/soft) : ");
                String thresholdType = scanner.nextLine().trim();
                System.out.println("Entrez la valeur du seuil : ");
                double threshold = scanner.nextDouble();
                scanner.nextLine(); // Consommer le caractère de nouvelle ligne restant

                // Projeter sur les composantes principales
                List<double[]> projected = PCAProcessor.project(centered, projection);

                // Appliquer le seuillage sur les coefficients projetés
                List<double[]> thresholdedProjected = applyThresholding(projected, thresholdType, threshold);

                // Reprojeter dans l’espace original (reconstruction inverse)
                List<double[]> reconstructedVectors = PCAProcessor.reconstruct(thresholdedProjected, projection, mean);
                            
                // Reconstruire les patchs vectorisés
                List<VectorizedPatch> thresholdedPatches = new ArrayList<>();
                for (int i = 0; i < vectorPatches.size(); i++) {
                    thresholdedPatches.add(new VectorizedPatch(reconstructedVectors.get(i), 
                                                            vectorPatches.get(i).getX(), 
                                                            vectorPatches.get(i).getY(), 
                                                            vectorPatches.get(i).getSize()));
                }
                
                // 7. Reconstruire l'image
                BufferedImage reconstructed = PatchUtils.reconstruct(thresholdedPatches, 
                    noisy.getWidth(), noisy.getHeight());
                String path = sourcePath + "denoised/" + thresholdType + "/" + imageCounter + "_" + imageName;
                System.out.println("Saving image to: " + path);
                ImageUtils.saveImage(reconstructed, path);

                imageCounter++; // Incrémenter le compteur après chaque sauvegarde
            
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to save image: " + e.getMessage());
        }
            
    }

    private static List<double[]> applyThresholding(List<double[]> vectors, String thresholdType, double threshold) {
        List<double[]> thresholdedVectors = new ArrayList<>();
        for (double[] vector : vectors) {
            double[] thresholdedVector = new double[vector.length];
            for (int i = 0; i < vector.length; i++) {
                if ("hard".equalsIgnoreCase(thresholdType)) {
                    thresholdedVector[i] = Denoiser.hardThreshold(vector[i], threshold);
                } else if ("soft".equalsIgnoreCase(thresholdType)) {
                    thresholdedVector[i] = Denoiser.softThreshold(vector[i], threshold);
                } else {
                    throw new IllegalArgumentException("Invalid threshold type: " + thresholdType);
                }
            }
            thresholdedVectors.add(thresholdedVector);
        }
        return thresholdedVectors;
    }
}