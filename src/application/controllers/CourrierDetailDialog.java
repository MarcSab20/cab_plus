package application.controllers;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.Scene;
import application.models.*;
import application.services.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Dialogue détaillé pour visualiser toutes les informations d'un courrier
 * avec position, service actuel, service antérieur, et parcours complet
 */
public class CourrierDetailDialog extends Stage {
    
    private Courrier courrier;
    private CotationService cotationService;
    private WorkflowAnalysisService workflowService;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    public CourrierDetailDialog(Courrier courrier, CotationService cotationService, 
                                WorkflowAnalysisService workflowService) {
        this.courrier = courrier;
        this.cotationService = cotationService;
        this.workflowService = workflowService;
        
        initModality(Modality.APPLICATION_MODAL);
        setTitle("📋 Détails du Courrier - " + courrier.getCodeCourrier());
        setWidth(900);
        setHeight(700);
        
        VBox mainLayout = createMainLayout();
        Scene scene = new Scene(mainLayout);
        scene.getStylesheets().add(getClass().getResource("/application/styles/styles.css").toExternalForm());
        setScene(scene);
    }
    
    private VBox createMainLayout() {
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #f5f7fa;");
        
        // En-tête avec informations principales
        layout.getChildren().add(createHeaderSection());
        
        // Position et services (NOUVEAU)
        layout.getChildren().add(createPositionSection());
        
        // TabPane pour organiser les informations
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Onglet Informations générales
        Tab infoTab = new Tab("📄 Informations", createInfoSection());
        
        // Onglet Parcours détaillé
        Tab parcoursTab = new Tab("🔄 Parcours Complet", createParcoursSection());
        
        // Onglet Documents
        Tab docsTab = new Tab("📎 Documents", createDocumentsSection());
        
        // Onglet Commentaires
        Tab commentsTab = new Tab("💬 Commentaires", createCommentsSection());
        
        tabPane.getTabs().addAll(infoTab, parcoursTab, docsTab, commentsTab);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        layout.getChildren().add(tabPane);
        
        // Boutons action
        layout.getChildren().add(createActionButtons());
        
        return layout;
    }
    
    /**
     * NOUVEAU : Section position avec service actuel et antérieur
     */
    private VBox createPositionSection() {
        VBox section = new VBox(15);
        section.setPadding(new Insets(15));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                        "-fx-border-color: #3498db; -fx-border-width: 2; -fx-border-radius: 10;");
        
        Label titre = new Label("📍 POSITION ACTUELLE");
        titre.setFont(Font.font("System", FontWeight.BOLD, 16));
        titre.setStyle("-fx-text-fill: #2c3e50;");
        
        // Obtenir les cotations pour déterminer la position
        List<CotationCourrier> cotations = cotationService.getCotationsByCourrier(courrier.getId());
        
        if (cotations.isEmpty()) {
            Label noData = new Label("Aucune cotation trouvée pour ce courrier");
            noData.setStyle("-fx-text-fill: #95a5a6; -fx-font-style: italic;");
            section.getChildren().addAll(titre, noData);
            return section;
        }
        
        // Trier par date de cotation pour avoir l'historique chronologique
        cotations.sort((c1, c2) -> c1.getDateCotation().compareTo(c2.getDateCotation()));
        
        // Service actuel (dernière cotation)
        CotationCourrier cotationActuelle = cotations.get(cotations.size() - 1);
        String serviceActuelCode = cotationActuelle.getServiceDestination();
        ServiceHierarchy serviceActuel = workflowService.getServiceByCode(serviceActuelCode);
        
        // Service antérieur (avant-dernière cotation)
        CotationCourrier cotationAnterieure = cotations.size() > 1 ? cotations.get(cotations.size() - 2) : null;
        String serviceAnterieurCode = cotationAnterieure != null ? cotationAnterieure.getServiceDestination() : null;
        ServiceHierarchy serviceAnterieur = serviceAnterieurCode != null ? 
            workflowService.getServiceByCode(serviceAnterieurCode) : null;
        
