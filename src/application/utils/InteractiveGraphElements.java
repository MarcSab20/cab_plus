package application.utils;

import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.geometry.Pos;
import javafx.animation.*;
import javafx.util.Duration;
import application.models.ServiceHierarchy;

import java.util.*;
import java.util.function.Consumer;

/**
 * Gestionnaire d'éléments interactifs pour la visualisation moderne du workflow
 * Fournit des composants visuels avec animations et interactions avancées
 */
public class InteractiveGraphElements {
    
    // ═══════════════════════════════════════════════════════════════
    // CLASSE GRAPHARC - ARC INTERACTIF AVEC MULTI-COURRIERS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Arc interactif représentant le flux de courriers entre services
     */
    public static class GraphArc extends Group {
        private double startX, startY, endX, endY;
        private int nbCourriers;
        private List<Integer> courrierIds;
        private Map<Integer, Color> courrierColors;
        private Path mainPath;
        private Consumer<List<Integer>> onArcClick;
        
        /**
         * Constructeur d'un arc avec plusieurs courriers
         */
        public GraphArc(double startX, double startY, double endX, double endY,
                       int nbCourriers, List<Integer> courrierIds,
                       Map<Integer, Color> courrierColors) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.nbCourriers = nbCourriers;
            this.courrierIds = new ArrayList<>(courrierIds);
            this.courrierColors = courrierColors;
            
            createArc();
        }
        
        private void createArc() {
            // Calculer l'épaisseur selon le nombre de courriers
            double baseWidth = 3;
            double maxWidth = 20;
            double width = Math.min(maxWidth, baseWidth + nbCourriers * 1.5);
            
            // Créer le chemin courbe
            double dx = endX - startX;
            double dy = endY - startY;
            double distance = Math.sqrt(dx * dx + dy * dy);
            
            // Point de contrôle pour courbe
            double curvature = 0.15;
            double midX = (startX + endX) / 2;
            double midY = (startY + endY) / 2;
            double controlX = midX - curvature * distance * (dy / distance);
            double controlY = midY + curvature * distance * (dx / distance);
            
            mainPath = new Path();
            mainPath.getElements().add(new MoveTo(startX, startY));
            mainPath.getElements().add(new QuadCurveTo(controlX, controlY, endX, endY));
            
            // Couleur moyenne des courriers
            Color avgColor = calculateAverageColor();
            mainPath.setStroke(avgColor);
            mainPath.setStrokeWidth(width);
            mainPath.setFill(null);
            mainPath.setOpacity(0.7);
            
            // Effet d'ombre
            DropShadow shadow = new DropShadow();
            shadow.setColor(Color.rgb(0, 0, 0, 0.3));
            shadow.setRadius(5);
            mainPath.setEffect(shadow);
            
            // Zone de clic invisible plus large
            Path clickArea = new Path();
            clickArea.getElements().add(new MoveTo(startX, startY));
            clickArea.getElements().add(new QuadCurveTo(controlX, controlY, endX, endY));
            clickArea.setStroke(Color.TRANSPARENT);
            clickArea.setStrokeWidth(Math.max(width * 2, 15));
            clickArea.setFill(null);
            clickArea.setCursor(Cursor.HAND);
            
            // Flèche à la fin
            Polygon arrow = createArrowHead(endX, endY, controlX, controlY, avgColor);
            
            // Interactivité
            setupInteractivity(clickArea);
            
            // Label avec nombre
            if (nbCourriers > 1) {
                Label countLabel = createCountLabel(midX, midY, nbCourriers);
                getChildren().add(countLabel);
            }
            
            getChildren().addAll(mainPath, clickArea, arrow);
        }
        
        private Color calculateAverageColor() {
            if (courrierIds.isEmpty()) {
                return Color.web("#3498db");
            }
            
            double r = 0, g = 0, b = 0;
            int count = 0;
            
            for (Integer id : courrierIds) {
                Color color = courrierColors.get(id);
                if (color != null) {
                    r += color.getRed();
                    g += color.getGreen();
                    b += color.getBlue();
                    count++;
                }
            }
            
            if (count == 0) return Color.web("#3498db");
            
            return Color.color(r / count, g / count, b / count);
        }
        
        private Polygon createArrowHead(double endX, double endY, 
                                       double controlX, double controlY, Color color) {
            double angle = Math.atan2(endY - controlY, endX - controlX);
            double arrowLength = 12;
            double arrowWidth = 8;
            
            double x1 = endX - arrowLength * Math.cos(angle - Math.PI / 6);
            double y1 = endY - arrowLength * Math.sin(angle - Math.PI / 6);
            
            double x2 = endX - arrowLength * Math.cos(angle + Math.PI / 6);
            double y2 = endY - arrowLength * Math.sin(angle + Math.PI / 6);
            
            Polygon arrow = new Polygon(
                endX, endY,
                x1, y1,
                x2, y2
            );
            
            arrow.setFill(color);
            arrow.setStroke(color);
            
            return arrow;
        }
        
        private Label createCountLabel(double x, double y, int count) {
            Label label = new Label(String.valueOf(count));
            label.setLayoutX(x - 15);
            label.setLayoutY(y - 25);
            label.setStyle(
                "-fx-background-color: white;" +
                "-fx-padding: 4 8;" +
                "-fx-border-color: #3498db;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: 11px;" +
                "-fx-text-fill: #3498db;"
            );
            
            return label;
        }
        
