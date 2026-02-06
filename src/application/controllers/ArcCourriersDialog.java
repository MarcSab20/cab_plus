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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import application.models.*;
import application.services.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dialogue affichant la liste des courriers passant par un arc
 * avec possibilité de visualiser les détails complets
 */
public class ArcCourriersDialog extends Stage {
    
    private List<Courrier> courriers;
    private String serviceSource;
    private String serviceDestination;
    private CourrierService courrierService;
    private CotationService cotationService;
    private WorkflowAnalysisService workflowService;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    public ArcCourriersDialog(List<Integer> courrierIds, String serviceSource, 
                             String serviceDestination, CourrierService courrierService,
                             CotationService cotationService, WorkflowAnalysisService workflowService) {
        this.serviceSource = serviceSource;
        this.serviceDestination = serviceDestination;
        this.courrierService = courrierService;
        this.cotationService = cotationService;
        this.workflowService = workflowService;
        
        // Charger les courriers
        this.courriers = courrierIds.stream()
            .map(courrierService::getCourrierById)
            .filter(c -> c != null)
            .collect(Collectors.toList());
        
        initModality(Modality.APPLICATION_MODAL);
        setTitle("📊 Flux de courriers : " + getServiceName(serviceSource) + " → " + 
                 getServiceName(serviceDestination));
        setWidth(1000);
        setHeight(650);
        
        VBox mainLayout = createMainLayout();
        Scene scene = new Scene(mainLayout);
        scene.getStylesheets().add(getClass().getResource("/application/styles/styles.css").toExternalForm());
        setScene(scene);
    }
    
    private VBox createMainLayout() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #f5f7fa;");
        
        // En-tête avec statistiques
        layout.getChildren().add(createHeaderSection());
        
        // Tableau des courriers
        TableView<Courrier> table = createCourrierTable();
        VBox.setVgrow(table, Priority.ALWAYS);
        layout.getChildren().add(table);
        
        // Boutons actions
        layout.getChildren().add(createActionButtons());
        