        // Progression (position actuelle / total étapes)
        int positionActuelle = cotations.size();
        int totalEtapes = cotations.size(); // Peut être enrichi avec les étapes prévues
        
        // Grille d'informations
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(12);
        grid.setPadding(new Insets(10, 0, 0, 0));
        
        int row = 0;
        
        // Position dans le parcours
        Label lblPosition = createLabel("📊 Position :", true);
        Label valPosition = createLabel(String.format("Étape %d / %d", positionActuelle, totalEtapes), false);
        valPosition.setStyle("-fx-font-size: 14px; -fx-text-fill: #2980b9; -fx-font-weight: bold;");
        grid.add(lblPosition, 0, row);
        grid.add(valPosition, 1, row);
        
        // Barre de progression
        ProgressBar progressBar = new ProgressBar((double) positionActuelle / totalEtapes);
        progressBar.setPrefWidth(300);
        progressBar.setStyle("-fx-accent: #27ae60;");
        grid.add(progressBar, 2, row, 2, 1);
        row++;
        
        // Service actuel
        Label lblActuel = createLabel("🏢 Service Actuel :", true);
        String serviceActuelNom = serviceActuel != null ? serviceActuel.getServiceName() : serviceActuelCode;
        String serviceActuelIcone = serviceActuel != null ? serviceActuel.getIcone() + " " : "";
        Label valActuel = createLabel(serviceActuelIcone + serviceActuelNom, false);
        valActuel.setStyle("-fx-font-size: 14px; -fx-text-fill: #27ae60; -fx-font-weight: bold;");
        grid.add(lblActuel, 0, row);
        grid.add(valActuel, 1, row, 3, 1);
        row++;
        
        // Date arrivée service actuel
        Label lblDateActuel = createLabel("📅 Depuis le :", true);
        String dateActuel = cotationActuelle.getDateCotation().format(dateFormatter);
        Label valDateActuel = createLabel(dateActuel, false);
        grid.add(lblDateActuel, 0, row);
        grid.add(valDateActuel, 1, row);
        row++;
        
        // Service antérieur
        if (serviceAnterieur != null) {
            Label lblAnterieur = createLabel("⬅️ Service Antérieur :", true);
            String serviceAnterieurNom = serviceAnterieur.getServiceName();
            String serviceAnterieurIcone = serviceAnterieur.getIcone() + " ";
            Label valAnterieur = createLabel(serviceAnterieurIcone + serviceAnterieurNom, false);
            valAnterieur.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");
            grid.add(lblAnterieur, 0, row);
            grid.add(valAnterieur, 1, row, 3, 1);
            row++;
            
            // Durée dans le service antérieur
            if (cotationAnterieure != null && cotations.size() > 2) {
                CotationCourrier cotationAvantAnterieure = cotations.get(cotations.size() - 3);
                long dureeHeures = java.time.Duration.between(
                    cotationAvantAnterieure.getDateCotation(),
                    cotationAnterieure.getDateCotation()
                ).toHours();
                
                Label lblDureeAnt = createLabel("⏱️ Durée séjour :", true);
                Label valDureeAnt = createLabel(formatDuree(dureeHeures), false);
                grid.add(lblDureeAnt, 0, row);
                grid.add(valDureeAnt, 1, row);
                row++;
            }
        } else {
            Label lblAnterieur = createLabel("⬅️ Service Antérieur :", true);
            Label valAnterieur = createLabel("Aucun (première étape)", false);
            valAnterieur.setStyle("-fx-font-size: 13px; -fx-text-fill: #95a5a6; -fx-font-style: italic;");
            grid.add(lblAnterieur, 0, row);
            grid.add(valAnterieur, 1, row);
            row++;
        }
        
        // Statut du courrier
        Label lblStatut = createLabel("🔖 Statut :", true);
        Label valStatut = createLabel(courrier.getStatut().toUpperCase(), false);
        String statutColor = switch (courrier.getStatut().toLowerCase()) {
            case "nouveau" -> "#3498db";
            case "en_cours" -> "#f39c12";
            case "traite" -> "#27ae60";
            case "archive" -> "#95a5a6";
            default -> "#2c3e50";
        };
        valStatut.setStyle("-fx-font-size: 13px; -fx-text-fill: " + statutColor + "; -fx-font-weight: bold;");
        grid.add(lblStatut, 0, row);
        grid.add(valStatut, 1, row);
        