        private void setupInteractivity(Path clickArea) {
            Glow glow = new Glow(0.0);
            mainPath.setEffect(glow);
            
            clickArea.setOnMouseEntered(e -> {
                mainPath.setOpacity(1.0);
                mainPath.setStrokeWidth(mainPath.getStrokeWidth() * 1.3);
                
                Timeline glowAnim = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(glow.levelProperty(), 0.0)),
                    new KeyFrame(Duration.millis(200), new KeyValue(glow.levelProperty(), 0.8))
                );
                glowAnim.play();
                
                showTooltip(clickArea);
            });
            
            clickArea.setOnMouseExited(e -> {
                mainPath.setOpacity(0.7);
                mainPath.setStrokeWidth(mainPath.getStrokeWidth() / 1.3);
                glow.setLevel(0.0);
            });
            
            clickArea.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && onArcClick != null) {
                    onArcClick.accept(courrierIds);
                }
            });
        }
        
        private void showTooltip(Path area) {
            String text = String.format(
                "📧 %d courrier%s\n🖱️ Cliquez pour voir les détails",
                nbCourriers,
                nbCourriers > 1 ? "s" : ""
            );
            
            Tooltip tooltip = new Tooltip(text);
            tooltip.setStyle(
                "-fx-background-color: rgba(44, 62, 80, 0.95);" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 12px;" +
                "-fx-padding: 10px;" +
                "-fx-background-radius: 8px;"
            );
            
            Tooltip.install(area, tooltip);
        }
        
        public void setOnArcClick(Consumer<List<Integer>> handler) {
            this.onArcClick = handler;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // CLASSE GRAPHNODE - NŒUD SERVICE INTERACTIF
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Nœud interactif représentant un service dans le graphe
     */
    public static class GraphNode extends Group {
        private double x, y, width, height;
        private ServiceHierarchy service;
        private Object nodeData; // ServiceNodeData
        private Rectangle rect;
        private VBox contentBox;
        private Runnable onNodeClick;
        
        /**
         * Constructeur d'un nœud de service
         */
        public GraphNode(double x, double y, double width, double height,
                        ServiceHierarchy service, Object nodeData) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.service = service;
            this.nodeData = nodeData;
            
            createNode();
        }
        
        private void createNode() {
            // Rectangle principal
            rect = new Rectangle(x, y, width, height);
            rect.setArcWidth(15);
            rect.setArcHeight(15);
            
            // Couleur selon le niveau
            String color = service.getCouleur();
            rect.setFill(Color.web(color));
            rect.setStroke(Color.web(CourrierColorPalette.darkenColor(color, 0.2)));
            rect.setStrokeWidth(2.5);
            
            // Ombre
            DropShadow shadow = new DropShadow();
            shadow.setColor(Color.rgb(0, 0, 0, 0.4));
            shadow.setRadius(10);
            shadow.setOffsetY(4);
            rect.setEffect(shadow);
            
            // Contenu texte
            contentBox = new VBox(5);
            contentBox.setLayoutX(x + 10);
            contentBox.setLayoutY(y + 10);
            contentBox.setMaxWidth(width - 20);
            contentBox.setAlignment(Pos.CENTER_LEFT);
            
            // Icône + Nom
            Label iconLabel = new Label(service.getIcone());
            iconLabel.setStyle("-fx-font-size: 20px;");
            
            Label nameLabel = new Label(service.getServiceName());
            nameLabel.setStyle(
                "-fx-font-weight: bold;" +
                "-fx-font-size: 12px;" +
                "-fx-text-fill: white;" +
                "-fx-wrap-text: true;"
            );
            nameLabel.setMaxWidth(width - 20);
            
            // Code service
            Label codeLabel = new Label(service.getServiceCode());
            codeLabel.setStyle(
                "-fx-font-size: 9px;" +
                "-fx-text-fill: rgba(255,255,255,0.7);"
            );
            
            contentBox.getChildren().addAll(iconLabel, nameLabel, codeLabel);
            
            // Interactivité
            setupNodeInteractivity();
            
            getChildren().addAll(rect, contentBox);
        }
        
        private void setupNodeInteractivity() {
            rect.setCursor(Cursor.HAND);
            
            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), this);
            scaleUp.setToX(1.08);
            scaleUp.setToY(1.08);
            
            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), this);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);
            
            rect.setOnMouseEntered(e -> {
                scaleUp.play();
                DropShadow shadow = (DropShadow) rect.getEffect();
                shadow.setRadius(15);
                shadow.setOffsetY(6);
                
                showNodeTooltip();
            });
            
            rect.setOnMouseExited(e -> {
                scaleDown.play();
                DropShadow shadow = (DropShadow) rect.getEffect();
                shadow.setRadius(10);
                shadow.setOffsetY(4);
            });
            
            rect.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && onNodeClick != null) {
                    // Animation de clic
                    ScaleTransition clickAnim = new ScaleTransition(Duration.millis(100), this);
                    clickAnim.setToX(0.95);
                    clickAnim.setToY(0.95);
                    clickAnim.setAutoReverse(true);
                    clickAnim.setCycleCount(2);
                    clickAnim.setOnFinished(ev -> onNodeClick.run());
                    clickAnim.play();
                }
            });
        }
        
        private void showNodeTooltip() {
            String text = String.format(
                "%s %s\nCode: %s\nNiveau: %d\n\n🖱️ Double-clic pour statistiques",
                service.getIcone(),
                service.getServiceName(),
                service.getServiceCode(),
                service.getNiveau()
            );
            
            Tooltip tooltip = new Tooltip(text);
            tooltip.setStyle(
                "-fx-background-color: rgba(44, 62, 80, 0.95);" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 11px;" +
                "-fx-padding: 12px;" +
                "-fx-background-radius: 8px;"
            );
            
            Tooltip.install(rect, tooltip);
        }
        
        public void setOnNodeClick(Runnable handler) {
            this.onNodeClick = handler;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // MÉTHODES UTILITAIRES
    // ═══════════════════════════════════════════════════════════════
    
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
        scale.play();
        
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