        return layout;
    }
    
    private VBox createHeaderSection() {
        VBox header = new VBox(12);
        header.setPadding(new Insets(15));
        header.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                       "-fx-border-color: #3498db; -fx-border-width: 2; -fx-border-radius: 10;");
        
        // Titre avec services
        Label titre = new Label("🔄 FLUX ENTRE SERVICES");
        titre.setFont(Font.font("System", FontWeight.BOLD, 18));
        titre.setStyle("-fx-text-fill: #2c3e50;");
        
        // Chemin
        HBox chemin = new HBox(15);
        chemin.setAlignment(Pos.CENTER_LEFT);
        
        ServiceHierarchy serviceS = workflowService.getServiceByCode(serviceSource);
        ServiceHierarchy serviceD = workflowService.getServiceByCode(serviceDestination);
        
        Label lblSource = new Label((serviceS != null ? serviceS.getIcone() + " " : "") + 
                                   getServiceName(serviceSource));
        lblSource.setFont(Font.font("System", FontWeight.BOLD, 14));
        lblSource.setStyle("-fx-text-fill: #2c3e50; -fx-background-color: #ecf0f1; " +
                          "-fx-padding: 8 15; -fx-background-radius: 8;");
        
        Label arrow = new Label("➔");
        arrow.setFont(Font.font(20));
        arrow.setStyle("-fx-text-fill: #3498db;");
        
        Label lblDest = new Label((serviceD != null ? serviceD.getIcone() + " " : "") + 
                                 getServiceName(serviceDestination));
        lblDest.setFont(Font.font("System", FontWeight.BOLD, 14));
        lblDest.setStyle("-fx-text-fill: #2c3e50; -fx-background-color: #ecf0f1; " +
                        "-fx-padding: 8 15; -fx-background-radius: 8;");
        
        chemin.getChildren().addAll(lblSource, arrow, lblDest);
        
        // Statistiques
        HBox stats = new HBox(30);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.setPadding(new Insets(10, 0, 0, 0));
        
        // Total courriers
        VBox statTotal = createStatBox("📊 Total", String.valueOf(courriers.size()), "#3498db");
        stats.getChildren().add(statTotal);
        
        // Par type
        long entrants = courriers.stream().filter(c -> "ENTRANT".equals(c.getTypeCourrier())).count();
        long sortants = courriers.stream().filter(c -> "SORTANT".equals(c.getTypeCourrier())).count();
        long internes = courriers.stream().filter(c -> "INTERNE".equals(c.getTypeCourrier())).count();
        
        if (entrants > 0) {
            VBox statEntrant = createStatBox("📥 Entrants", String.valueOf(entrants), "#27ae60");
            stats.getChildren().add(statEntrant);
        }
        if (sortants > 0) {
            VBox statSortant = createStatBox("📤 Sortants", String.valueOf(sortants), "#e74c3c");
            stats.getChildren().add(statSortant);
        }
        if (internes > 0) {
            VBox statInterne = createStatBox("🔄 Internes", String.valueOf(internes), "#f39c12");
            stats.getChildren().add(statInterne);
        }
        
        // Par priorité
        long urgents = courriers.stream()
            .filter(c -> "TRES_URGENTE".equals(c.getPriorite()) || "URGENTE".equals(c.getPriorite()))
            .count();
        
        if (urgents > 0) {
            VBox statUrgent = createStatBox("🚨 Urgents", String.valueOf(urgents), "#c0392b");
            stats.getChildren().add(statUrgent);
        }
        
        header.getChildren().addAll(titre, chemin, new Separator(), stats);
        return header;
    }
    
    private VBox createStatBox(String label, String value, String color) {
        VBox box = new VBox(3);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-padding: 8; -fx-background-color: " + color + "22; -fx-background-radius: 8;");
        
        Label valLabel = new Label(value);
        valLabel.setFont(Font.font("System", FontWeight.BOLD, 20));
        valLabel.setStyle("-fx-text-fill: " + color + ";");
        
        Label lblLabel = new Label(label);
        lblLabel.setFont(Font.font("System", 11));
        lblLabel.setStyle("-fx-text-fill: #2c3e50;");
        
        box.getChildren().addAll(valLabel, lblLabel);
        return box;
    }
    
    private TableView<Courrier> createCourrierTable() {
        TableView<Courrier> table = new TableView<>();
        table.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        
        // Colonne Code
        TableColumn<Courrier, String> colCode = new TableColumn<>("Code");
        colCode.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getCodeCourrier()));
        colCode.setPrefWidth(120);
        
        // Colonne Objet
        TableColumn<Courrier, String> colObjet = new TableColumn<>("Objet");
        colObjet.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getObjet()));
        colObjet.setPrefWidth(300);
        
        // Colonne Type
        TableColumn<Courrier, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            getTypeIcon(data.getValue().getTypeCourrier()) + " " + data.getValue().getTypeCourrier()));
        colType.setPrefWidth(100);
        
        // Colonne Priorité
        TableColumn<Courrier, String> colPriorite = new TableColumn<>("Priorité");
        colPriorite.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            getPrioriteIcon(data.getValue().getPriorite()) + " " + 
            data.getValue().getPriorite().replace("_", " ")));
        colPriorite.setPrefWidth(130);
        
        // Colonne Statut
        TableColumn<Courrier, String> colStatut = new TableColumn<>("Statut");
        colStatut.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
            data.getValue().getStatut().toUpperCase()));
        colStatut.setPrefWidth(100);
        
        // Colonne Position
        TableColumn<Courrier, String> colPosition = new TableColumn<>("Position");
        colPosition.setCellValueFactory(data -> {
            List<CotationCourrier> cotations = cotationService.getCotationsByCourrier(data.getValue().getId());
            int position = cotations.size();
            return new javafx.beans.property.SimpleStringProperty(
                String.format("Étape %d/%d", position, position));
        });
        colPosition.setPrefWidth(100);
        
        // Colonne Actions
        TableColumn<Courrier, Void> colActions = new TableColumn<>("Actions");
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button btnVoir = new Button("👁️ Voir");
            {
                btnVoir.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                               "-fx-font-size: 11px; -fx-padding: 5 10; -fx-background-radius: 5;");
                btnVoir.setOnAction(event -> {
                    Courrier courrier = getTableView().getItems().get(getIndex());
                    openCourrierDetail(courrier);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnVoir);
            }
        });
        colActions.setPrefWidth(100);
        
        table.getColumns().addAll(colCode, colObjet, colType, colPriorite, colStatut, colPosition, colActions);
        
        // Données
        ObservableList<Courrier> data = FXCollections.observableArrayList(courriers);
        table.setItems(data);
        
        // Double-clic pour voir détails
        table.setRowFactory(tv -> {
            TableRow<Courrier> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openCourrierDetail(row.getItem());
                }
            });
            return row;
        });
        
        return table;
    }
    
    private void openCourrierDetail(Courrier courrier) {
        CourrierDetailDialog dialog = new CourrierDetailDialog(courrier, cotationService, workflowService);
        dialog.showAndWait();
    }
    
    private HBox createActionButtons() {
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(10, 0, 0, 0));
        
        Button btnExport = new Button("📤 Exporter Liste");
        btnExport.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                          "-fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5;");
        
        Button btnClose = new Button("✖️ Fermer");
        btnClose.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; " +
                         "-fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5;");
        btnClose.setOnAction(e -> close());
        
        buttons.getChildren().addAll(btnExport, btnClose);
        return buttons;
    }
    
    // Utilitaires
    
    private String getServiceName(String code) {
        ServiceHierarchy service = workflowService.getServiceByCode(code);
        return service != null ? service.getServiceName() : code;
    }
    
    private String getTypeIcon(String type) {
        return switch (type) {
            case "ENTRANT" -> "📥";
            case "SORTANT" -> "📤";
            case "INTERNE" -> "🔄";
            default -> "📄";
        };
    }
    
    private String getPrioriteIcon(String priorite) {
        return switch (priorite) {
            case "TRES_URGENTE" -> "🚨";
            case "URGENTE" -> "🔴";
            default -> "🟡";
        };
    }
}