package application.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import application.models.*;
import application.services.WorkflowAnalysisService;
import application.services.CotationService;
import application.services.CourrierService;
import application.utils.SessionManager;
import application.utils.AlertUtils;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CONTRÔLEUR AMÉLIORÉ pour la visualisation dynamique et interactive du workflow
 * 
 * NOUVELLES FONCTIONNALITÉS :
 * - Conformité totale avec la structure de la BD
 * - Intégration des cotations dans le workflow
 * - Statistiques par service avec bureaux associés
 * - Détection avancée des goulots d'étranglement
 * - Métriques de performance détaillées
 * - Vue détaillée par service
 */
public class WorkflowSuiviController implements Initializable {
    
    // === Contrôles FXML ===
    @FXML private ComboBox<String> cbTypeFlux;
    @FXML private DatePicker dpDebut;
    @FXML private DatePicker dpFin;
    @FXML private CheckBox chkAfficherStatistiques;
    @FXML private CheckBox chkAfficherGoulots;
    @FXML private Slider sliderZoom;
    @FXML private ScrollPane graphScrollPane;
    @FXML private Pane graphPane;
    
    // Statistiques globales
    @FXML private Label statTotalCourriers;
    @FXML private Label statServicesActifs;
    @FXML private Label statDureeMoyenne;
    @FXML private Label statGoulotsDetectes;
    @FXML private VBox statsDetailContainer;
    
    // Tableau détaillé
    @FXML private TableView<ServiceFlowStats> tableFluxDetails;
    @FXML private TableColumn<ServiceFlowStats, String> colService;
    @FXML private TableColumn<ServiceFlowStats, Number> colEntrants;
    @FXML private TableColumn<ServiceFlowStats, Number> colSortants;
    @FXML private TableColumn<ServiceFlowStats, Number> colInternes;
    @FXML private TableColumn<ServiceFlowStats, String> colDuree;
    @FXML private TableColumn<ServiceFlowStats, String> colStatut;
    
    // Mode de visualisation
    @FXML private RadioButton rbModeCollectif;
    @FXML private RadioButton rbModeIndividuel;
    @FXML private ToggleGroup modeToggleGroup;
    @FXML private VBox courrierSelectionBox;
    @FXML private ComboBox<CourrierItem> cbCourrierSelection;
    @FXML private Button btnRechercherCourrier;
    @FXML private VBox controlesModeCollectif;
    @FXML private VBox infoCourrierIndividuel;
    
    // Informations courrier individuel
    @FXML private Label lblCourrierNumero;
    @FXML private Label lblCourrierObjet;
    @FXML private Label lblCourrierType;
    @FXML private Label lblCourrierDate;
    @FXML private Label lblCourrierStatut;
    
    // Labels dynamiques
    @FXML private Label lblModeActif;
    @FXML private Label lblStatsTitre;
    @FXML private Label lblDetailsTitle;
    @FXML private Label lblStatus;
    @FXML private Label lblNbEtapes;
    @FXML private Label lblZoomValue;
    @FXML private Label statTotalCourrierLabel;
    @FXML private Label statServicesLabel;
    @FXML private Label statDureeLabel;
    @FXML private Label statGoulotsLabel;
    @FXML private Label lblInfo1;
    @FXML private Label lblInfo2;
    @FXML private Label lblInfo3;
    
    // Chronologie
    @FXML private ScrollPane chronologieScrollPane;
    @FXML private VBox chronologieContainer;
    
    // Services
    private User currentUser;
    private WorkflowAnalysisService workflowService;
    private CourrierService courrierService;
    private CotationService cotationService;
    
    // Données
    private List<ServiceHierarchy> servicesAutorises;
    private Map<String, ServiceFlowStats> fluxStats;
    private List<FluxCourrier> fluxCourriers;
    
    // Variables d'état
    private boolean modeIndividuel = false;
    private Courrier courrierSelectionne = null;
    
    // Constantes de dessin
    private static final double NODE_WIDTH = 150;
    private static final double NODE_HEIGHT = 60;
    private static final double VERTICAL_SPACING = 120;
    private static final double HORIZONTAL_SPACING = 300;
    private static final double MIN_ARROW_WIDTH = 2;
    private static final double MAX_ARROW_WIDTH = 20;
    
    // Zoom
    private Scale scaleTransform;
    private double currentZoom = 1.0;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("=== WorkflowSuiviController.initialize() - VERSION AMÉLIORÉE ===");
        
