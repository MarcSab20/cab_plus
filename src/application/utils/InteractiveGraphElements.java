package application.utils;

import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.geometry.Point2D;
import javafx.animation.*;
import javafx.util.Duration;
import application.models.Courrier;
import application.models.WorkflowStep;
import java.util.function.Consumer;

/**
 * Gestionnaire d'éléments interactifs pour la visualisation moderne du workflow
 * Fournit des composants visuels avec animations et interactions avancées
 */
public class InteractiveGraphElements {
    
    /**
     * Crée une flèche de connexion interactive avec animation au survol
     */
    public static Group createInteractiveArrow(
            Point2D start, Point2D end, double width, String color,
            Courrier courrier, Consumer<Courrier> onClickHandler) {
        
        Group arrowGroup = new Group();
        
        // Calcul du point de contrôle pour courbe de Bézier
        double midX = (start.getX() + end.getX()) / 2;
        double midY = (start.getY() + end.getY()) / 2;
        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        double length = Math.sqrt(dx * dx + dy * dy);
        double curvature = 0.2;
        double controlX = midX - curvature * length * (dy / length);
        double controlY = midY + curvature * length * (dx / length);
        
        // Créer le chemin courbe
        Path path = new Path();
        path.getElements().add(new MoveTo(start.getX(), start.getY()));
        path.getElements().add(new QuadCurveTo(controlX, controlY, end.getX(), end.getY()));
        
        path.setStroke(Color.web(color));
        path.setStrokeWidth(width);
        path.setFill(null);
        path.setOpacity(0.7);
        
        // Effet visuel
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.3));
        shadow.setRadius(5);
        path.setEffect(shadow);
        
        // Ajouter une zone invisible plus large pour faciliter l'interaction
        Path hitArea = new Path();
        hitArea.getElements().add(new MoveTo(start.getX(), start.getY()));
        hitArea.getElements().add(new QuadCurveTo(controlX, controlY, end.getX(), end.getY()));
        hitArea.setStroke(Color.TRANSPARENT);
        hitArea.setStrokeWidth(Math.max(width * 3, 20)); // Zone de clic plus large
        hitArea.setFill(null);
        hitArea.setCursor(Cursor.HAND);
        
        // Animation au survol
        Glow glow = new Glow(0.0);
        path.setEffect(glow);
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), path);
        fadeIn.setToValue(1.0);
        
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(200), path);
        scaleIn.setToX(1.1);
        scaleIn.setToY(1.1);
        
        Timeline glowAnimation = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(glow.levelProperty(), 0.0)),
            new KeyFrame(Duration.millis(200), new KeyValue(glow.levelProperty(), 0.8))
        );
        
        hitArea.setOnMouseEntered(e -> {
            path.setStrokeWidth(width * 1.5);
            fadeIn.play();
            glowAnimation.play();
            hitArea.setCursor(Cursor.HAND);
            
            // Afficher un tooltip personnalisé
            showCustomTooltip(hitArea, courrier);
        });
        
        hitArea.setOnMouseExited(e -> {
            path.setStrokeWidth(width);
            path.setOpacity(0.7);
            glow.setLevel(0.0);
        });
        
        // Gestion du clic
        hitArea.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && onClickHandler != null) {
                onClickHandler.accept(courrier);
            }
        });
        
        arrowGroup.getChildren().addAll(path, hitArea);
        
        return arrowGroup;
    }
    
    /**
     * Crée un nœud de service interactif avec effet 3D
     */
    public static Group createInteractiveServiceNode(
            double x, double y, double width, double height,
            String serviceCode, String serviceName, String color,
            Consumer<String> onClickHandler) {
        
        Group nodeGroup = new Group();
        
        // Rectangle principal avec coins arrondis
        Rectangle rect = new Rectangle(x, y, width, height);
        rect.setArcWidth(15);
        rect.setArcHeight(15);
        rect.setFill(Color.web(color));
        rect.setStroke(Color.web(CourrierColorPalette.darkenColor(color, 0.2)));
        rect.setStrokeWidth(2);
        
        // Effet d'ombre et de profondeur
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.4));
        shadow.setRadius(10);
        shadow.setOffsetX(0);
        shadow.setOffsetY(4);
        rect.setEffect(shadow);
        
        // Animation au survol
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), rect);
        scaleUp.setToX(1.1);
        scaleUp.setToY(1.1);
        
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), rect);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);
        
        rect.setOnMouseEntered(e -> {
            scaleUp.play();
            rect.setCursor(Cursor.HAND);
            shadow.setRadius(15);
            shadow.setOffsetY(6);
        });
        
        rect.setOnMouseExited(e -> {
            scaleDown.play();
            shadow.setRadius(10);
            shadow.setOffsetY(4);
        });
        
        rect.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && onClickHandler != null) {
                // Animation de clic
                ScaleTransition clickAnim = new ScaleTransition(Duration.millis(100), rect);
                clickAnim.setToX(0.95);
                clickAnim.setToY(0.95);
                clickAnim.setAutoReverse(true);
                clickAnim.setCycleCount(2);
                clickAnim.play();
                
                onClickHandler.accept(serviceCode);
            }
        });
        
        nodeGroup.getChildren().add(rect);
        
        return nodeGroup;
    }
    
    /**
     * Affiche un tooltip personnalisé stylisé
     */
    private static void showCustomTooltip(javafx.scene.Node node, Courrier courrier) {
        if (courrier == null) return;
        
        String tooltipText = String.format(
            "📧 %s\n" +
            "Objet: %s\n" +
            "Type: %s\n" +
            "Priorité: %s\n" +
            "Statut: %s\n\n" +
            "🖱️ Cliquez pour voir les détails",
            courrier.getCodeCourrier(),
            courrier.getObjet().length() > 40 ? 
                courrier.getObjet().substring(0, 37) + "..." : courrier.getObjet(),
            courrier.getTypeCourrier(),
            courrier.getPriorite(),
            courrier.getStatut()
        );
        
        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.setStyle(
            "-fx-background-color: rgba(44, 62, 80, 0.95);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 12px;" +
            "-fx-padding: 10px;" +
            "-fx-background-radius: 8px;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 0, 0);"
        );
        tooltip.setShowDelay(javafx.util.Duration.millis(200));
        
        Tooltip.install(node, tooltip);
    }
    
    /**
     * Crée une animation de pulsation pour attirer l'attention
     */
    public static Timeline createPulseAnimation(javafx.scene.Node node) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(800), node);
        scale.setToX(1.1);
        scale.setToY(1.1);
        scale.setAutoReverse(true);
        scale.setCycleCount(Animation.INDEFINITE);
        
        Timeline pulse = new Timeline();
        pulse.getKeyFrames().add(new KeyFrame(Duration.millis(800)));
        
        return pulse;
    }
    
    /**
     * Crée un indicateur de notification (badge)
     */
    public static Circle createNotificationBadge(double x, double y, int count, String color) {
        Circle badge = new Circle(x, y, 12);
        badge.setFill(Color.web(color));
        badge.setStroke(Color.WHITE);
        badge.setStrokeWidth(2);
        
        // Effet de pulsation
        ScaleTransition pulse = new ScaleTransition(Duration.millis(600), badge);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.15);
        pulse.setToY(1.15);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();
        
        return badge;
    }
}