package com.acp;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

public class ImageUtils {
    
    /**
     * Charge une image en niveaux de gris
     * @param path chemin de l'image
     * @return Image en niveaux de gris
     * @throws IOException Si erreur de lecture
     */
    public static BufferedImage loadImage(String path) throws IOException {
        BufferedImage img = ImageIO.read(new File(path));
        BufferedImage gray = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        gray.getGraphics().drawImage(img, 0, 0, null);
        return gray;
    }

    /**
     * Sauvegarde une image
     * @param img Image à sauvegarder
     * @param path Chemin de destination
     * @throws IOException Si errreur d'écriture
     */
    public static void saveImage(BufferedImage img, String path) throws IOException {
        ImageIO.write(img, "png", new File(path));
    }

    /**
     * Renvoie la liste des noms des images
     * @param pathImg Chemin du dossier contenant les images
     * @return Liste des noms des images
     * @throws IOException Si erreur de lecture
     */
    public static List<String> listImg(String pathImg) throws IOException {
        File folder = new File(pathImg);
        List<String> imageNames = new ArrayList<>();
        if (folder.exists() && folder.isDirectory()) {
            for (File file : folder.listFiles()) {
                if (file.isFile()) {
                    imageNames.add(file.getName());
                }
            }
        }
        return imageNames;
    }

    /**
     * Ajoute un bruit gaussien à l'image
     * @param original Image originale
     * @param sigma Ecart-Type du bruit
     * @return Image Bruitée
     */
    public static BufferedImage noising(BufferedImage original, double sigma) {
        BufferedImage noisy = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Random rand = new Random();

        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                int gray = original.getRaster().getSample(x, y, 0);
                double noise = rand.nextGaussian() * sigma;
                int noisyVal = Math.max(0, Math.min(255, (int) Math.round(gray + noise)));
                noisy.getRaster().setSample(x, y, 0, noisyVal);
            }
        }
        return noisy;
    }

    /*public ArrayList<BufferedImage> listNoisyImages(BufferedImage original, double[] sigmas) {
        ArrayList<BufferedImage> listImgNoisy = new ArrayList<>();
        for (double sigma: sigmas) {
            listImgNoisy.add(noising(original, sigma));
        }
        return listImgNoisy;
    }
    */

    /**
     * Génère une base d'images bruitées à partir d'une image originale
     * @param original Image originale
     * @param sigmas Tableau des valeurs d'écart-type
     * @param outputDir Répertoire de sortie pour les images bruitées
     * @throws IOException Si erreur d'écriture
     */
    public static ArrayList<BufferedImage> generateNoisyImages(BufferedImage original, double[] sigmas, String originalPath, String outputDir) throws IOException {
        ArrayList<BufferedImage> listImages = new ArrayList<>();
        String originalName = new File(originalPath).getName();
        int index = originalName.lastIndexOf('.');
        if (index > 0) {
            originalName = originalName.substring(0, index); //supprimer l'extension
        }

        for (double sigma: sigmas) {
            BufferedImage noisyImage = noising(original, sigma);
            String outputPath = outputDir + "/" + originalName + "_sigma_" + (int) sigma + ".png";
            saveImage(noisyImage, outputPath);
            listImages.add(original);
        }
        return listImages;
    }

        /**
    * Divise une image en quatre parties égales.
    * @param image L'image à diviser.
    * @return Un tableau contenant les quatre sous-images :
    *         - [0] Partie supérieure gauche
    *         - [1] Partie supérieure droite
    *         - [2] Partie inférieure gauche
    *         - [3] Partie inférieure droite
    */

    public static BufferedImage[] divisionEnQuatre(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Calculer les dimensions des sous-images
        int halfWidth = width / 2;
        int halfHeight = height / 2;

        // Ajuster les dimensions pour inclure les pixels restants
        int widthRemainder = width % 2;
        int heightRemainder = height % 2;

        BufferedImage[] parts = new BufferedImage[4];

        parts[0] = image.getSubimage(0, 0, halfWidth, halfHeight); // Partie supérieure gauche
        parts[1] = image.getSubimage(halfWidth, 0, halfWidth + widthRemainder, halfHeight); // Partie supérieure droite
        parts[2] = image.getSubimage(0, halfHeight, halfWidth, halfHeight + heightRemainder); // Partie inférieure gauche
        parts[3] = image.getSubimage(halfWidth, halfHeight, halfWidth + widthRemainder, halfHeight + heightRemainder); // Partie inférieure droite

        return parts;
    }

    /**
     * Fusionne quatre images en une seule image.
    * Les images doivent être disposées comme suit :
    * - [0] Partie supérieure gauche
    * - [1] Partie supérieure droite
    * - [2] Partie inférieure gauche
    * - [3] Partie inférieure droite
    * @param images Tableau contenant les quatre images à fusionner.
    * @return Une image fusionnée composée des quatre images d'entrée.
    */

    public static BufferedImage fusionnerImages(BufferedImage[] images) {
        // Calculer les dimensions totales de l'image fusionnée
        int width = images[0].getWidth() + images[1].getWidth();
        int height = images[0].getHeight() + images[2].getHeight();

        // Créer une nouvelle image pour la fusion
        BufferedImage mergedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        // Placer les images dans la nouvelle image
        mergedImage.getGraphics().drawImage(images[0], 0, 0, null); // Partie supérieure gauche
        mergedImage.getGraphics().drawImage(images[1], images[0].getWidth(), 0, null); // Partie supérieure droite
        mergedImage.getGraphics().drawImage(images[2], 0, images[0].getHeight(), null); // Partie inférieure gauche
        mergedImage.getGraphics().drawImage(images[3], images[0].getWidth(), images[0].getHeight(), null); // Partie inférieure droite

        return mergedImage;
    }


}