        try {
            // Initialiser les services
            currentUser = SessionManager.getInstance().getCurrentUser();
            workflowService = WorkflowAnalysisService.getInstance();
            courrierService = CourrierService.getInstance();
            cotationService = CotationService.getInstance();
            
            if (currentUser == null) {
                AlertUtils.showError("Aucun utilisateur connecté");
                return;
            }
            
            System.out.println("✓ Utilisateur connecté: " + currentUser.getNomComplet());
            
            // Charger les services autorisés
            loadServicesAutorises();
            
            // Initialiser les composants
            initializeComponents();
            
            // Charger les données initiales
            loadInitialData();
            
            System.out.println("✓ WorkflowSuiviController initialisé avec succès");
            
        } catch (Exception e) {
            System.err.println("❌ Erreur initialisation: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur d'initialisation: " + e.getMessage());
        }
    }
    
    /**
     * Initialise les composants de l'interface
     */
    private void initializeComponents() {
        // Types de flux
        if (cbTypeFlux != null) {
            cbTypeFlux.getItems().addAll(
                "Tous les flux",
                "Flux entrants uniquement",
                "Flux sortants uniquement",
                "Flux internes uniquement"
            );
            cbTypeFlux.setValue("Tous les flux");
            cbTypeFlux.setOnAction(e -> {
                if (!modeIndividuel) regenerateGraph();
            });
        }
        
        // Dates par défaut
        if (dpFin != null) {
            dpFin.setValue(LocalDate.now());
            dpFin.setOnAction(e -> {
                if (!modeIndividuel) {
                    regenerateGraph();
                } else {
                    loadCourriersList();
                }
            });
        }
        
        if (dpDebut != null) {
            dpDebut.setValue(LocalDate.now().minusMonths(1));
            dpDebut.setOnAction(e -> {
                if (!modeIndividuel) {
                    regenerateGraph();
                } else {
                    loadCourriersList();
                }
            });
        }
        
        // Checkboxes
        if (chkAfficherStatistiques != null) {
            chkAfficherStatistiques.setSelected(true);
            chkAfficherStatistiques.setOnAction(e -> updateStatisticsVisibility());
        }
        
        if (chkAfficherGoulots != null) {
            chkAfficherGoulots.setSelected(true);
            chkAfficherGoulots.setOnAction(e -> regenerateGraph());
        }
        
        // Zoom
        setupZoom();
        
        // Configuration du graphPane
        if (graphPane != null) {
            graphPane.setMinSize(2000, 1500);
            graphPane.setStyle("-fx-background-color: #f8f9fa;");
        }
        
        // Configuration de la table
        setupTable();
        
        // Configuration du mode de visualisation
        setupModeToggle();
        
        // Charger la liste des courriers
        loadCourriersList();
        
        // Configuration du bouton de recherche
        if (btnRechercherCourrier != null) {
            btnRechercherCourrier.setOnAction(e -> openCourrierSearchDialog());
        }
        
        // Configuration de la sélection de courrier
        if (cbCourrierSelection != null) {
            cbCourrierSelection.setOnAction(e -> {
                CourrierItem selected = cbCourrierSelection.getValue();
                if (selected != null && modeIndividuel) {
                    loadCourrierIndividuel(selected.getCourrierId());
                }
            });
        }
        
        System.out.println("✓ Composants initialisés");
    }
    
    /**
     * Configure le système de zoom
     */
    private void setupZoom() {
        if (sliderZoom != null && graphPane != null) {
            scaleTransform = new Scale(1.0, 1.0);
            graphPane.getTransforms().add(scaleTransform);
            
            sliderZoom.setMin(0.25);
            sliderZoom.setMax(3.0);
            sliderZoom.setValue(1.0);
            sliderZoom.setShowTickMarks(true);
            sliderZoom.setShowTickLabels(true);
            sliderZoom.setMajorTickUnit(0.5);
            
            sliderZoom.valueProperty().addListener((obs, oldVal, newVal) -> {
                currentZoom = newVal.doubleValue();
                scaleTransform.setX(currentZoom);
                scaleTransform.setY(currentZoom);
                graphPane.setMinWidth(2000 * currentZoom);
                graphPane.setMinHeight(1500 * currentZoom);
                
                if (lblZoomValue != null) {
                    lblZoomValue.setText(String.format("%.0f%%", currentZoom * 100));
                }
            });
        }
        
        // Zoom avec la molette
        if (graphScrollPane != null) {
            graphScrollPane.setOnScroll(event -> {
                if (event.isControlDown()) {
                    event.consume();
                    double delta = event.getDeltaY() > 0 ? 0.1 : -0.1;
                    double newZoom = Math.max(0.25, Math.min(3.0, currentZoom + delta));
                    sliderZoom.setValue(newZoom);
                }
            });
        }
    }
    
    /**
     * Configure la table des détails
     */
    private void setupTable() {
        if (tableFluxDetails == null) return;
        
        colService.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getServiceName()));
        
        colEntrants.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleIntegerProperty(data.getValue().getFluxEntrants()));
        
        colSortants.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleIntegerProperty(data.getValue().getFluxSortants()));
        
        colInternes.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleIntegerProperty(data.getValue().getFluxInternes()));
        
        colDuree.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getDureeMoyenneFormatee()));
        
        colStatut.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getStatutDescription()));
        
        // Colorier la colonne statut
        colStatut.setCellFactory(column -> new TableCell<ServiceFlowStats, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    ServiceFlowStats stats = getTableView().getItems().get(getIndex());
                    if (stats.estGoulot()) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else if (stats.getScorePerformance() >= 80) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #f39c12;");
                    }
                }
            }
        });
        
        // Double-clic sur une ligne pour voir les détails du service
        tableFluxDetails.setRowFactory(tv -> {
            TableRow<ServiceFlowStats> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    ServiceFlowStats stats = row.getItem();
                    showServiceDetails(stats);
                }
            });
            return row;
        });
    }
    
    /**
     * Charge les services autorisés selon le niveau hiérarchique
     */
    private void loadServicesAutorises() {
        servicesAutorises = new ArrayList<>();
        int niveauAutorite = currentUser.getNiveauAutorite();
        
        if (niveauAutorite == 0) {
            // Niveau 0 : voir tout
            servicesAutorises.addAll(workflowService.getAllServices());
        } else if (niveauAutorite >= 1) {
            // Autres niveaux : voir sa hiérarchie
            String serviceCode = currentUser.getServiceCode();
            if (serviceCode != null) {
                ServiceHierarchy userService = workflowService.getServiceByCode(serviceCode);
                if (userService != null) {
                    servicesAutorises.add(userService);
                    servicesAutorises.addAll(userService.getTousLesDescendants());
                }
            }
        }
        
        System.out.println("✓ " + servicesAutorises.size() + " services autorisés");
    }
    
    /**
     * Configure le toggle entre mode collectif et individuel
     */
    private void setupModeToggle() {
        if (modeToggleGroup != null) {
            modeToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
                if (newToggle == rbModeIndividuel) {
                    switchToModeIndividuel();
                } else {
                    switchToModeCollectif();
                }
            });
        }
    }
    
    /**
     * Bascule vers le mode collectif
     */
    private void switchToModeCollectif() {
        System.out.println("🔄 Passage en mode COLLECTIF");
        modeIndividuel = false;
        
        // Afficher/masquer les contrôles appropriés
        if (courrierSelectionBox != null) {
            courrierSelectionBox.setDisable(true);
        }
        
        if (controlesModeCollectif != null) {
            controlesModeCollectif.setVisible(true);
            controlesModeCollectif.setManaged(true);
        }
        
        if (infoCourrierIndividuel != null) {
            infoCourrierIndividuel.setVisible(false);
            infoCourrierIndividuel.setManaged(false);
        }
        
        // Table visible, chronologie cachée
        if (tableFluxDetails != null) {
            tableFluxDetails.setVisible(true);
            tableFluxDetails.setManaged(true);
        }
        
        if (chronologieScrollPane != null) {
            chronologieScrollPane.setVisible(false);
            chronologieScrollPane.setManaged(false);
        }
        
        // Mettre à jour les labels
        updateLabelsForModeCollectif();
        
        // Régénérer le graphe collectif
        regenerateGraph();
    }
    
    /**
     * Bascule vers le mode individuel
     */
    private void switchToModeIndividuel() {
        System.out.println("🔄 Passage en mode INDIVIDUEL");
        modeIndividuel = true;
        
        // Afficher/masquer les contrôles appropriés
        if (courrierSelectionBox != null) {
            courrierSelectionBox.setDisable(false);
        }
        
        if (controlesModeCollectif != null) {
            controlesModeCollectif.setVisible(false);
            controlesModeCollectif.setManaged(false);
        }
        
        if (infoCourrierIndividuel != null) {
            infoCourrierIndividuel.setVisible(true);
            infoCourrierIndividuel.setManaged(true);
        }
        
        // Table cachée, chronologie visible
        if (tableFluxDetails != null) {
            tableFluxDetails.setVisible(false);
            tableFluxDetails.setManaged(false);
        }
        
        if (chronologieScrollPane != null) {
            chronologieScrollPane.setVisible(true);
            chronologieScrollPane.setManaged(true);
        }
        
        // Mettre à jour les labels
        updateLabelsForModeIndividuel();
        
        // Charger la liste des courriers si pas déjà fait
        loadCourriersList();
        
        // Effacer le graphe
        if (graphPane != null) {
            graphPane.getChildren().clear();
            showSelectCourrierMessage();
        }
    }
    
    /**
     * Met à jour les labels pour le mode collectif
     */
    private void updateLabelsForModeCollectif() {
        if (lblModeActif != null) {
            lblModeActif.setText("📊 MODE: VUE COLLECTIVE");
            lblModeActif.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #3498db;");
        }
        
        if (lblStatsTitre != null) {
            lblStatsTitre.setText("📈 STATISTIQUES GLOBALES");
        }
        
        if (lblDetailsTitle != null) {
            lblDetailsTitle.setText("📋 DÉTAILS PAR SERVICE");
        }
        
        if (lblStatus != null) {
            lblStatus.setText("ℹ️ Vue collective de tous les flux de courriers");
        }
        
        if (statTotalCourrierLabel != null) statTotalCourrierLabel.setText("Courriers");
        if (statServicesLabel != null) statServicesLabel.setText("Services");
        if (statDureeLabel != null) statDureeLabel.setText("Durée moy.");
        if (statGoulotsLabel != null) statGoulotsLabel.setText("Goulots");
        
        if (lblInfo1 != null) lblInfo1.setText("• Survolez les nœuds pour voir les détails des services");
        if (lblInfo2 != null) lblInfo2.setText("• La largeur des flèches = volume de courriers");
        if (lblInfo3 != null) lblInfo3.setText("• Ctrl + Molette pour zoomer, Double-clic pour détails service");
    }
    
    /**
     * Met à jour les labels pour le mode individuel
     */
    private void updateLabelsForModeIndividuel() {
        if (lblModeActif != null) {
            lblModeActif.setText("🔍 MODE: VUE INDIVIDUELLE");
            lblModeActif.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #9b59b6;");
        }
        
        if (lblStatsTitre != null) {
            lblStatsTitre.setText("📈 DÉTAILS DU COURRIER");
        }
        
        if (lblDetailsTitle != null) {
            lblDetailsTitle.setText("⏱️ CHRONOLOGIE DU PARCOURS");
        }
        
        if (lblStatus != null) {
            lblStatus.setText("ℹ️ Sélectionnez un courrier pour voir son parcours");
        }
        
        if (statTotalCourrierLabel != null) statTotalCourrierLabel.setText("Étapes");
        if (statServicesLabel != null) statServicesLabel.setText("Services visités");
        if (statDureeLabel != null) statDureeLabel.setText("Durée totale");
        if (statGoulotsLabel != null) statGoulotsLabel.setText("Retards");
        
        if (lblInfo1 != null) lblInfo1.setText("• Parcours chronologique du courrier");
        if (lblInfo2 != null) lblInfo2.setText("• Chaque nœud = une étape de traitement");
        if (lblInfo3 != null) lblInfo3.setText("• Les durées sont affichées sur les transitions");
    }
    
    /**
     * Charge la liste des courriers dans le ComboBox
     * CORRIGÉ : Utilise getDateCreation() au lieu de getDateReception()
     */
    private void loadCourriersList() {
        if (cbCourrierSelection == null) return;
        
        System.out.println("📋 Chargement de la liste des courriers...");
        
        LocalDateTime debut = dpDebut != null && dpDebut.getValue() != null ? 
            dpDebut.getValue().atStartOfDay() : LocalDateTime.now().minusMonths(3);
        LocalDateTime fin = dpFin != null && dpFin.getValue() != null ? 
            dpFin.getValue().atTime(23, 59, 59) : LocalDateTime.now();
        
        // CORRECTION: Utilise getDateCreation() au lieu de getDateReception()
        List<Courrier> courriers = courrierService.getAllCourriers().stream()
            .filter(c -> c.getDateCreation() != null)
            .filter(c -> !c.getDateCreation().isBefore(debut))
            .filter(c -> !c.getDateCreation().isAfter(fin))
            .filter(c -> !workflowService.getCourrierParcours(c.getId()).isEmpty())
            .sorted((a, b) -> b.getDateCreation().compareTo(a.getDateCreation()))
            .collect(Collectors.toList());
        
        // Convertir en CourrierItem pour affichage
        List<CourrierItem> items = courriers.stream()
            .map(c -> new CourrierItem(
                c.getId(),
                c.getCodeCourrier(),
                c.getObjet(),
                TypeCourrier.fromString(c.getTypeCourrier()),
                c.getDateCreation()
            ))
            .collect(Collectors.toList());
        
        cbCourrierSelection.getItems().clear();
        cbCourrierSelection.getItems().addAll(items);
        
        System.out.println("✓ " + items.size() + " courriers chargés");
    }
    
    /**
     * Charge et affiche le parcours d'un courrier spécifique
     */
    private void loadCourrierIndividuel(int courrierId) {
        System.out.println("📧 Chargement du courrier ID: " + courrierId);
        
        try {
            // Récupérer le courrier
            courrierSelectionne = courrierService.getCourrierById(courrierId);
            
            if (courrierSelectionne == null) {
                AlertUtils.showError("Courrier non trouvé", "Impossible de charger le courrier #" + courrierId);
                return;
            }
            
            // Récupérer l'historique du workflow
            List<WorkflowStep> steps = workflowService.getCourrierParcours(courrierId);
            
            // Récupérer les cotations associées
            List<CotationCourrier> cotations = cotationService.getCotationsByCourrier(courrierId);
            
            if (steps.isEmpty() && cotations.isEmpty()) {
                AlertUtils.showWarning("Aucun parcours", "Ce courrier n'a pas encore d'historique de workflow ou de cotations");
                return;
            }
            
            // Mettre à jour les informations du courrier
            updateCourrierInfo(courrierSelectionne, steps, cotations);
            
            // Dessiner le parcours
            drawCourrierParcours(courrierSelectionne, steps, cotations);
            
            // Afficher la chronologie
            displayChronologie(steps, cotations);
            
            // Mettre à jour les statistiques
            updateStatsForCourrierIndividuel(steps, cotations);
            
            System.out.println("✓ Courrier chargé: " + courrierSelectionne.getCodeCourrier());
            
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement courrier: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur", "Erreur lors du chargement du courrier: " + e.getMessage());
        }
    }
    
    /**
     * Met à jour les informations affichées du courrier
     */
    private void updateCourrierInfo(Courrier courrier, List<WorkflowStep> steps, List<CotationCourrier> cotations) {
        if (lblCourrierNumero != null) {
            lblCourrierNumero.setText("📧 Courrier: " + courrier.getCodeCourrier());
        }
        
        if (lblCourrierObjet != null) {
            String objet = courrier.getObjet();
            if (objet.length() > 40) objet = objet.substring(0, 37) + "...";
            lblCourrierObjet.setText("Objet: " + objet);
        }
        
        if (lblCourrierType != null) {
            String typeCourrier = courrier.getTypeCourrier();
            String icon = "ENTRANT".equalsIgnoreCase(typeCourrier) ? "📥" :
                         "SORTANT".equalsIgnoreCase(typeCourrier) ? "📤" : "📄";
            lblCourrierType.setText(icon + " " + typeCourrier);
        }
        
        if (lblCourrierDate != null) {
            lblCourrierDate.setText("Date: " + courrier.getDateCreation().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            ));
        }
        
        if (lblCourrierStatut != null) {
            String statut = courrier.getStatut();
            String couleur = "traite".equalsIgnoreCase(statut) ? "#27ae60" : "#f39c12";
            lblCourrierStatut.setText("Statut: " + statut);
            lblCourrierStatut.setStyle("-fx-font-size: 12px; -fx-text-fill: " + couleur + "; -fx-font-weight: bold;");
        }
    }
    
    /**
     * Dessine le parcours linéaire d'un courrier avec workflow et cotations intégrés
     */
    private void drawCourrierParcours(Courrier courrier, List<WorkflowStep> steps, List<CotationCourrier> cotations) {
        if (graphPane == null) return;
        
        Platform.runLater(() -> {
            graphPane.getChildren().clear();
            
            if (steps.isEmpty() && cotations.isEmpty()) {
                showEmptyGraphMessage();
                return;
            }
            
            System.out.println("🎨 Dessin du parcours: " + steps.size() + " étapes workflow, " + 
                             cotations.size() + " cotations");
            
            // Créer une liste combinée d'événements chronologiques
            List<EvenementParcours> evenements = new ArrayList<>();
            
            // Ajouter les étapes de workflow
            for (int i = 0; i < steps.size(); i++) {
                WorkflowStep step = steps.get(i);
                evenements.add(new EvenementParcours(
                    step.getDateAction(),
                    "WORKFLOW",
                    step.getServiceCode(),
                    step.getAction(),
                    step.getStatutEtape(),
                    step
                ));
            }
            
            // Ajouter les cotations
            for (CotationCourrier cotation : cotations) {
                evenements.add(new EvenementParcours(
                    cotation.getDateCotation(),
                    "COTATION",
                    cotation.getServiceDestination(),
                    "Cotation à " + cotation.getAssigneNom(),
                    null,
                    cotation
                ));
            }
            
            // Trier par date
            evenements.sort(Comparator.comparing(EvenementParcours::getDate));
            
            // Disposer les événements horizontalement
            double startX = 150;
            double startY = 400;
            double horizontalSpacing = 250;
            
            List<VBox> nodes = new ArrayList<>();
            
            // Créer les nœuds pour chaque événement
            for (int i = 0; i < evenements.size(); i++) {
                EvenementParcours event = evenements.get(i);
                double x = startX + i * horizontalSpacing;
                
                VBox node = createEventNode(event, i + 1, x, startY);
                nodes.add(node);
                graphPane.getChildren().add(node);
            }
            
            // Dessiner les connexions entre les événements
            for (int i = 0; i < evenements.size() - 1; i++) {
                EvenementParcours currentEvent = evenements.get(i);
                EvenementParcours nextEvent = evenements.get(i + 1);
                
                double x1 = startX + i * horizontalSpacing + NODE_WIDTH;
                double y1 = startY + NODE_HEIGHT / 2;
                double x2 = startX + (i + 1) * horizontalSpacing;
                double y2 = startY + NODE_HEIGHT / 2;
                
                // Calculer la durée
                long heures = java.time.Duration.between(
                    currentEvent.getDate(),
                    nextEvent.getDate()
                ).toHours();
                
                boolean enRetard = currentEvent.getType().equals("WORKFLOW") && 
                                  currentEvent.getWorkflowStep() != null && 
                                  currentEvent.getWorkflowStep().isEnRetard();
                
                drawEventConnection(x1, y1, x2, y2, heures, enRetard);
            }
            
            System.out.println("✓ Parcours dessiné avec " + evenements.size() + " événements");
        });
    }
    
    /**
     * Crée un nœud visuel pour un événement (workflow ou cotation)
     */
    private VBox createEventNode(EvenementParcours event, int numero, double x, double y) {
        VBox node = new VBox(8);
        node.setLayoutX(x);
        node.setLayoutY(y);
        node.setPrefWidth(NODE_WIDTH);
        node.setMinHeight(NODE_HEIGHT + 20);
        node.setAlignment(Pos.CENTER);
        node.setPadding(new Insets(15));
        
        // Couleur selon le type et le statut
        String borderColor = "#3498db";
        String backgroundColor = "#ffffff";
        
        if (event.getType().equals("COTATION")) {
            CotationCourrier cotation = event.getCotation();
            borderColor = "#9b59b6";
            backgroundColor = "#f5eef8";
            
            if (cotation.getStatut().equals("traite")) {
                borderColor = "#27ae60";
                backgroundColor = "#e8f8f5";
            } else if (cotation.isEnRetard()) {
                borderColor = "#e74c3c";
                backgroundColor = "#fde6e6";
            }
        } else {
            WorkflowStep step = event.getWorkflowStep();
            if (step.getStatutEtape() == StatutEtapeWorkflow.TERMINE) {
                borderColor = "#27ae60";
                backgroundColor = "#e8f8f5";
            } else if (step.getStatutEtape() == StatutEtapeWorkflow.EN_COURS) {
                borderColor = "#f39c12";
                backgroundColor = "#fef5e7";
            } else if (step.isEnRetard()) {
                borderColor = "#e74c3c";
                backgroundColor = "#fde6e6";
            }
        }
        
        node.setStyle(
            "-fx-background-color: " + backgroundColor + ";" +
            "-fx-border-color: " + borderColor + ";" +
            "-fx-border-width: 3;" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 4);"
        );
        
        // Type d'événement
        String typeIcon = event.getType().equals("COTATION") ? "📋" : "📊";
        Label typeLabel = new Label(typeIcon + " " + event.getType());
        typeLabel.setFont(Font.font("System", FontWeight.BOLD, 10));
        typeLabel.setStyle("-fx-text-fill: " + borderColor + ";");
        
        // Numéro
        Label numLabel = new Label("#" + numero);
        numLabel.setFont(Font.font("System", FontWeight.BOLD, 11));
        numLabel.setStyle("-fx-text-fill: " + borderColor + ";");
        
        // Service
        ServiceHierarchy service = workflowService.getServiceByCode(event.getServiceCode());
        String serviceName = service != null ? service.getServiceName() : event.getServiceCode();
        if (serviceName.length() > 18) serviceName = serviceName.substring(0, 15) + "...";
        
        Label serviceIcon = new Label(service != null ? service.getIcone() : "🏢");
        serviceIcon.setFont(Font.font(22));
        
        Label nameLabel = new Label(serviceName);
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        nameLabel.setStyle("-fx-text-fill: #2c3e50;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(NODE_WIDTH - 20);
        
        // Action
        String action = event.getAction();
        if (action.length() > 20) action = action.substring(0, 17) + "...";
        Label actionLabel = new Label(action);
        actionLabel.setFont(Font.font(9));
        actionLabel.setStyle("-fx-text-fill: #7f8c8d;");
        
        // Date
        Label dateLabel = new Label(event.getDate().format(
            DateTimeFormatter.ofPattern("dd/MM à HH:mm")
        ));
        dateLabel.setFont(Font.font(9));
        dateLabel.setStyle("-fx-text-fill: #7f8c8d;");
        
        node.getChildren().addAll(typeLabel, numLabel, serviceIcon, nameLabel, actionLabel, dateLabel);
        
        // Tooltip détaillé
        String tooltipText = buildEventTooltip(event, service);
        Tooltip tooltip = new Tooltip(tooltipText);
        Tooltip.install(node, tooltip);
        
        // Interactivité
        node.setCursor(Cursor.HAND);
        node.setOnMouseEntered(e -> {
            node.setScaleX(1.08);
            node.setScaleY(1.08);
        });
        node.setOnMouseExited(e -> {
            node.setScaleX(1.0);
            node.setScaleY(1.0);
        });
        
        return node;
    }
    
    /**
     * Construit le tooltip pour un événement
     */
    private String buildEventTooltip(EvenementParcours event, ServiceHierarchy service) {
        StringBuilder tooltip = new StringBuilder();
        
        if (event.getType().equals("WORKFLOW")) {
            WorkflowStep step = event.getWorkflowStep();
            tooltip.append(String.format(
                "🔹 ÉTAPE WORKFLOW\n\n" +
                "Service: %s\n" +
                "Action: %s\n" +
                "Agent: %s\n" +
                "Date: %s\n" +
                "Statut: %s",
                service != null ? service.getServiceName() : step.getServiceCode(),
                step.getAction(),
                step.getUserName() != null ? step.getUserName() : "Non assigné",
                step.getDateAction().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                step.getStatutEtape().name()
            ));
            
            if (step.getCommentaire() != null && !step.getCommentaire().isEmpty()) {
                tooltip.append("\n\n💬 ").append(step.getCommentaire());
            }
            
            if (step.isEnRetard()) {
                tooltip.append("\n\n⚠️ EN RETARD");
            }
        } else {
            CotationCourrier cotation = event.getCotation();
            tooltip.append(String.format(
                "🔹 COTATION\n\n" +
                "Coté par: %s\n" +
                "Assigné à: %s\n" +
                "Service: %s\n" +
                "Date cotation: %s\n" +
                "Échéance: %s\n" +
                "Priorité: %s\n" +
                "Statut: %s",
                cotation.getCoteurNom() != null ? cotation.getCoteurNom() : "Inconnu",
                cotation.getAssigneNom() != null ? cotation.getAssigneNom() : "Inconnu",
                service != null ? service.getServiceName() : cotation.getServiceDestination(),
                cotation.getDateCotation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                cotation.getDateEcheanceFormatee(),
                cotation.getPriorite(),
                cotation.getStatut()
            ));
            
            if (cotation.getCommentaire() != null && !cotation.getCommentaire().isEmpty()) {
                tooltip.append("\n\n💬 ").append(cotation.getCommentaire());
            }
            
            if (cotation.isEnRetard()) {
                tooltip.append("\n\n⚠️ EN RETARD - ").append(cotation.getJoursRetard()).append(" jour(s)");
            }
        }
        
        return tooltip.toString();
    }
    
    /**
     * Dessine une connexion entre deux événements
     */
    private void drawEventConnection(double x1, double y1, double x2, double y2, long heures, boolean enRetard) {
        Group connectionGroup = new Group();
        
        // Ligne
        Line line = new Line(x1, y1, x2, y2);
        line.setStroke(Color.web(enRetard ? "#e74c3c" : "#3498db"));
        line.setStrokeWidth(3);
        
        if (enRetard) {
            line.getStrokeDashArray().addAll(10d, 5d);
        }
        
        // Flèche
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double arrowLength = 12;
        double arrowAngle = Math.PI / 6;
        
        double arrowX1 = x2 - arrowLength * Math.cos(angle - arrowAngle);
        double arrowY1 = y2 - arrowLength * Math.sin(angle - arrowAngle);
        double arrowX2 = x2 - arrowLength * Math.cos(angle + arrowAngle);
        double arrowY2 = y2 - arrowLength * Math.sin(angle + arrowAngle);
        
        Line arrow1 = new Line(x2, y2, arrowX1, arrowY1);
        Line arrow2 = new Line(x2, y2, arrowX2, arrowY2);
        arrow1.setStroke(line.getStroke());
        arrow2.setStroke(line.getStroke());
        arrow1.setStrokeWidth(3);
        arrow2.setStrokeWidth(3);
        
        // Label durée
        Label dureeLabel = new Label(formatDuree(heures));
        dureeLabel.setLayoutX((x1 + x2) / 2 - 25);
        dureeLabel.setLayoutY(y1 - 25);
        dureeLabel.setStyle(
            "-fx-background-color: white; " +
            "-fx-padding: 3 8; " +
            "-fx-border-color: " + (enRetard ? "#e74c3c" : "#3498db") + "; " +
            "-fx-border-width: 2; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 11px;"
        );
        
        connectionGroup.getChildren().addAll(line, arrow1, arrow2, dureeLabel);
        graphPane.getChildren().add(connectionGroup);
    }
    
    /**
     * Affiche la chronologie dans le panneau de droite
     */
    private void displayChronologie(List<WorkflowStep> steps, List<CotationCourrier> cotations) {
        if (chronologieContainer == null) return;
        
        chronologieContainer.getChildren().clear();
        
        // Créer une liste combinée d'événements
        List<EvenementParcours> evenements = new ArrayList<>();
        
        for (WorkflowStep step : steps) {
            evenements.add(new EvenementParcours(
                step.getDateAction(),
                "WORKFLOW",
                step.getServiceCode(),
                step.getAction(),
                step.getStatutEtape(),
                step
            ));
        }
        
        for (CotationCourrier cotation : cotations) {
            evenements.add(new EvenementParcours(
                cotation.getDateCotation(),
                "COTATION",
                cotation.getServiceDestination(),
                "Cotation à " + cotation.getAssigneNom(),
                null,
                cotation
            ));
        }
        
        // Trier par date
        evenements.sort(Comparator.comparing(EvenementParcours::getDate));
        
        for (int i = 0; i < evenements.size(); i++) {
            EvenementParcours event = evenements.get(i);
            
            VBox eventBox = createChronologieEventBox(event, i + 1);
            chronologieContainer.getChildren().add(eventBox);
            
            // Ajouter la durée jusqu'à l'événement suivant
            if (i < evenements.size() - 1) {
                long heures = java.time.Duration.between(
                    event.getDate(),
                    evenements.get(i + 1).getDate()
                ).toHours();
                
                Label dureeLabel = new Label("⏱ " + formatDuree(heures));
                dureeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #f39c12; -fx-font-weight: bold; -fx-padding: 5 0 5 20;");
                chronologieContainer.getChildren().add(dureeLabel);
            }
        }
    }
    
    /**
     * Crée une box pour la chronologie
     */
    private VBox createChronologieEventBox(EvenementParcours event, int numero) {
        VBox eventBox = new VBox(5);
        eventBox.setStyle(
            "-fx-background-color: white; " +
            "-fx-padding: 12; " +
            "-fx-border-color: #e0e0e0; " +
            "-fx-border-width: 1; " +
            "-fx-border-radius: 8; " +
            "-fx-background-radius: 8;"
        );
        
        // En-tête
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label numLabel = new Label(String.valueOf(numero));
        numLabel.setStyle(
            "-fx-background-color: " + (event.getType().equals("COTATION") ? "#9b59b6" : "#3498db") + "; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 5 10; " +
            "-fx-background-radius: 50%;"
        );
        
        Label typeLabel = new Label(event.getType());
        typeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 10px; -fx-text-fill: #7f8c8d;");
        
        Label dateLabel = new Label(event.getDate().format(
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        ));
        dateLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
        
        header.getChildren().addAll(numLabel, typeLabel, dateLabel);
        
        // Détails
        ServiceHierarchy service = workflowService.getServiceByCode(event.getServiceCode());
        Label serviceLabel = new Label((service != null ? service.getIcone() + " " : "") + 
            (service != null ? service.getServiceName() : event.getServiceCode()));
        serviceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        Label actionLabel = new Label("📌 " + event.getAction());
        actionLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
        actionLabel.setWrapText(true);
        
        eventBox.getChildren().addAll(header, serviceLabel, actionLabel);
        
        // Informations spécifiques
        if (event.getType().equals("WORKFLOW")) {
            WorkflowStep step = event.getWorkflowStep();
            
            if (step.getUserName() != null) {
                Label agentLabel = new Label("👤 Agent: " + step.getUserName());
                agentLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #95a5a6;");
                eventBox.getChildren().add(agentLabel);
            }
            
            if (step.getCommentaire() != null && !step.getCommentaire().isEmpty()) {
                Label commentLabel = new Label("💬 " + step.getCommentaire());
                commentLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #34495e; -fx-font-style: italic;");
                commentLabel.setWrapText(true);
                eventBox.getChildren().add(commentLabel);
            }
            
            if (step.isEnRetard()) {
                Label retardLabel = new Label("⚠️ EN RETARD");
                retardLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                eventBox.getChildren().add(retardLabel);
            }
        } else {
            CotationCourrier cotation = event.getCotation();
            
            Label assigneLabel = new Label("👤 Assigné à: " + 
                (cotation.getAssigneNom() != null ? cotation.getAssigneNom() : "Inconnu"));
            assigneLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #95a5a6;");
            eventBox.getChildren().add(assigneLabel);
            
            Label prioriteLabel = new Label("🎯 Priorité: " + cotation.getPriorite());
            prioriteLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #95a5a6;");
            eventBox.getChildren().add(prioriteLabel);
            
            Label echeanceLabel = new Label("📅 Échéance: " + cotation.getDateEcheanceFormatee());
            echeanceLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #95a5a6;");
            eventBox.getChildren().add(echeanceLabel);
            
            if (cotation.isEnRetard()) {
                Label retardLabel = new Label("⚠️ EN RETARD - " + cotation.getJoursRetard() + " jour(s)");
                retardLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                eventBox.getChildren().add(retardLabel);
            }
        }
        
        return eventBox;
    }
    
    /**
     * Met à jour les statistiques pour un courrier individuel
     */
    private void updateStatsForCourrierIndividuel(List<WorkflowStep> steps, List<CotationCourrier> cotations) {
        int totalEtapes = steps.size() + cotations.size();
        
        // Nombre d'étapes
        if (statTotalCourriers != null) {
            statTotalCourriers.setText(String.valueOf(totalEtapes));
        }
        
        // Nombre de services visités
        Set<String> servicesVisites = new HashSet<>();
        steps.forEach(s -> servicesVisites.add(s.getServiceCode()));
        cotations.forEach(c -> {
            if (c.getServiceDestination() != null) {
                servicesVisites.add(c.getServiceDestination());
            }
        });
        
        if (statServicesActifs != null) {
            statServicesActifs.setText(String.valueOf(servicesVisites.size()));
        }
        
        // Durée totale
        if (totalEtapes >= 2 && statDureeMoyenne != null) {
            LocalDateTime debut = steps.isEmpty() ? cotations.get(0).getDateCotation() : steps.get(0).getDateAction();
            LocalDateTime fin = LocalDateTime.now();
            
            // Trouver la date la plus récente
            if (!steps.isEmpty()) {
                fin = steps.get(steps.size() - 1).getDateAction();
            }
            if (!cotations.isEmpty() && cotations.get(cotations.size() - 1).getDateCotation().isAfter(fin)) {
                fin = cotations.get(cotations.size() - 1).getDateCotation();
            }
            
            long heuresTotal = java.time.Duration.between(debut, fin).toHours();
            statDureeMoyenne.setText(formatDuree(heuresTotal));
        }
        
        // Nombre de retards
        long retardsWorkflow = steps.stream().filter(WorkflowStep::isEnRetard).count();
        long retardsCotations = cotations.stream().filter(CotationCourrier::isEnRetard).count();
        long retardsTotal = retardsWorkflow + retardsCotations;
        
        if (statGoulotsDetectes != null) {
            statGoulotsDetectes.setText(String.valueOf(retardsTotal));
            
            if (retardsTotal > 0) {
                statGoulotsDetectes.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
            } else {
                statGoulotsDetectes.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
            }
        }
        
        // Mettre à jour le label du nombre d'étapes
        if (lblNbEtapes != null) {
            lblNbEtapes.setText(totalEtapes + " étapes • " + servicesVisites.size() + " services");
        }
    }
    
    /**
     * Formate une durée en heures
     */
    private String formatDuree(long heures) {
        if (heures < 1) {
            return "< 1h";
        } else if (heures < 24) {
            return heures + "h";
        } else {
            long jours = heures / 24;
            long restHeures = heures % 24;
            return jours + "j " + (restHeures > 0 ? restHeures + "h" : "");
        }
    }
    
    /**
     * Affiche un message pour sélectionner un courrier
     */
    private void showSelectCourrierMessage() {
        VBox messageBox = new VBox(20);
        messageBox.setAlignment(Pos.CENTER);
        messageBox.setLayoutX(1000 - 200);
        messageBox.setLayoutY(750 - 100);
        messageBox.setPrefWidth(400);
        
        Label iconLabel = new Label("🔍");
        iconLabel.setFont(Font.font(64));
        iconLabel.setStyle("-fx-text-fill: #9b59b6;");
        
        Label messageLabel = new Label("Sélectionnez un courrier dans la liste ci-dessus");
        messageLabel.setFont(Font.font(16));
        messageLabel.setStyle("-fx-text-fill: #7f8c8d;");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(380);
        
        Label hintLabel = new Label("pour visualiser son parcours détaillé");
        hintLabel.setFont(Font.font(12));
        hintLabel.setStyle("-fx-text-fill: #95a5a6;");
        
        messageBox.getChildren().addAll(iconLabel, messageLabel, hintLabel);
        graphPane.getChildren().add(messageBox);
    }
    
    /**
     * Ouvre une boîte de dialogue pour rechercher un courrier
     */
    private void openCourrierSearchDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Rechercher un courrier");
        dialog.setHeaderText("Entrez le numéro ou l'objet du courrier");
        dialog.setContentText("Recherche:");
        
        dialog.showAndWait().ifPresent(searchText -> {
            if (searchText.trim().isEmpty()) return;
            
            // Rechercher dans la liste
            for (CourrierItem item : cbCourrierSelection.getItems()) {
                if (item.toString().toLowerCase().contains(searchText.toLowerCase())) {
                    cbCourrierSelection.setValue(item);
                    loadCourrierIndividuel(item.getCourrierId());
                    return;
                }
            }
            
            AlertUtils.showWarning("Non trouvé", "Aucun courrier ne correspond à: " + searchText);
        });
    }
    
    /**
     * Charge les données initiales
     */
    private void loadInitialData() {
        calculateFluxStatistics();
        generateGraph();
        updateStatistics();
        updateTable();
    }
    
    /**
     * SUITE DU CONTRÔLEUR - PARTIE 2
     * Calcule les statistiques des flux (MODE COLLECTIF)
     * CORRIGÉ : Utilise getDateCreation() au lieu de getDateReception()
     */
    private void calculateFluxStatistics() {
        fluxStats = new HashMap<>();
        fluxCourriers = new ArrayList<>();
        
        LocalDateTime debut = dpDebut.getValue() != null ? 
            dpDebut.getValue().atStartOfDay() : LocalDateTime.now().minusMonths(1);
        LocalDateTime fin = dpFin.getValue() != null ? 
            dpFin.getValue().atTime(23, 59, 59) : LocalDateTime.now();
        
        System.out.println("📊 Calcul des statistiques du " + debut + " au " + fin);
        
        // CORRECTION: Utilise getDateCreation() au lieu de getDateReception()
        List<Courrier> courriers = courrierService.getAllCourriers().stream()
            .filter(c -> c.getDateCreation() != null)
            .filter(c -> !c.getDateCreation().isBefore(debut))
            .filter(c -> !c.getDateCreation().isAfter(fin))
            .collect(Collectors.toList());
        
        System.out.println("✓ " + courriers.size() + " courriers dans la période");
        
        // Pour chaque courrier, analyser ses étapes de workflow ET ses cotations
        for (Courrier courrier : courriers) {
            List<WorkflowStep> steps = workflowService.getCourrierParcours(courrier.getId());
            List<CotationCourrier> cotations = cotationService.getCotationsByCourrier(courrier.getId());
            
            if (steps.isEmpty() && cotations.isEmpty()) continue;
            
            TypeCourrier typeCourrier = TypeCourrier.fromString(courrier.getTypeCourrier());
            
            // Analyser les étapes de workflow
            analyzeWorkflowSteps(courrier, steps, typeCourrier);
            
            // Analyser les cotations
            analyzeCotations(courrier, cotations, typeCourrier);
        }
        
        System.out.println("✓ Statistiques calculées pour " + fluxStats.size() + " services");
    }
    
    /**
     * Analyse les étapes de workflow pour les statistiques
     */
    private void analyzeWorkflowSteps(Courrier courrier, List<WorkflowStep> steps, TypeCourrier typeCourrier) {
        for (int i = 0; i < steps.size(); i++) {
            WorkflowStep step = steps.get(i);
            String serviceCode = step.getServiceCode();
            
            // Vérifier si ce service est autorisé
            boolean isAuthorized = servicesAutorises.stream()
                .anyMatch(s -> s.getServiceCode().equals(serviceCode));
            
            if (!isAuthorized) continue;
            
            // Obtenir ou créer les stats pour ce service
            ServiceFlowStats stats = fluxStats.computeIfAbsent(serviceCode, 
                k -> new ServiceFlowStats(serviceCode, getServiceName(serviceCode)));
            
            // Déterminer le type de flux
            if (i == 0) {
                // Première étape = flux entrant
                stats.incrementFluxEntrants();
            } else if (i == steps.size() - 1 && step.getStatutEtape() == StatutEtapeWorkflow.TERMINE) {
                // Dernière étape terminée = flux sortant
                stats.incrementFluxSortants();
            } else {
                // Étape intermédiaire = flux interne
                stats.incrementFluxInternes();
            }
            
            // Calculer la durée de traitement
            if (i < steps.size() - 1) {
                WorkflowStep nextStep = steps.get(i + 1);
                long heures = java.time.Duration.between(
                    step.getDateAction(),
                    nextStep.getDateAction()
                ).toHours();
                
                stats.ajouterDureeTraitement(heures);
                
                // Enregistrer le flux
                FluxCourrier flux = new FluxCourrier(
                    courrier.getId(),
                    courrier.getCodeCourrier(),
                    typeCourrier,
                    serviceCode,
                    nextStep.getServiceCode(),
                    heures,
                    step.getDateAction()
                );
                fluxCourriers.add(flux);
            }
            
            // Détecter les retards
            if (step.isEnRetard()) {
                stats.incrementRetards();
            }
        }
    }
    
    /**
     * Analyse les cotations pour les statistiques
     */
    private void analyzeCotations(Courrier courrier, List<CotationCourrier> cotations, TypeCourrier typeCourrier) {
        for (CotationCourrier cotation : cotations) {
            String serviceCode = cotation.getServiceDestination();
            
            if (serviceCode == null || serviceCode.isEmpty()) continue;
            
            // Vérifier si ce service est autorisé
            boolean isAuthorized = servicesAutorises.stream()
                .anyMatch(s -> s.getServiceCode().equals(serviceCode));
            
            if (!isAuthorized) continue;
            
            // Obtenir ou créer les stats pour ce service
            ServiceFlowStats stats = fluxStats.computeIfAbsent(serviceCode, 
                k -> new ServiceFlowStats(serviceCode, getServiceName(serviceCode)));
            
            // Les cotations sont considérées comme des flux internes
            stats.incrementFluxInternes();
            
            // Calculer la durée de traitement si la cotation est traitée
            if (cotation.getDateTraitement() != null) {
                long heures = java.time.Duration.between(
                    cotation.getDateCotation(),
                    cotation.getDateTraitement()
                ).toHours();
                
                stats.ajouterDureeTraitement(heures);
            } else if (cotation.getDatePriseEnCharge() != null) {
                // Sinon calculer depuis la prise en charge
                long heures = java.time.Duration.between(
                    cotation.getDateCotation(),
                    cotation.getDatePriseEnCharge()
                ).toHours();
                
                stats.ajouterDureeTraitement(heures);
            }
            
            // Détecter les retards
            if (cotation.isEnRetard()) {
                stats.incrementRetards();
            }
        }
    }
    
    /**
     * Obtient le nom d'un service
     */
    private String getServiceName(String serviceCode) {
        ServiceHierarchy service = workflowService.getServiceByCode(serviceCode);
        return service != null ? service.getServiceName() : serviceCode;
    }
    
    /**
     * Génère le graphe de visualisation
     */
    private void generateGraph() {
        if (graphPane == null) return;
        
        Platform.runLater(() -> {
            graphPane.getChildren().clear();
            
            try {
                // Filtrer les flux selon le type sélectionné
                String typeFlux = cbTypeFlux.getValue();
                List<FluxCourrier> fluxFiltres = filterFluxByType(fluxCourriers, typeFlux);
                
                if (fluxFiltres.isEmpty()) {
                    showEmptyGraphMessage();
                    return;
                }
                
                // Identifier les services uniques impliqués
                Set<String> servicesImpliques = new HashSet<>();
                for (FluxCourrier flux : fluxFiltres) {
                    servicesImpliques.add(flux.getServiceSource());
                    servicesImpliques.add(flux.getServiceDestination());
                }
                
                // Filtrer pour garder uniquement les services autorisés
                servicesImpliques = servicesImpliques.stream()
                    .filter(code -> servicesAutorises.stream()
                        .anyMatch(s -> s.getServiceCode().equals(code)))
                    .collect(Collectors.toSet());
                
                // Calculer les positions des nœuds
                Map<String, Point2D> positions = calculateNodePositions(new ArrayList<>(servicesImpliques));
                
                // Dessiner les flux (arêtes)
                drawFlows(fluxFiltres, positions, typeFlux);
                
                // Dessiner les nœuds
                drawNodes(servicesImpliques, positions);
                
                System.out.println("✓ Graphe généré avec " + servicesImpliques.size() + " services");
                
            } catch (Exception e) {
                System.err.println("❌ Erreur génération graphe: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Filtre les flux selon le type sélectionné
     */
    private List<FluxCourrier> filterFluxByType(List<FluxCourrier> flux, String typeFlux) {
        if (typeFlux == null || typeFlux.equals("Tous les flux")) {
            return new ArrayList<>(flux);
        }
        
        return flux.stream().filter(f -> {
            switch (typeFlux) {
                case "Flux entrants uniquement":
                    return f.getTypeCourrier() == TypeCourrier.ENTRANT;
                case "Flux sortants uniquement":
                    return f.getTypeCourrier() == TypeCourrier.SORTANT;
                case "Flux internes uniquement":
                    return f.getTypeCourrier() == TypeCourrier.INTERNE;
                default:
                    return true;
            }
        }).collect(Collectors.toList());
    }
    
    /**
     * Calcule les positions des nœuds dans le graphe
     */
    private Map<String, Point2D> calculateNodePositions(List<String> serviceCodes) {
        Map<String, Point2D> positions = new HashMap<>();
        
        // Grouper les services par niveau hiérarchique
        Map<Integer, List<String>> parNiveau = serviceCodes.stream()
            .collect(Collectors.groupingBy(code -> {
                ServiceHierarchy service = workflowService.getServiceByCode(code);
                return service != null ? service.getNiveau() : 999;
            }));
        
        double startY = 150;
        int niveauIndex = 0;
        
        List<Integer> niveaux = new ArrayList<>(parNiveau.keySet());
        Collections.sort(niveaux);
        
        for (Integer niveau : niveaux) {
            List<String> services = parNiveau.get(niveau);
            
            double totalWidth = services.size() * HORIZONTAL_SPACING;
            double startX = Math.max(200, (2000 - totalWidth) / 2);
            
            for (int i = 0; i < services.size(); i++) {
                String serviceCode = services.get(i);
                double x = startX + i * HORIZONTAL_SPACING;
                double y = startY + niveauIndex * VERTICAL_SPACING;
                
                positions.put(serviceCode, new Point2D(x, y));
            }
            
            niveauIndex++;
        }
        
        return positions;
    }
    
    /**
     * Dessine les flux entre services
     */
    private void drawFlows(List<FluxCourrier> flux, Map<String, Point2D> positions, String typeFlux) {
        // Regrouper les flux par paire source-destination
        Map<String, List<FluxCourrier>> fluxGroupes = flux.stream()
            .collect(Collectors.groupingBy(f -> f.getServiceSource() + "->" + f.getServiceDestination()));
        
        for (Map.Entry<String, List<FluxCourrier>> entry : fluxGroupes.entrySet()) {
            List<FluxCourrier> fluxGroupe = entry.getValue();
            FluxCourrier premier = fluxGroupe.get(0);
            
            Point2D posSource = positions.get(premier.getServiceSource());
            Point2D posDest = positions.get(premier.getServiceDestination());
            
            if (posSource == null || posDest == null) continue;
            
            // Calculer l'épaisseur selon le nombre de courriers
            int nombreCourriers = fluxGroupe.size();
            double epaisseur = calculateArrowWidth(nombreCourriers);
            
            // Déterminer la couleur selon le type
            String couleur = getFlowColor(premier.getTypeCourrier(), typeFlux);
            
            // Calculer la durée moyenne
            double dureeMoyenne = fluxGroupe.stream()
                .mapToLong(FluxCourrier::getDureeHeures)
                .average()
                .orElse(0);
            
            // Dessiner la flèche
            drawCurvedArrow(posSource, posDest, epaisseur, couleur, nombreCourriers, dureeMoyenne);
        }
    }
    
    /**
     * Calcule la largeur de la flèche selon le nombre de courriers
     */
    private double calculateArrowWidth(int nombreCourriers) {
        if (nombreCourriers <= 1) return MIN_ARROW_WIDTH;
        if (nombreCourriers >= 50) return MAX_ARROW_WIDTH;
        
        return MIN_ARROW_WIDTH + (MAX_ARROW_WIDTH - MIN_ARROW_WIDTH) * 
               Math.log(nombreCourriers) / Math.log(50);
    }
    
    /**
     * Obtient la couleur du flux selon le type
     */
    private String getFlowColor(TypeCourrier type, String filtreType) {
        if (filtreType != null && filtreType.contains("entrants")) {
            return "#e67e22"; // Orange
        } else if (filtreType != null && filtreType.contains("sortants")) {
            return "#3498db"; // Bleu
        } else if (filtreType != null && filtreType.contains("internes")) {
            return "#95a5a6"; // Gris
        }
        
        // Couleur par défaut selon le type
        switch (type) {
            case ENTRANT: return "#e67e22"; // Orange
            case SORTANT: return "#3498db"; // Bleu
            case INTERNE: return "#95a5a6"; // Gris
            default: return "#34495e";
        }
    }
    
    /**
     * Dessine une flèche courbe entre deux points
     */
    private void drawCurvedArrow(Point2D start, Point2D end, double width, String color, 
                                 int count, double duree) {
        Group arrowGroup = new Group();
        
        // Calcul du point de contrôle pour la courbe de Bézier
        double midX = (start.getX() + end.getX()) / 2;
        double midY = (start.getY() + end.getY()) / 2;
        
        // Décalage perpendiculaire pour créer la courbe
        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        double length = Math.sqrt(dx * dx + dy * dy);
        double curvature = 0.2; // 20% de courbure
        
        double controlX = midX - curvature * length * (dy / length);
        double controlY = midY + curvature * length * (dx / length);
        
        // Créer le chemin courbe
        Path path = new Path();
        path.getElements().add(new MoveTo(start.getX() + NODE_WIDTH, start.getY() + NODE_HEIGHT / 2));
        path.getElements().add(new QuadCurveTo(
            controlX, controlY,
            end.getX(), end.getY() + NODE_HEIGHT / 2
        ));
        
        path.setStroke(Color.web(color));
        path.setStrokeWidth(width);
        path.setFill(null);
        path.setOpacity(0.7);
        
        // Ajouter une lueur si le flux est important
        if (count > 20) {
            javafx.scene.effect.Glow glow = new javafx.scene.effect.Glow();
            glow.setLevel(0.5);
            path.setEffect(glow);
        }
        
        arrowGroup.getChildren().add(path);
        
        // Ajouter le label avec le nombre si > 5
        if (count > 5) {
            Label countLabel = new Label(String.valueOf(count));
            countLabel.setLayoutX(controlX - 15);
            countLabel.setLayoutY(controlY - 25);
            countLabel.setStyle(
                "-fx-background-color: white; " +
                "-fx-padding: 3 8; " +
                "-fx-border-color: " + color + "; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 10; " +
                "-fx-background-radius: 10; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 11px;"
            );
            arrowGroup.getChildren().add(countLabel);
        }
        
        // Tooltip avec détails
        String tooltipText = String.format(
            "%d courrier%s\nDurée moyenne: %.1fh",
            count, count > 1 ? "s" : "", duree
        );
        Tooltip tooltip = new Tooltip(tooltipText);
        Tooltip.install(path, tooltip);
        
        // Interactivité
        path.setCursor(Cursor.HAND);
        path.setOnMouseEntered(e -> {
            path.setStrokeWidth(width * 1.5);
            path.setOpacity(1.0);
        });
        path.setOnMouseExited(e -> {
            path.setStrokeWidth(width);
            path.setOpacity(0.7);
        });
        
        graphPane.getChildren().add(arrowGroup);
    }
    
    /**
     * Dessine les nœuds représentant les services
     */
    private void drawNodes(Set<String> serviceCodes, Map<String, Point2D> positions) {
        for (String serviceCode : serviceCodes) {
            Point2D pos = positions.get(serviceCode);
            if (pos == null) continue;
            
            ServiceHierarchy service = workflowService.getServiceByCode(serviceCode);
            ServiceFlowStats stats = fluxStats.get(serviceCode);
            
            if (service == null) continue;
            
            VBox nodeBox = createServiceNode(service, stats, pos.getX(), pos.getY());
            graphPane.getChildren().add(nodeBox);
        }
    }
    
    /**
     * SUITE DANS LE PROCHAIN FICHIER...
     */
    
    /**
     * PARTIE 3 - MÉTHODES FINALES ET CLASSES INTERNES
     * Crée un nœud visuel pour un service
     */
    private VBox createServiceNode(ServiceHierarchy service, ServiceFlowStats stats, double x, double y) {
        VBox node = new VBox(8);
        node.setLayoutX(x);
        node.setLayoutY(y);
        node.setPrefWidth(NODE_WIDTH);
        node.setMinHeight(NODE_HEIGHT);
        node.setAlignment(Pos.CENTER);
        node.setPadding(new Insets(12));
        
        // Style selon les performances
        String borderColor = "#3498db";
        String backgroundColor = "#ffffff";
        
        if (stats != null) {
            if (stats.estGoulot() && chkAfficherGoulots.isSelected()) {
                borderColor = "#e74c3c";
                backgroundColor = "#fde6e6";
            } else if (stats.getScorePerformance() >= 80) {
                borderColor = "#27ae60";
                backgroundColor = "#e8f8f5";
            } else if (stats.getScorePerformance() < 60) {
                borderColor = "#f39c12";
                backgroundColor = "#fef5e7";
            }
        }
        
        node.setStyle(
            "-fx-background-color: " + backgroundColor + ";" +
            "-fx-border-color: " + borderColor + ";" +
            "-fx-border-width: 3;" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 8, 0, 0, 3);"
        );
        
        // Icône
        Label iconLabel = new Label(service.getIcone());
        iconLabel.setFont(Font.font(24));
        
        // Code du service
        Label codeLabel = new Label(service.getServiceCode());
        codeLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        codeLabel.setStyle("-fx-text-fill: #2c3e50;");
        
        // Nom du service (tronqué si trop long)
        String serviceName = service.getServiceName();
        if (serviceName.length() > 20) {
            serviceName = serviceName.substring(0, 17) + "...";
        }
        Label nameLabel = new Label(serviceName);
        nameLabel.setFont(Font.font(10));
        nameLabel.setStyle("-fx-text-fill: #7f8c8d;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(NODE_WIDTH - 20);
        
        node.getChildren().addAll(iconLabel, codeLabel, nameLabel);
        
        // Statistiques si disponibles
        if (stats != null) {
            HBox statsBox = new HBox(8);
            statsBox.setAlignment(Pos.CENTER);
            
            if (stats.getFluxEntrants() > 0) {
                Label entrants = new Label("↓" + stats.getFluxEntrants());
                entrants.setStyle("-fx-font-size: 9px; -fx-text-fill: #e67e22; -fx-font-weight: bold;");
                statsBox.getChildren().add(entrants);
            }
            
            if (stats.getFluxSortants() > 0) {
                Label sortants = new Label("↑" + stats.getFluxSortants());
                sortants.setStyle("-fx-font-size: 9px; -fx-text-fill: #3498db; -fx-font-weight: bold;");
                statsBox.getChildren().add(sortants);
            }
            
            if (stats.getFluxInternes() > 0) {
                Label internes = new Label("↔" + stats.getFluxInternes());
                internes.setStyle("-fx-font-size: 9px; -fx-text-fill: #95a5a6; -fx-font-weight: bold;");
                statsBox.getChildren().add(internes);
            }
            
            if (!statsBox.getChildren().isEmpty()) {
                node.getChildren().add(statsBox);
            }
        }
        
        // Tooltip détaillé
        if (stats != null) {
            String tooltipText = String.format(
                "%s\n\n" +
                "📥 Flux entrants: %d\n" +
                "📤 Flux sortants: %d\n" +
                "🔄 Flux internes: %d\n" +
                "⏱ Durée moyenne: %s\n" +
                "📊 Score: %d%%\n" +
                "%s",
                service.getServiceName(),
                stats.getFluxEntrants(),
                stats.getFluxSortants(),
                stats.getFluxInternes(),
                stats.getDureeMoyenneFormatee(),
                stats.getScorePerformance(),
                stats.estGoulot() ? "⚠️ GOULOT DÉTECTÉ" : "✓ Flux normal"
            );
            Tooltip tooltip = new Tooltip(tooltipText);
            Tooltip.install(node, tooltip);
        }
        
        // Interactivité
        node.setCursor(Cursor.HAND);
        node.setOnMouseEntered(e -> {
            node.setScaleX(1.1);
            node.setScaleY(1.1);
        });
        node.setOnMouseExited(e -> {
            node.setScaleX(1.0);
            node.setScaleY(1.0);
        });
        
        // Double-clic pour voir les détails
        node.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && stats != null) {
                showServiceDetails(stats);
            }
        });
        
        return node;
    }
    
    /**
     * Affiche les détails d'un service avec ses bureaux/sections
     */
    private void showServiceDetails(ServiceFlowStats stats) {
        System.out.println("📊 Affichage détails service: " + stats.getServiceCode());
        
        ServiceHierarchy service = workflowService.getServiceByCode(stats.getServiceCode());
        if (service == null) return;
        
        // Créer une fenêtre de dialogue
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Détails du Service");
        dialog.setHeaderText(service.getIcone() + " " + service.getServiceName());
        
        // Créer le contenu détaillé
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: white;");
        
        // Informations générales
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(20);
        infoGrid.setVgap(10);
        
        infoGrid.add(new Label("Code:"), 0, 0);
        infoGrid.add(new Label(service.getServiceCode()), 1, 0);
        
        infoGrid.add(new Label("Niveau hiérarchique:"), 0, 1);
        infoGrid.add(new Label(String.valueOf(service.getNiveau())), 1, 1);
        
        if (service.getParent() != null) {
            infoGrid.add(new Label("Service parent:"), 0, 2);
            infoGrid.add(new Label(service.getParent().getServiceName()), 1, 2);
        }
        
        content.getChildren().add(infoGrid);
        content.getChildren().add(new Separator());
        
        // Statistiques de flux
        Label statsTitle = new Label("📊 Statistiques de Flux");
        statsTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        content.getChildren().add(statsTitle);
        
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(8);
        
        statsGrid.add(new Label("📥 Flux entrants:"), 0, 0);
        statsGrid.add(new Label(String.valueOf(stats.getFluxEntrants())), 1, 0);
        
        statsGrid.add(new Label("📤 Flux sortants:"), 0, 1);
        statsGrid.add(new Label(String.valueOf(stats.getFluxSortants())), 1, 1);
        
        statsGrid.add(new Label("🔄 Flux internes:"), 0, 2);
        statsGrid.add(new Label(String.valueOf(stats.getFluxInternes())), 1, 2);
        
        statsGrid.add(new Label("⏱ Durée moyenne:"), 0, 3);
        statsGrid.add(new Label(stats.getDureeMoyenneFormatee()), 1, 3);
        
        statsGrid.add(new Label("⚠️ Retards:"), 0, 4);
        Label retardsLabel = new Label(String.valueOf(stats.getRetards()));
        if (stats.getRetards() > 0) {
            retardsLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        }
        statsGrid.add(retardsLabel, 1, 4);
        
        statsGrid.add(new Label("📊 Score performance:"), 0, 5);
        Label scoreLabel = new Label(stats.getScorePerformance() + "%");
        if (stats.getScorePerformance() >= 80) {
            scoreLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        } else if (stats.getScorePerformance() < 60) {
            scoreLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        }
        statsGrid.add(scoreLabel, 1, 5);
        
        content.getChildren().add(statsGrid);
        
        // Bureaux/sections associés
        if (!service.getEnfants().isEmpty()) {
            content.getChildren().add(new Separator());
            
            Label bureauxTitle = new Label("🏢 Bureaux / Sections Associés");
            bureauxTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
            content.getChildren().add(bureauxTitle);
            
            VBox bureauxList = new VBox(5);
            for (ServiceHierarchy enfant : service.getEnfants()) {
                HBox bureauBox = new HBox(10);
                bureauBox.setAlignment(Pos.CENTER_LEFT);
                
                Label bureauIcon = new Label(enfant.getIcone());
                Label bureauName = new Label(enfant.getServiceName());
                bureauName.setFont(Font.font(12));
                
                // Récupérer les stats du bureau si disponibles
                ServiceFlowStats bureauStats = fluxStats.get(enfant.getServiceCode());
                if (bureauStats != null) {
                    int totalFlux = bureauStats.getFluxEntrants() + 
                                   bureauStats.getFluxSortants() + 
                                   bureauStats.getFluxInternes();
                    Label fluxLabel = new Label("(" + totalFlux + " courriers)");
                    fluxLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
                    bureauBox.getChildren().addAll(bureauIcon, bureauName, fluxLabel);
                } else {
                    bureauBox.getChildren().addAll(bureauIcon, bureauName);
                }
                
                bureauxList.getChildren().add(bureauBox);
            }
            
            ScrollPane scrollPane = new ScrollPane(bureauxList);
            scrollPane.setMaxHeight(150);
            scrollPane.setFitToWidth(true);
            content.getChildren().add(scrollPane);
        }
        
        // Afficher la fenêtre
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(500);
        dialog.showAndWait();
    }
    
    /**
     * Met à jour les statistiques globales
     */
    private void updateStatistics() {
        int totalCourriers = fluxCourriers.stream()
            .map(FluxCourrier::getCourrierId)
            .collect(Collectors.toSet())
            .size();
        
        int servicesActifs = fluxStats.size();
        
        double dureeMoyenne = fluxStats.values().stream()
            .mapToDouble(ServiceFlowStats::getDureeMoyenne)
            .filter(d -> d > 0)
            .average()
            .orElse(0);
        
        long goulotsDetectes = fluxStats.values().stream()
            .filter(ServiceFlowStats::estGoulot)
            .count();
        
        if (statTotalCourriers != null) {
            statTotalCourriers.setText(String.valueOf(totalCourriers));
        }
        
        if (statServicesActifs != null) {
            statServicesActifs.setText(String.valueOf(servicesActifs));
        }
        
        if (statDureeMoyenne != null) {
            statDureeMoyenne.setText(formatDuree((long) dureeMoyenne));
        }
        
        if (statGoulotsDetectes != null) {
            statGoulotsDetectes.setText(String.valueOf(goulotsDetectes));
            if (goulotsDetectes > 0) {
                statGoulotsDetectes.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            } else {
                statGoulotsDetectes.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            }
        }
    }
    
    /**
     * Met à jour la table des détails
     */
    private void updateTable() {
        if (tableFluxDetails == null) return;
        
        List<ServiceFlowStats> statsList = new ArrayList<>(fluxStats.values());
        statsList.sort((a, b) -> {
            // Trier par goulots d'abord, puis par score
            if (a.estGoulot() != b.estGoulot()) {
                return a.estGoulot() ? -1 : 1;
            }
            return Integer.compare(b.getScorePerformance(), a.getScorePerformance());
        });
        
        tableFluxDetails.getItems().clear();
        tableFluxDetails.getItems().addAll(statsList);
    }
    
    /**
     * Affiche un message quand le graphe est vide
     */
    private void showEmptyGraphMessage() {
        VBox emptyBox = new VBox(20);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setLayoutX(1000 - 200);
        emptyBox.setLayoutY(750 - 100);
        emptyBox.setPrefWidth(400);
        
        Label iconLabel = new Label("📊");
        iconLabel.setFont(Font.font(64));
        iconLabel.setStyle("-fx-text-fill: #bdc3c7;");
        
        Label messageLabel = new Label("Aucun flux de courriers pour la période sélectionnée");
        messageLabel.setFont(Font.font(16));
        messageLabel.setStyle("-fx-text-fill: #7f8c8d;");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(380);
        
        Label hintLabel = new Label("Essayez de modifier les filtres ou la période");
        hintLabel.setFont(Font.font(12));
        hintLabel.setStyle("-fx-text-fill: #95a5a6;");
        
        emptyBox.getChildren().addAll(iconLabel, messageLabel, hintLabel);
        graphPane.getChildren().add(emptyBox);
    }
    
    // === HANDLERS ===
    
    private void regenerateGraph() {
        calculateFluxStatistics();
        generateGraph();
        updateStatistics();
        updateTable();
    }
    
    private void updateStatisticsVisibility() {
        boolean visible = chkAfficherStatistiques != null && chkAfficherStatistiques.isSelected();
        if (statsDetailContainer != null) {
            statsDetailContainer.setVisible(visible);
            statsDetailContainer.setManaged(visible);
        }
    }
    
    // === CLASSES INTERNES ===
    
    /**
     * Classe pour représenter un point 2D
     */
    private static class Point2D {
        private final double x, y;
        
        public Point2D(double x, double y) {
            this.x = x;
            this.y = y;
        }
        
        public double getX() { return x; }
        public double getY() { return y; }
    }
    
    /**
     * Classe pour représenter un flux de courrier
     */
    private static class FluxCourrier {
        private final int courrierId;
        private final String numeroCourrier;
        private final TypeCourrier typeCourrier;
        private final String serviceSource;
        private final String serviceDestination;
        private final long dureeHeures;
        private final LocalDateTime dateFlux;
        
        public FluxCourrier(int courrierId, String numeroCourrier, TypeCourrier typeCourrier,
                           String serviceSource, String serviceDestination, long dureeHeures,
                           LocalDateTime dateFlux) {
            this.courrierId = courrierId;
            this.numeroCourrier = numeroCourrier;
            this.typeCourrier = typeCourrier;
            this.serviceSource = serviceSource;
            this.serviceDestination = serviceDestination;
            this.dureeHeures = dureeHeures;
            this.dateFlux = dateFlux;
        }
        
        public int getCourrierId() { return courrierId; }
        public String getNumeroCourrier() { return numeroCourrier; }
        public TypeCourrier getTypeCourrier() { return typeCourrier; }
        public String getServiceSource() { return serviceSource; }
        public String getServiceDestination() { return serviceDestination; }
        public long getDureeHeures() { return dureeHeures; }
        public LocalDateTime getDateFlux() { return dateFlux; }
    }
    
    /**
     * NOUVELLE CLASSE : Événement de parcours (workflow ou cotation)
     */
    private static class EvenementParcours {
        private final LocalDateTime date;
        private final String type; // "WORKFLOW" ou "COTATION"
        private final String serviceCode;
        private final String action;
        private final StatutEtapeWorkflow statutWorkflow;
        private final WorkflowStep workflowStep;
        private final CotationCourrier cotation;
        
        public EvenementParcours(LocalDateTime date, String type, String serviceCode, 
                                String action, StatutEtapeWorkflow statutWorkflow, Object data) {
            this.date = date;
            this.type = type;
            this.serviceCode = serviceCode;
            this.action = action;
            this.statutWorkflow = statutWorkflow;
            
            if (data instanceof WorkflowStep) {
                this.workflowStep = (WorkflowStep) data;
                this.cotation = null;
            } else if (data instanceof CotationCourrier) {
                this.workflowStep = null;
                this.cotation = (CotationCourrier) data;
            } else {
                this.workflowStep = null;
                this.cotation = null;
            }
        }
        
        public LocalDateTime getDate() { return date; }
        public String getType() { return type; }
        public String getServiceCode() { return serviceCode; }
        public String getAction() { return action; }
        public StatutEtapeWorkflow getStatutWorkflow() { return statutWorkflow; }
        public WorkflowStep getWorkflowStep() { return workflowStep; }
        public CotationCourrier getCotation() { return cotation; }
    }
    
    /**
     * Classe pour les statistiques de flux d'un service
     */
    public static class ServiceFlowStats {
        private final String serviceCode;
        private final String serviceName;
        private int fluxEntrants;
        private int fluxSortants;
        private int fluxInternes;
        private double dureeMoyenne;
        private int nombreDurees;
        private int retards;
        
        public ServiceFlowStats(String serviceCode, String serviceName) {
            this.serviceCode = serviceCode;
            this.serviceName = serviceName;
        }
        
        public void incrementFluxEntrants() { fluxEntrants++; }
        public void incrementFluxSortants() { fluxSortants++; }
        public void incrementFluxInternes() { fluxInternes++; }
        public void incrementRetards() { retards++; }
        
        public void ajouterDureeTraitement(long heures) {
            dureeMoyenne = (dureeMoyenne * nombreDurees + heures) / (nombreDurees + 1);
            nombreDurees++;
        }
        
        public String getServiceCode() { return serviceCode; }
        public String getServiceName() { return serviceName; }
        public int getFluxEntrants() { return fluxEntrants; }
        public int getFluxSortants() { return fluxSortants; }
        public int getFluxInternes() { return fluxInternes; }
        public double getDureeMoyenne() { return dureeMoyenne; }
        public int getRetards() { return retards; }
        
        public String getDureeMoyenneFormatee() {
            if (dureeMoyenne < 1) {
                return String.format("%.0f min", dureeMoyenne * 60);
            } else if (dureeMoyenne < 24) {
                return String.format("%.1f h", dureeMoyenne);
            } else {
                return String.format("%.1f j", dureeMoyenne / 24);
            }
        }
        
        public int getScorePerformance() {
            int total = fluxEntrants + fluxSortants + fluxInternes;
            if (total == 0) return 100;
            
            double tauxRetard = (double) retards / total;
            double score = 100 - (tauxRetard * 50);
            
            if (dureeMoyenne > 48) {
                score -= 20;
            } else if (dureeMoyenne > 24) {
                score -= 10;
            }
            
            if (dureeMoyenne < 4) {
                score += 10;
            }
            
            return Math.max(0, Math.min(100, (int) score));
        }
        
        public boolean estGoulot() {
            int total = fluxEntrants + fluxSortants + fluxInternes;
            return dureeMoyenne > 24 || (total > 0 && retards > total * 0.3);
        }
        
        public String getStatutDescription() {
            if (estGoulot()) {
                return "⚠️ Goulot";
            } else if (getScorePerformance() >= 80) {
                return "✓ Excellent";
            } else if (getScorePerformance() >= 60) {
                return "◐ Satisfaisant";
            } else {
                return "◯ À améliorer";
            }
        }
    }
    
    /**
     * Classe pour représenter un courrier dans le ComboBox
     */
    public static class CourrierItem {
        private final int courrierId;
        private final String numero;
        private final String objet;
        private final TypeCourrier type;
        private final LocalDateTime date;
        
        public CourrierItem(int courrierId, String numero, String objet, TypeCourrier type, LocalDateTime date) {
            this.courrierId = courrierId;
            this.numero = numero;
            this.objet = objet;
            this.type = type;
            this.date = date;
        }
        
        public int getCourrierId() { return courrierId; }
        public String getNumero() { return numero; }
        public String getObjet() { return objet; }
        public TypeCourrier getType() { return type; }
        public LocalDateTime getDate() { return date; }
        
        @Override
        public String toString() {
            String icon = type == TypeCourrier.ENTRANT ? "📥" :
                         type == TypeCourrier.SORTANT ? "📤" : "📄";
            String objetCourt = objet.length() > 30 ? objet.substring(0, 27) + "..." : objet;
            String dateCourte = date.format(DateTimeFormatter.ofPattern("dd/MM/yy"));
            return String.format("%s %s - %s (%s)", icon, numero, objetCourt, dateCourte);
        }
    }
}
    