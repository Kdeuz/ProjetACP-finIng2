package com.acp;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class MainIHMAcp extends Application {

    private static final int IMG_SIZE = 400;

    private ImageView originalImageView = new ImageView();
    private List<ImageView> noisyImageViews = List.of(new ImageView(), new ImageView(), new ImageView());
    private List<ImageView> denoisedImageViews = List.of(new ImageView(), new ImageView(), new ImageView());

    private ComboBox<String> imageCombo;
    private ComboBox<String> thresholdTypeCombo;
    private ComboBox<String> seuilModeCombo;
    private ComboBox<String> acpModeCombo;
    private TextField[] sigmaFields = new TextField[3];
    private TextField[] seuilFields = new TextField[3];

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Débruitage d'Images par ACP (Affichage)");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        HBox controls = new HBox(15);
        controls.setPadding(new Insets(10));
        controls.getChildren().add(new Label("Image :"));
        imageCombo = new ComboBox<>();
        controls.getChildren().add(imageCombo);

        controls.getChildren().add(new Label("Type de seuillage :"));
        thresholdTypeCombo = new ComboBox<>();
        thresholdTypeCombo.getItems().addAll("hard", "soft");
        thresholdTypeCombo.setValue("hard");
        controls.getChildren().add(thresholdTypeCombo);

        seuilModeCombo = new ComboBox<>();
        seuilModeCombo.getItems().addAll("manuel", "visuShrink", "bayesShrink");
        seuilModeCombo.setValue("manuel");
        controls.getChildren().add(new Label("Mode seuil :"));
        controls.getChildren().add(seuilModeCombo);

        acpModeCombo = new ComboBox<>();
        acpModeCombo.getItems().addAll("global", "local");
        acpModeCombo.setValue("global");
        controls.getChildren().add(new Label("Mode ACP :"));
        controls.getChildren().add(acpModeCombo);

        controls.getChildren().add(new Label("Sigmas :"));
        for (int i = 0; i < 3; i++) {
            sigmaFields[i] = new TextField(String.valueOf(10 + i * 10));
            sigmaFields[i].setPrefWidth(50);
            controls.getChildren().add(sigmaFields[i]);
        }

        controls.getChildren().add(new Label("Seuils :"));
        for (int i = 0; i < 3; i++) {
            seuilFields[i] = new TextField("20");
            seuilFields[i].setPrefWidth(50);
            controls.getChildren().add(seuilFields[i]);
        }

        seuilModeCombo.setOnAction(e -> {
            boolean manuel = "manuel".equalsIgnoreCase(seuilModeCombo.getValue());
            for (TextField tf : seuilFields) {
                tf.setDisable(!manuel);
            }
        });

        seuilModeCombo.getOnAction().handle(null);

        Button afficherButton = new Button("Afficher");
        controls.getChildren().add(afficherButton);

        GridPane imageGrid = new GridPane();
        imageGrid.setHgap(10);
        imageGrid.setVgap(10);

        imageGrid.add(new Label("Image originale"), 0, 0);
        setImageViewSize(originalImageView);
        imageGrid.add(originalImageView, 0, 1);

        for (int i = 0; i < 3; i++) {
            imageGrid.add(new Label("Bruitée σ" + (i+1)), i+1, 0);
            setImageViewSize(noisyImageViews.get(i));
            imageGrid.add(noisyImageViews.get(i), i+1, 1);
        }
        for (int i = 0; i < 3; i++) {
            imageGrid.add(new Label("Débruitée seuil " + (i+1)), i+1, 2);
            setImageViewSize(denoisedImageViews.get(i));
            imageGrid.add(denoisedImageViews.get(i), i+1, 3);
        }

        root.setTop(controls);
        root.setCenter(imageGrid);

        remplirListeImages();

        afficherButton.setOnAction(e -> lancerMainEtAfficher());

        Scene scene = new Scene(root, 1100, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void setImageViewSize(ImageView iv) {
        iv.setFitWidth(IMG_SIZE);
        iv.setFitHeight(IMG_SIZE);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
    }

    private void remplirListeImages() {
        try {
            String basePath = "src/main/resources/images/originals/";
            List<String> imageNames = ImageUtils.listImg(basePath).stream()
                .filter(name -> name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png") || name.toLowerCase().endsWith(".jpeg"))
                .toList();
            imageCombo.getItems().clear();
            imageCombo.getItems().addAll(imageNames);
            if (!imageNames.isEmpty()) {
                imageCombo.setValue(imageNames.get(0));
            }
        } catch (IOException e) {
            showAlert("Erreur", "Impossible de lister les images originales.");
            e.printStackTrace();
        }
    }

    private BufferedImage tryLoadImage(String pathWithoutExt) throws IOException {
        IOException lastException = null;
        for (String ext : new String[]{".png", ".jpg", ".jpeg"}) {
            try {
                return ImageUtils.loadImage(pathWithoutExt + ext);
            } catch (IOException e) {
                lastException = e;
            }
        }
        throw lastException != null ? lastException : new IOException("Image not found: " + pathWithoutExt);
    }

    private void afficherImagesGenerees() {
        String imageName = imageCombo.getValue();
        String thresholdType = thresholdTypeCombo.getValue();
        if (imageName == null || thresholdType == null) {
            showAlert("Erreur", "Veuillez sélectionner une image et un type de seuillage.");
            return;
        }
        String baseName = imageName.replaceAll("\\.[^.]+$", "");
        String basePath = "src/main/resources/images/";

        // Original
        try {
            String originalPathBase = basePath + "originals/" + baseName;
            BufferedImage original = tryLoadImage(originalPathBase);
            originalImageView.setImage(SwingFXUtils.toFXImage(original, null));
        } catch (IOException e) {
            originalImageView.setImage(null);
            showAlert("Erreur", "Image originale manquante : " + imageName);
        }

        // Bruitées
        double[] sigmas = new double[3];
        for (int i = 0; i < 3; i++) {
            try {
                sigmas[i] = Double.parseDouble(sigmaFields[i].getText());
            } catch (NumberFormatException e) {
                sigmas[i] = 10 + i * 10;
            }
        }
        for (int i = 0; i < 3; i++) {
            try {
                String noisyPathBase = basePath + "noisy/" + baseName + "_sigma_" + (int)sigmas[i];
                BufferedImage noisy = tryLoadImage(noisyPathBase);
                noisyImageViews.get(i).setImage(SwingFXUtils.toFXImage(noisy, null));
            } catch (IOException e) {
                noisyImageViews.get(i).setImage(null);
            }
        }

        // Débruitées
        for (int i = 0; i < 3; i++) {
            try {
                String thresholdTypeValue = thresholdTypeCombo.getValue();
                String denoisedPathBase = basePath + "denoised/" + thresholdTypeValue + "/" + baseName + "_sigma_" + (int)sigmas[i];
                BufferedImage denoised = tryLoadImage(denoisedPathBase);
                denoisedImageViews.get(i).setImage(SwingFXUtils.toFXImage(denoised, null));
            } catch (IOException e) {
                denoisedImageViews.get(i).setImage(null);
            }
        }
    }

    private void lancerMainEtAfficher() {
        String imageName = imageCombo.getValue();
        String thresholdType = thresholdTypeCombo.getValue();
        String seuilMode = seuilModeCombo.getValue();
        if (imageName == null || thresholdType == null || seuilMode == null) {
            showAlert("Erreur", "Veuillez sélectionner une image, un type de seuillage et un mode de seuil.");
            return;
        }

        double[] sigmas = new double[3];
        double[] seuils = new double[3];
        try {
            for (int i = 0; i < 3; i++) {
                sigmas[i] = Double.parseDouble(sigmaFields[i].getText());
                seuils[i] = Double.parseDouble(seuilFields[i].getText());
            }
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Veuillez entrer des valeurs numériques pour les sigmas et seuils.");
            return;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-cp", "target/classes:" +
                       System.getProperty("user.home") + "/.m2/repository/org/apache/commons/commons-math3/3.6.1/commons-math3-3.6.1.jar",
                "com.acp.Main",
                imageName,
                thresholdType,
                seuilMode,
                String.valueOf(seuils[0]),
                String.valueOf(seuils[1]),
                String.valueOf(seuils[2]),
                String.valueOf(sigmas[0]),
                String.valueOf(sigmas[1]),
                String.valueOf(sigmas[2])
            );
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                showAlert("Erreur", "Erreur lors de la génération des images (code " + exitCode + ").");
                return;
            }
            afficherImagesGenerees();
        } catch (Exception e) {
            showAlert("Erreur", "Impossible de lancer le traitement : " + e.getMessage());
        }
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
