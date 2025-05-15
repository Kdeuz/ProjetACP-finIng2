package com.acp;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.math3.linear.RealMatrix;

public class Main {
    public static void main(String[] args) {
        String sourcePath = "src/main/resources/images/train/";
        List<String> imageNames;
        String imageName = null;
        String thresholdType = null;
        String seuilMode = "manuel";
        double[] sigmas = {10, 20, 30};
        double[] thresholds = {20, 20, 20}; // Valeurs par défaut, à adapter

        try {
            imageNames = ImageUtils.listImg(sourcePath + "originals/");
            if (args.length >= 2) {
                // Mode automatique (appelé par l'IHM)
                imageName = args[0];
                thresholdType = args[1];
                if (args.length >= 3) {
                    seuilMode = args[2];
                }

                if (args.length >= 6) {
                    for (int i = 0; i < 3; i++) {
                        thresholds[i] = Double.parseDouble(args[3 + i]);
                    }
                }
                if (args.length >= 9) {
                    for (int i = 0; i < 3; i++) {
                        sigmas[i] = Double.parseDouble(args[6 + i]);
                    }
                }
            } else {
                // Mode interactif console
                try (Scanner scanner = new Scanner(System.in)) {
                    System.out.println("Liste des images disponibles : ");
                    for (int i = 0; i < imageNames.size(); i++) {
                        System.out.println((i + 1) + ": " + imageNames.get(i));
                    }
                    System.out.print("Choisissez une image (1-" + imageNames.size() + "): ");
                    int imageIndex = scanner.nextInt() - 1;
                    scanner.nextLine();
                    if (imageIndex < 0 || imageIndex >= imageNames.size()) {
                        System.out.println("Index d'image invalide.");
                        return;
                    }
                    imageName = imageNames.get(imageIndex);

                    System.out.print("Type de seuillage (hard/soft): ");
                    thresholdType = scanner.nextLine().trim();

                    System.out.print("Mode de seuil (manuel/visuShrink/bayesShrink): ");
                    seuilMode = scanner.nextLine().trim();

                    for (int i = 0; i < 3; i++) {
                        System.out.print("Seuil pour sigma " + (int)sigmas[i] + " : ");
                        thresholds[i] = scanner.nextDouble();
                        scanner.nextLine();
                    }
                }
            }

            String originalPath = sourcePath + "originals/" + imageName;
            File originalFile = new File(originalPath);
            if (!originalFile.exists()) {
                System.err.println("Image originale introuvable : " + originalPath);
                System.exit(1);
            }
            BufferedImage original = ImageUtils.loadImage(originalPath);

            // 2. Ajouter du bruit
            String targetPath = sourcePath + "noisy/";
            List<BufferedImage> noisyImages = ImageUtils.generateNoisyImages(original, sigmas, originalPath, targetPath);
            int imageCounter = 0;
            for (BufferedImage noisy : noisyImages) {
                // 3. Extraire les patchs
                List<ImagePatch> patches = PatchUtils.extractPatches(noisy, 8, 4);

                // 4. Vectoriser les patchs
                List<VectorizedPatch> vectorPatches = PatchUtils.vectorPatches(patches);

                // 5. ACP et débruitage
                List<double[]> vectors = new ArrayList<>();
                for (VectorizedPatch vp : vectorPatches) vectors.add(vp.getVector());

                double[] mean = PCAProcessor.computeMean(vectors);
                List<double[]> centered = PCAProcessor.centerData(vectors, mean);
                RealMatrix projection = PCAProcessor.performPCA(centered);

                // 6. Projeter sur les composantes principales
                List<double[]> projected = PCAProcessor.project(centered, projection);

                // 7. Appliquer le seuillage sur les coefficients projetés
                double threshold;
                if ("visuShrink".equalsIgnoreCase(seuilMode)) {
                    threshold = Denoiser.visuShrink(sigmas[imageCounter], projected.get(0).length);
                } else if ("bayesShrink".equalsIgnoreCase(seuilMode)) {
                    double sigmaBruit = sigmas[imageCounter];
                    double sigmaSignal = estimateSignalStd(projected);
                    threshold = Denoiser.bayesShrink(sigmaBruit, sigmaSignal);
                } else {
                    threshold = thresholds[imageCounter];
                }
                List<double[]> thresholdedProjected = applyThresholding(projected, thresholdType, threshold);

                // 8. Reprojeter dans l’espace original (reconstruction inverse)
                List<double[]> reconstructedVectors = PCAProcessor.reconstruct(thresholdedProjected, projection, mean);

                // 9. Reconstruire les patchs vectorisés
                List<VectorizedPatch> thresholdedPatches = new ArrayList<>();
                for (int i = 0; i < vectorPatches.size(); i++) {
                    thresholdedPatches.add(new VectorizedPatch(reconstructedVectors.get(i),
                            vectorPatches.get(i).getX(),
                            vectorPatches.get(i).getY(),
                            vectorPatches.get(i).getSize()));
                }

                // 10. Reconstruire l'image
                BufferedImage reconstructed = PatchUtils.reconstruct(thresholdedPatches,
                        noisy.getWidth(), noisy.getHeight());
                String baseName = imageName.replaceAll("\\.[^.]+$", "");
                String path = sourcePath + "denoised/" + thresholdType + "/" + baseName + "_sigma_" + (int)sigmas[imageCounter] + ".png";
                new File(path).getParentFile().mkdirs();
                System.out.println("Saving image to: " + path);
                ImageUtils.saveImage(reconstructed, path);

                // 11. Calculer la MSE et le PSNR
                double Mse= MseCalculator.calculateMSE(original, reconstructed);
                System.out.println("MSE: " + Mse);
                double PSNR= MseCalculator.calculatePSNR(original,reconstructed);
                System.out.println("PSNR: " + PSNR);

                imageCounter++;
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

    private static double estimateSignalStd(List<double[]> projected) {
        // Calcule la variance moyenne sur tous les coefficients
        int n = projected.size();
        int dim = projected.get(0).length;
        double sum = 0;
        double sumSq = 0;
        int count = 0;
        for (double[] vec : projected) {
            for (double v : vec) {
                sum += v;
                sumSq += v * v;
                count++;
            }
        }
        double mean = sum / count;
        double variance = (sumSq / count) - (mean * mean);
        return Math.sqrt(Math.max(variance, 1e-8));
    }
}