        section.getChildren().addAll(titre, new Separator(), grid);
        return section;
    }
    
    private VBox createHeaderSection() {
        VBox header = new VBox(10);
        header.setPadding(new Insets(15));
        header.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
                       "-fx-background-radius: 10;");
        
        Label code = new Label("📨 " + courrier.getCodeCourrier());
        code.setFont(Font.font("System", FontWeight.BOLD, 24));
        code.setStyle("-fx-text-fill: white;");
        
        Label objet = new Label(courrier.getObjet());
        objet.setFont(Font.font("System", 16));
        objet.setStyle("-fx-text-fill: white; -fx-opacity: 0.9;");
        objet.setWrapText(true);
        
        HBox badges = createBadges();
        
        header.getChildren().addAll(code, objet, badges);
        return header;
    }
    
    private HBox createBadges() {
        HBox badges = new HBox(10);
        
        // Badge type
        Label typeBadge = createBadge(courrier.getTypeCourrier(), getTypeColor(courrier.getTypeCourrier()));
        badges.getChildren().add(typeBadge);
        
        // Badge priorité
        String prioriteIcon = switch (courrier.getPriorite()) {
            case "TRES_URGENTE" -> "🚨";
            case "URGENTE" -> "🔴";
            default -> "🟡";
        };
        Label prioriteBadge = createBadge(prioriteIcon + " " + courrier.getPriorite().replace("_", " "), 
                                         getPrioriteColor(courrier.getPriorite()));
        badges.getChildren().add(prioriteBadge);
        
        // Badge confidentiel
        if (courrier.isConfidentiel()) {
            Label confBadge = createBadge("🔒 CONFIDENTIEL", "#c0392b");
            badges.getChildren().add(confBadge);
        }
        
        return badges;
    }
    
    private Label createBadge(String text, String color) {
        Label badge = new Label(text);
        badge.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                      "-fx-padding: 5 12; -fx-background-radius: 15; -fx-font-size: 11px; " +
                      "-fx-font-weight: bold;");
        return badge;
    }
    
    private ScrollPane createInfoSection() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(15));
        
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(12);
        
        int row = 0;
        
        // Expéditeur
        addGridRow(grid, row++, "📤 Expéditeur :", courrier.getExpediteur() != null ? 
                   courrier.getExpediteur() : "Non renseigné");
        
        // Destinataire
        addGridRow(grid, row++, "📥 Destinataire :", courrier.getDestinataire() != null ? 
                   courrier.getDestinataire() : "Non renseigné");
        
        // Date création
        addGridRow(grid, row++, "📅 Date création :", 
                   courrier.getDateCreation().format(dateFormatter));
        
        // Date réception
        if (courrier.getDateCourrier() != null) {
            addGridRow(grid, row++, "📬 Date réception :", 
                       courrier.getDateCreation().format(dateFormatter));
        }
        
        // Numéro ordre
        addGridRow(grid, row++, "🔢 Numéro d'ordre :", String.valueOf(courrier.getCodeCourrier()));
        
        // Observations
        if (courrier.getObservations() != null && !courrier.getObservations().isEmpty()) {
            Label lblObs = createLabel("📝 Observations :", true);
            TextArea txtObs = new TextArea(courrier.getObservations());
            txtObs.setWrapText(true);
            txtObs.setEditable(false);
            txtObs.setPrefRowCount(3);
            txtObs.setStyle("-fx-control-inner-background: #ecf0f1;");
            
            grid.add(lblObs, 0, row);
            grid.add(txtObs, 1, row, 3, 1);
            row++;
        }
        
        content.getChildren().add(grid);
        
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: white;");
        return scroll;
    }
    
    private ScrollPane createParcoursSection() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        
        List<CotationCourrier> cotations = cotationService.getCotationsByCourrier(courrier.getId());
        cotations.sort((c1, c2) -> c1.getDateCotation().compareTo(c2.getDateCotation()));
        
        if (cotations.isEmpty()) {
            Label noData = new Label("Aucun parcours disponible");
            noData.setStyle("-fx-font-size: 14px; -fx-text-fill: #95a5a6;");
            content.getChildren().add(noData);
        } else {
            for (int i = 0; i < cotations.size(); i++) {
                CotationCourrier cotation = cotations.get(i);
                content.getChildren().add(createParcoursItem(cotation, i + 1, i == cotations.size() - 1));
                
                // Ajouter une flèche sauf pour le dernier
                if (i < cotations.size() - 1) {
                    Label arrow = new Label("⬇️");
                    arrow.setStyle("-fx-font-size: 20px; -fx-padding: 5;");
                    arrow.setAlignment(Pos.CENTER);
                    HBox arrowBox = new HBox(arrow);
                    arrowBox.setAlignment(Pos.CENTER);
                    content.getChildren().add(arrowBox);
                }
            }
        }
        
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: white;");
        return scroll;
    }
    
    private VBox createParcoursItem(CotationCourrier cotation, int numero, boolean isCurrent) {
        VBox item = new VBox(8);
        item.setPadding(new Insets(12));
        
        String bgColor = isCurrent ? "#e8f5e9" : "#f8f9fa";
        String borderColor = isCurrent ? "#27ae60" : "#dee2e6";
        item.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 8; " +
                     "-fx-border-color: " + borderColor + "; -fx-border-width: 2; -fx-border-radius: 8;");
        
        // En-tête avec numéro et service
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label numLabel = new Label(String.valueOf(numero));
        numLabel.setStyle("-fx-background-color: " + (isCurrent ? "#27ae60" : "#95a5a6") + "; " +
                         "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 10; " +
                         "-fx-background-radius: 15; -fx-min-width: 30; -fx-alignment: center;");
        
        ServiceHierarchy service = workflowService.getServiceByCode(cotation.getServiceDestination());
        String serviceName = service != null ? 
            service.getIcone() + " " + service.getServiceName() : cotation.getServiceDestination();
        
        Label serviceLabel = new Label(serviceName);
        serviceLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        serviceLabel.setStyle("-fx-text-fill: #2c3e50;");
        
        if (isCurrent) {
            Label currentBadge = new Label("ACTUEL");
            currentBadge.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                                 "-fx-padding: 3 8; -fx-background-radius: 10; -fx-font-size: 10px;");
            header.getChildren().addAll(numLabel, serviceLabel, currentBadge);
        } else {
            header.getChildren().addAll(numLabel, serviceLabel);
        }
        
        // Informations
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(6);
        grid.setPadding(new Insets(8, 0, 0, 0));
        
        int row = 0;
        addGridRow(grid, row++, "📅 Date :", cotation.getDateCotation().format(dateFormatter));
        
        if (cotation.getCommentaire() != null && !cotation.getCommentaire().isEmpty()) {
            addGridRow(grid, row++, "💬 Commentaire :", cotation.getCommentaire());
        }
        
        item.getChildren().addAll(header, grid);
        return item;
    }
    
    private ScrollPane createDocumentsSection() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        
        Label titre = new Label("📎 Documents attachés");
        titre.setFont(Font.font("System", FontWeight.BOLD, 16));
        
        // TODO: Récupérer les documents liés au courrier
        Label placeholder = new Label("Fonctionnalité en cours de développement\nLes documents seront affichés ici");
        placeholder.setStyle("-fx-text-fill: #95a5a6; -fx-font-style: italic; -fx-padding: 20;");
        placeholder.setAlignment(Pos.CENTER);
        
        content.getChildren().addAll(titre, placeholder);
        
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: white;");
        return scroll;
    }
    
    private ScrollPane createCommentsSection() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(15));
        
        Label titre = new Label("💬 Commentaires et Annotations");
        titre.setFont(Font.font("System", FontWeight.BOLD, 16));
        
        // Récupérer tous les commentaires depuis les cotations
        List<CotationCourrier> cotations = cotationService.getCotationsByCourrier(courrier.getId());
        
        VBox commentsList = new VBox(10);
        boolean hasComments = false;
        
        for (CotationCourrier cotation : cotations) {
            if ((cotation.getCommentaire() != null && !cotation.getCommentaire().isEmpty())) {
                
                hasComments = true;
                commentsList.getChildren().add(createCommentItem(cotation));
            }
        }
        
        if (!hasComments) {
            Label noComments = new Label("Aucun commentaire pour ce courrier");
            noComments.setStyle("-fx-text-fill: #95a5a6; -fx-font-style: italic;");
            commentsList.getChildren().add(noComments);
        }
        
        content.getChildren().addAll(titre, new Separator(), commentsList);
        
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: white;");
        return scroll;
    }
    
    private VBox createCommentItem(CotationCourrier cotation) {
        VBox item = new VBox(8);
        item.setPadding(new Insets(12));
        item.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; " +
                     "-fx-border-color: #dee2e6; -fx-border-width: 1; -fx-border-radius: 8;");
        
        ServiceHierarchy service = workflowService.getServiceByCode(cotation.getServiceDestination());
        String serviceName = service != null ? service.getServiceName() : cotation.getServiceDestination();
        
        Label header = new Label(serviceName + " - " + cotation.getDateCotation().format(dateFormatter));
        header.setFont(Font.font("System", FontWeight.BOLD, 12));
        header.setStyle("-fx-text-fill: #2c3e50;");
        
        VBox texts = new VBox(5);
        
        if (cotation.getCommentaire() != null && !cotation.getCommentaire().isEmpty()) {
            Label commentLabel = new Label("💬 " + cotation.getCommentaire());
            commentLabel.setWrapText(true);
            commentLabel.setStyle("-fx-text-fill: #34495e;");
            texts.getChildren().add(commentLabel);
        }
        
        
        item.getChildren().addAll(header, texts);
        return item;
    }
    
    private HBox createActionButtons() {
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(10, 0, 0, 0));
        
        Button btnPrint = new Button("🖨️ Imprimer");
        btnPrint.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                         "-fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5;");
        
        Button btnExport = new Button("📤 Exporter PDF");
        btnExport.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                          "-fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5;");
        
        Button btnClose = new Button("✖️ Fermer");
        btnClose.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; " +
                         "-fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5;");
        btnClose.setOnAction(e -> close());
        
        buttons.getChildren().addAll(btnPrint, btnExport, btnClose);
        return buttons;
    }
    
    // Méthodes utilitaires
    
    private void addGridRow(GridPane grid, int row, String label, String value) {
        Label lbl = createLabel(label, true);
        Label val = createLabel(value, false);
        grid.add(lbl, 0, row);
        grid.add(val, 1, row);
    }
    
    private Label createLabel(String text, boolean isBold) {
        Label label = new Label(text);
        if (isBold) {
            label.setFont(Font.font("System", FontWeight.BOLD, 13));
            label.setStyle("-fx-text-fill: #2c3e50;");
        } else {
            label.setFont(Font.font("System", 13));
            label.setStyle("-fx-text-fill: #34495e;");
        }
        return label;
    }
    
    private String getTypeColor(String type) {
        return switch (type) {
            case "ENTRANT" -> "#3498db";
            case "SORTANT" -> "#e74c3c";
            case "INTERNE" -> "#f39c12";
            default -> "#95a5a6";
        };
    }
    
    private String getPrioriteColor(String priorite) {
        return switch (priorite) {
            case "TRES_URGENTE" -> "#c0392b";
            case "URGENTE" -> "#e74c3c";
            default -> "#f39c12";
        };
    }
    
    private String formatDuree(long heures) {
        if (heures < 24) {
            return heures + "h";
        } else {
            long jours = heures / 24;
            long h = heures % 24;
            return jours + "j " + (h > 0 ? h + "h" : "");
        }
    }
}
