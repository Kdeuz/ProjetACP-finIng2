package com.acp;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import org.apache.commons.math3.linear.RealMatrix;

public class MainIHMAcp extends Application {

    private ImageView originalImageView = new ImageView();
    private ImageView noisyImageView = new ImageView();
    private ImageView denoisedImageView = new ImageView();

    private File selectedFile;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Débruitage d'Images par ACP");

        // Layout principal
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        // Haut : Boutons de contrôle
        HBox controls = new HBox(10);
        controls.setPadding(new Insets(10));
        Button loadButton = new Button("Charger une image");
        Button denoiseButton = new Button("Débruiter");
        controls.getChildren().addAll(loadButton, denoiseButton);

        // Centre : Affichage des images
        GridPane imageGrid = new GridPane();
        imageGrid.setHgap(10);
        imageGrid.setVgap(10);
        imageGrid.add(new Label("Image originale"), 0, 0);
        imageGrid.add(originalImageView, 0, 1);
        imageGrid.add(new Label("Image bruitée"), 1, 0);
        imageGrid.add(noisyImageView, 1, 1);
        imageGrid.add(new Label("Image débruitée"), 2, 0);
        imageGrid.add(denoisedImageView, 2, 1);

        // Bas : Paramètres de seuillage
        VBox thresholdingBox = new VBox(10);
        thresholdingBox.setPadding(new Insets(10));
        ComboBox<String> thresholdTypeCombo = new ComboBox<>();
        thresholdTypeCombo.getItems().addAll("Hard", "Soft");
        thresholdTypeCombo.setValue("Hard");
        TextField thresholdValueField = new TextField();
        thresholdValueField.setPromptText("Valeur du seuil");
        thresholdingBox.getChildren().addAll(new Label("Type de seuillage"), thresholdTypeCombo, new Label("Valeur du seuil"), thresholdValueField);

        root.setTop(controls);
        root.setCenter(imageGrid);
        root.setBottom(thresholdingBox);

        // Actions des boutons
        loadButton.setOnAction(e -> loadImage(primaryStage));
        denoiseButton.setOnAction(e -> {
            String thresholdType = thresholdTypeCombo.getValue();
            double thresholdValue;
            try {
                thresholdValue = Double.parseDouble(thresholdValueField.getText());
            } catch (NumberFormatException ex) {
                showAlert("Erreur", "Veuillez entrer une valeur numérique pour le seuil.");
                return;
            }
            denoiseImage(thresholdType, thresholdValue);
        });

        // Afficher la scène
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void loadImage(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner une image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.webp")
        );
        selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            try {
                BufferedImage original = ImageUtils.loadImage(selectedFile.getAbsolutePath());
                originalImageView.setImage(convertToFxImage(original));
            } catch (IOException e) {
                showAlert("Erreur", "Impossible de charger l'image.");
            }
        }
    }

    private void denoiseImage(String thresholdType, double thresholdValue) {
        if (selectedFile == null) {
            showAlert("Erreur", "Veuillez d'abord charger une image.");
            return;
        }

        try {
            // Charger l'image originale
            BufferedImage original = ImageUtils.loadImage(selectedFile.getAbsolutePath());

            // Ajouter du bruit
            double sigma = 20; // Exemple d'écart-type
            BufferedImage noisy = ImageUtils.noising(original, sigma);
            noisyImageView.setImage(convertToFxImage(noisy));

            // Débruiter
            List<ImagePatch> patches = PatchUtils.extractPatches(noisy, 8, 4);
            List<VectorizedPatch> vectorPatches = PatchUtils.vectorPatches(patches);
            List<double[]> vectors = vectorPatches.stream().map(VectorizedPatch::getVector).toList();

            double[] mean = PCAProcessor.computeMean(vectors);
            List<double[]> centered = PCAProcessor.centerData(vectors, mean);
            RealMatrix projection = PCAProcessor.performPCA(centered);
            List<double[]> projected = PCAProcessor.project(centered, projection);
            List<double[]> thresholded = applyThresholding(projected, thresholdType, thresholdValue);
            List<double[]> reconstructed = PCAProcessor.reconstruct(thresholded, projection, mean);

            List<VectorizedPatch> reconstructedPatches = new ArrayList<>();
            for (int i = 0; i < vectorPatches.size(); i++) {
                reconstructedPatches.add(new VectorizedPatch(reconstructed.get(i),
                        vectorPatches.get(i).getX(),
                        vectorPatches.get(i).getY(),
                        vectorPatches.get(i).getSize()));
            }

            BufferedImage denoised = PatchUtils.reconstruct(reconstructedPatches, noisy.getWidth(), noisy.getHeight());
            denoisedImageView.setImage(convertToFxImage(denoised));

        } catch (IOException e) {
            showAlert("Erreur", "Une erreur est survenue lors du débruitage.");
        }
    }

    private Image convertToFxImage(BufferedImage bufferedImage) {
        return javafx.embed.swing.SwingFXUtils.toFXImage(bufferedImage, null);
    }

    private List<double[]> applyThresholding(List<double[]> vectors, String thresholdType, double threshold) {
        return vectors.stream().map(vector -> {
            double[] thresholded = new double[vector.length];
            for (int i = 0; i < vector.length; i++) {
                if ("Hard".equalsIgnoreCase(thresholdType)) {
                    thresholded[i] = Denoiser.hardThreshold(vector[i], threshold);
                } else {
                    thresholded[i] = Denoiser.softThreshold(vector[i], threshold);
                }
            }
            return thresholded;
        }).toList();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
