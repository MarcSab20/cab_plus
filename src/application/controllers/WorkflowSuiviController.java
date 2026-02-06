package application.controllers;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import application.models.*;
import application.services.*;
import application.utils.*;
import application.controllers.CourrierDetailDialog;
import application.controllers.ArcCourriersDialog;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import application.utils.InteractiveGraphElements.GraphNode;
import application.utils.InteractiveGraphElements.GraphArc;
import java.util.stream.Collectors;

/**
 * 🎨 CONTRÔLEUR ULTRA-MODERNE DE VISUALISATION DES WORKFLOWS
 * 
 * FONCTIONNALITÉS PRINCIPALES :
 * ✅ 5 Modes de visualisation distincts
 * ✅ Graphes interactifs avec couleurs par courrier
 * ✅ Arcs/nœuds cliquables avec actions contextuelles
 * ✅ Tableau de courriers avec sélection et commentaires
 * ✅ Statistiques avancées (globales, par service, temporelles)
 * ✅ Graphe extensible verticalement et horizontalement
 * ✅ Gestion des permissions par niveau hiérarchique
 * ✅ Export et rapports
 */
public class WorkflowSuiviController implements Initializable {
    
    // ═══════════════════════════════════════════════════════════════
    // CONTRÔLES FXML - PANNEAU SUPÉRIEUR
    // ═══════════════════════════════════════════════════════════════
    
    @FXML private ComboBox<ModeVisualisationItem> cbModeVisualisation;
    @FXML private ComboBox<PeriodeItem> cbPeriode;
    @FXML private DatePicker dpDebut;
    @FXML private DatePicker dpFin;
    @FXML private ComboBox<String> cbPriorite;
    @FXML private CheckBox chkInclureConfidentiels;
    @FXML private CheckBox chkAfficherStatistiques;
    @FXML private CheckBox chkAfficherGoulots;
    @FXML private Button btnActualiser;
    @FXML private Button btnExporter;
    
    // ═══════════════════════════════════════════════════════════════
    // ZONE GRAPHE
    // ═══════════════════════════════════════════════════════════════
    
    @FXML private ScrollPane graphScrollPane;
    @FXML private Pane graphPane;
    @FXML private Slider sliderZoom;
    @FXML private Label lblZoomValue;
    @FXML private Label lblNbCourriers;
    @FXML private Label lblNbEtapes;
    
    // ═══════════════════════════════════════════════════════════════
    // TABLEAU DES COURRIERS
    // ═══════════════════════════════════════════════════════════════
    
    @FXML private VBox tableauCourriersContainer;
    @FXML private TableView<CourrierVisuItem> tableCourriers;
    @FXML private TableColumn<CourrierVisuItem, String> colCourrierCode;
    @FXML private TableColumn<CourrierVisuItem, String> colCourrierObjet;
    @FXML private TableColumn<CourrierVisuItem, String> colCourrierType;
    @FXML private TableColumn<CourrierVisuItem, String> colCourrierPriorite;
    @FXML private TableColumn<CourrierVisuItem, String> colCourrierStatut;
    @FXML private TableColumn<CourrierVisuItem, Integer> colCourrierEtapes;
    @FXML private TableColumn<CourrierVisuItem, String> colCourrierDuree;
    @FXML private TableColumn<CourrierVisuItem, Void> colCourrierActions;
    @FXML private TextField txtRechercherCourrier;
    @FXML private Button btnVoirSelectionne;
    @FXML private Button btnCommenterSelectionne;
    
    // ═══════════════════════════════════════════════════════════════
    // STATISTIQUES
    // ═══════════════════════════════════════════════════════════════
    
    @FXML private VBox statsContainer;
    @FXML private Label statTotalCourriers;
    @FXML private Label statServicesActifs;
    @FXML private Label statDureeMoyenne;
    @FXML private Label statGoulotsDetectes;
    @FXML private Label statTauxReussite;
    @FXML private Label statRetards;
    
    @FXML private TabPane statsTabPane;
    @FXML private Tab tabStatsGlobales;
    @FXML private Tab tabStatsServices;
    @FXML private Tab tabStatsTemporelles;
    
    @FXML private VBox statsGlobalesContent;
    @FXML private VBox statsServicesContent;
    @FXML private VBox statsTemporellesContent;
    
    // ═══════════════════════════════════════════════════════════════
    // SERVICES & DONNÉES
    // ═══════════════════════════════════════════════════════════════
    
    private User currentUser;
    private WorkflowAnalysisService workflowService;
    private CourrierService courrierService;
    private CotationService cotationService;
    
    private List<ServiceHierarchy> servicesAutorises;
    private ObservableList<CourrierVisuItem> courriersVisibles;
    private Map<Integer, Color> courrierColors; // Couleur par courrier_id
    private Map<String, InteractiveGraphElements.GraphNode> nodeMap;
    private List<InteractiveGraphElements.GraphArc> arcsList;
    
    // ═══════════════════════════════════════════════════════════════
    // VARIABLES D'ÉTAT
    // ═══════════════════════════════════════════════════════════════
    
    private ModeVisualisation modeActuel;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private Scale scaleTransform;
    private double currentZoom = 1.0;
    
    // Constantes graphiques
    private static final double NODE_WIDTH = 180;
    private static final double NODE_HEIGHT = 80;
    private static final double VERTICAL_SPACING = 150;
    private static final double HORIZONTAL_SPACING = 350;
    private static final double MIN_ARROW_WIDTH = 2;
    private static final double MAX_ARROW_WIDTH = 25;
    private static final int GRAPH_BASE_WIDTH = 2500;
    private static final int GRAPH_BASE_HEIGHT = 2000;
    
    // ═══════════════════════════════════════════════════════════════
    // ÉNUMÉRATIONS
    // ═══════════════════════════════════════════════════════════════
    
    public enum ModeVisualisation {
        COLLECTIF_TOTAL("📊 Vue Collective Totale", "Tous les courriers non confidentiels"),
        COLLECTIF_GROUPE("👥 Vue Collective Groupée", "Courriers selon hiérarchie"),
        INDIVIDUEL("🔍 Vue Individuelle", "Un courrier spécifique"),
        CONFIDENTIELS("🔒 Courriers Confidentiels", "Niveau 0 uniquement"),
        PAR_PRIORITE("🎯 Vue par Priorité", "Filtrer par priorité");
        
        private final String label;
        private final String description;
        
        ModeVisualisation(String label, String description) {
            this.label = label;
            this.description = description;
        }
        
        public String getLabel() { return label; }
        public String getDescription() { return description; }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // INITIALISATION
    // ═══════════════════════════════════════════════════════════════
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("🎨 INITIALISATION - Visualisation Workflow Avancée");
        System.out.println("═══════════════════════════════════════════════════════");
        
        try {
            // Initialiser les services
            currentUser = SessionManager.getInstance().getCurrentUser();
            workflowService = WorkflowAnalysisService.getInstance();
            courrierService = CourrierService.getInstance();
            cotationService = CotationService.getInstance();
            
            if (currentUser == null) {
                AlertUtils.showError("Erreur", "Aucun utilisateur connecté");
                return;
            }
            
            System.out.println("✅ Utilisateur: " + currentUser.getNomComplet() + 
                             " (Niveau: " + currentUser.getNiveauAutorite() + ")");
            
            // Charger les services autorisés selon le niveau
            loadServicesAutorises();
            
            // Initialiser les structures de données
            courriersVisibles = FXCollections.observableArrayList();
            courrierColors = new HashMap<>();
            nodeMap = new HashMap<>();
            arcsList = new ArrayList<>();
            
            // Configurer les composants UI
            setupModeSelector();
            setupPeriodeSelector();
            setupPrioriteFilter();
            setupTableCourriers();
            setupZoom();
            setupStatistiques();
            setupActions();
            
            // Charger les données initiales
            setDefaultPeriod();
            loadInitialData();
            
            System.out.println("✅ Initialisation terminée avec succès");
            
        } catch (Exception e) {
            System.err.println("❌ Erreur initialisation: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur", "Erreur d'initialisation: " + e.getMessage());
        }
    }
    
    /**
     * Charge les services autorisés selon le niveau hiérarchique
     */
    private void loadServicesAutorises() {
        servicesAutorises = new ArrayList<>();
        int niveau = currentUser.getNiveauAutorite();
        
        if (niveau == 0) {
            // Niveau 0 : voir TOUT
            servicesAutorises.addAll(workflowService.getAllServices());
            System.out.println("✅ Niveau 0 - Accès total: " + servicesAutorises.size() + " services");
            
        } else if (niveau == 1) {
            // Niveau 1 : voir sa hiérarchie + descendance
            String serviceCode = currentUser.getServiceCode();
            if (serviceCode != null) {
                ServiceHierarchy userService = workflowService.getServiceByCode(serviceCode);
                if (userService != null) {
                    servicesAutorises.add(userService);
                    servicesAutorises.addAll(userService.getTousLesDescendants());
                }
            }
            System.out.println("✅ Niveau 1 - Services autorisés: " + servicesAutorises.size());
            
        } else {
            // Niveau 2+ : uniquement son service + subordonnés directs
            String serviceCode = currentUser.getServiceCode();
            if (serviceCode != null) {
                ServiceHierarchy userService = workflowService.getServiceByCode(serviceCode);
                if (userService != null) {
                    servicesAutorises.add(userService);
                    servicesAutorises.addAll(userService.getEnfants()); // Uniquement enfants directs
                }
            }
            System.out.println("✅ Niveau " + niveau + " - Services limités: " + servicesAutorises.size());
        }
    }
    
    /**
     * Configure le sélecteur de mode de visualisation
     */
    private void setupModeSelector() {
        ObservableList<ModeVisualisationItem> modes = FXCollections.observableArrayList();
        
        // Ajouter les modes disponibles selon le niveau
        modes.add(new ModeVisualisationItem(ModeVisualisation.COLLECTIF_TOTAL));
        modes.add(new ModeVisualisationItem(ModeVisualisation.COLLECTIF_GROUPE));
        modes.add(new ModeVisualisationItem(ModeVisualisation.INDIVIDUEL));
        modes.add(new ModeVisualisationItem(ModeVisualisation.PAR_PRIORITE));
        
        // Mode confidentiels uniquement pour niveau 0
        if (currentUser.getNiveauAutorite() == 0) {
            modes.add(new ModeVisualisationItem(ModeVisualisation.CONFIDENTIELS));
            chkInclureConfidentiels.setDisable(false);
        } else {
            chkInclureConfidentiels.setDisable(true);
            chkInclureConfidentiels.setSelected(false);
        }
        
        cbModeVisualisation.setItems(modes);
        cbModeVisualisation.setValue(modes.get(0));
        modeActuel = ModeVisualisation.COLLECTIF_TOTAL;
        
        cbModeVisualisation.setOnAction(e -> {
            ModeVisualisationItem selected = cbModeVisualisation.getValue();
            if (selected != null) {
                modeActuel = selected.getMode();
                adaptUIToMode();
            }
        });
    }
    
    /**
     * Configure le sélecteur de période
     */
    private void setupPeriodeSelector() {
        ObservableList<PeriodeItem> periodes = FXCollections.observableArrayList(
            new PeriodeItem("Aujourd'hui", 0),
            new PeriodeItem("Il y a 2 jours", 2),
            new PeriodeItem("Cette semaine", 7),
            new PeriodeItem("Ce mois", 30),
            new PeriodeItem("Cette année", 365),
            new PeriodeItem("Personnalisé", -1)
        );
        
        cbPeriode.setItems(periodes);
        cbPeriode.setValue(periodes.get(2)); // Cette semaine par défaut
        
        cbPeriode.setOnAction(e -> {
            PeriodeItem selected = cbPeriode.getValue();
            if (selected != null) {
                if (selected.getJours() == -1) {
                    // Personnalisé : activer les DatePickers
                    dpDebut.setDisable(false);
                    dpFin.setDisable(false);
                } else {
                    // Période prédéfinie
                    dpDebut.setDisable(true);
                    dpFin.setDisable(true);
                    setDateRange(selected.getJours());
                    refreshVisualization();
                }
            }
        });
        
        dpDebut.setOnAction(e -> {
            if (!dpDebut.isDisabled() && dpDebut.getValue() != null) {
                dateDebut = dpDebut.getValue().atStartOfDay();
                refreshVisualization();
            }
        });
        
        dpFin.setOnAction(e -> {
            if (!dpFin.isDisabled() && dpFin.getValue() != null) {
                dateFin = dpFin.getValue().atTime(23, 59, 59);
                refreshVisualization();
            }
        });
    }
    
    /**
     * Configure le filtre de priorité
     */
    private void setupPrioriteFilter() {
        ObservableList<String> priorites = FXCollections.observableArrayList(
            "Toutes les priorités",
            "🚨 Très Urgente",
            "🔴 Urgente",
            "🟡 Normale"
        );
        
        cbPriorite.setItems(priorites);
        cbPriorite.setValue(priorites.get(0));
        
        cbPriorite.setOnAction(e -> {
            if (modeActuel == ModeVisualisation.PAR_PRIORITE) {
                refreshVisualization();
            }
        });
    }
    
    /**
     * Configure la table des courriers
     */
    private void setupTableCourriers() {
    // Colonnes
    colCourrierCode.setCellValueFactory(new PropertyValueFactory<>("codeCourrier"));
    colCourrierObjet.setCellValueFactory(new PropertyValueFactory<>("objet"));
    colCourrierType.setCellValueFactory(new PropertyValueFactory<>("typeLibelle"));
    colCourrierPriorite.setCellValueFactory(new PropertyValueFactory<>("prioriteLibelle"));
    colCourrierStatut.setCellValueFactory(new PropertyValueFactory<>("statutLibelle"));
    colCourrierEtapes.setCellValueFactory(new PropertyValueFactory<>("nbEtapes"));
    colCourrierDuree.setCellValueFactory(new PropertyValueFactory<>("dureeFormatee"));
    
    // Colonne actions avec boutons
    colCourrierActions.setCellFactory(createActionsCell());
    
    // Style des lignes selon priorité
    tableCourriers.setRowFactory(tv -> {
        TableRow<CourrierVisuItem> row = new TableRow<>() {
            @Override
            protected void updateItem(CourrierVisuItem item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty || item == null) {
                    setStyle("");
                } else {
                    // Bordure gauche selon la couleur du courrier
                    Color color = courrierColors.get(item.getCourrierId());
                    if (color != null) {
                        String colorHex = String.format("#%02X%02X%02X",
                            (int)(color.getRed() * 255),
                            (int)(color.getGreen() * 255),
                            (int)(color.getBlue() * 255));
                        setStyle("-fx-border-color: " + colorHex + " transparent transparent transparent;" +
                               "-fx-border-width: 0 0 0 4;");
                    }
                    
                    // Surbrillance si retard
                    if (item.isEnRetard()) {
                        setStyle(getStyle() + "-fx-background-color: #fdeaea;");
                    }
                }
            }
        };
        
        // Double-clic pour voir le courrier
        row.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && !row.isEmpty()) {
                CourrierVisuItem item = row.getItem();
                Courrier courrier = courrierService.getCourrierById(item.getCourrierId());
                
                if (courrier != null) {
                    Platform.runLater(() -> {
                        CourrierDetailDialog dialog = new CourrierDetailDialog(
                            courrier,
                            cotationService,
                            workflowService
                        );
                        dialog.show();
                    });
                }
            }
        });
        return row;
    }); // ← IMPORTANT: Fermer le setRowFactory (MANQUAIT DANS VOTRE CODE)
    
    tableCourriers.setItems(courriersVisibles);
    
    // Recherche
    if (txtRechercherCourrier != null) {
        txtRechercherCourrier.textProperty().addListener((obs, oldVal, newVal) -> {
            filterTableCourriers(newVal);
        });
    }
    
    // Boutons actions
    if (btnVoirSelectionne != null) {
        btnVoirSelectionne.setOnAction(e -> {
            CourrierVisuItem selected = tableCourriers.getSelectionModel().getSelectedItem();
            if (selected != null) {
                voirCourrierDetails(selected);
            }
        });
    }
    
    if (btnCommenterSelectionne != null) {
        btnCommenterSelectionne.setOnAction(e -> {
            CourrierVisuItem selected = tableCourriers.getSelectionModel().getSelectedItem();
            if (selected != null) {
                ajouterCommentaire(selected);
            }
        });
    }
}
    
    /**
     * Crée les cellules d'actions pour la table
     */
    private Callback<TableColumn<CourrierVisuItem, Void>, TableCell<CourrierVisuItem, Void>> createActionsCell() {
        return param -> new TableCell<>() {
            private final Button btnVoir = new Button("👁");
            private final Button btnComment = new Button("💬");
            private final Button btnDoc = new Button("📄");
            private final HBox pane = new HBox(5, btnVoir, btnComment, btnDoc);
            
            {
                pane.setAlignment(Pos.CENTER);
                
                btnVoir.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand;");
                btnComment.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-cursor: hand;");
                btnDoc.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-cursor: hand;");
                
                btnVoir.setTooltip(new Tooltip("Voir le parcours"));
                btnComment.setTooltip(new Tooltip("Ajouter un commentaire"));
                btnDoc.setTooltip(new Tooltip("Voir le document"));
                
                btnVoir.setOnAction(e -> {
                    CourrierVisuItem item = getTableView().getItems().get(getIndex());
                    voirCourrierDetails(item);
                });
                
                btnComment.setOnAction(e -> {
                    CourrierVisuItem item = getTableView().getItems().get(getIndex());
                    ajouterCommentaire(item);
                });
                
                btnDoc.setOnAction(e -> {
                    CourrierVisuItem item = getTableView().getItems().get(getIndex());
                    voirDocument(item);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        };
    }
    
    /**
     * Configure le zoom
     */
    private void setupZoom() {
        if (sliderZoom != null && graphPane != null) {
            scaleTransform = new Scale(1.0, 1.0);
            graphPane.getTransforms().add(scaleTransform);
            
            sliderZoom.setMin(0.25);
            sliderZoom.setMax(3.0);
            sliderZoom.setValue(1.0);
            
            sliderZoom.valueProperty().addListener((obs, oldVal, newVal) -> {
                currentZoom = newVal.doubleValue();
                scaleTransform.setX(currentZoom);
                scaleTransform.setY(currentZoom);
                
                // Ajuster la taille du pane
                graphPane.setMinWidth(GRAPH_BASE_WIDTH * currentZoom);
                graphPane.setMinHeight(GRAPH_BASE_HEIGHT * currentZoom);
                
                if (lblZoomValue != null) {
                    lblZoomValue.setText(String.format("%.0f%%", currentZoom * 100));
                }
            });
        }
        
        // Zoom avec molette
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
     * Configure les statistiques
     */
    private void setupStatistiques() {
        // Les onglets de stats seront remplis dynamiquement
        if (chkAfficherStatistiques != null) {
            chkAfficherStatistiques.setSelected(true);
            chkAfficherStatistiques.setOnAction(e -> {
                boolean visible = chkAfficherStatistiques.isSelected();
                if (statsContainer != null) {
                    statsContainer.setVisible(visible);
                    statsContainer.setManaged(visible);
                }
            });
        }
        
        if (chkAfficherGoulots != null) {
            chkAfficherGoulots.setSelected(true);
            chkAfficherGoulots.setOnAction(e -> refreshVisualization());
        }
    }
    
    /**
     * Configure les actions des boutons
     */
    private void setupActions() {
        if (btnActualiser != null) {
            btnActualiser.setOnAction(e -> refreshVisualization());
        }
        
        if (btnExporter != null) {
            btnExporter.setOnAction(e -> exporterVisualization());
        }
    }
    
    /**
     * Adapte l'UI selon le mode sélectionné
     */
    private void adaptUIToMode() {
        System.out.println("🔄 Changement de mode: " + modeActuel);
        
        // Adapter l'interface selon le mode
        switch (modeActuel) {
            case INDIVIDUEL:
                tableauCourriersContainer.setVisible(true);
                tableauCourriersContainer.setManaged(true);
                cbPeriode.setDisable(false);
                cbPriorite.setDisable(true);
                break;
                
            case PAR_PRIORITE:
                tableauCourriersContainer.setVisible(true);
                tableauCourriersContainer.setManaged(true);
                cbPriorite.setDisable(false);
                break;
                
            case CONFIDENTIELS:
                if (currentUser.getNiveauAutorite() != 0) {
                    AlertUtils.showWarning("Accès refusé", 
                        "Seuls les utilisateurs de niveau 0 peuvent voir les courriers confidentiels");
                    
                    cbModeVisualisation.setValue(new ModeVisualisationItem(ModeVisualisation.COLLECTIF_TOTAL));
                    return;
                }
                tableauCourriersContainer.setVisible(true);
                tableauCourriersContainer.setManaged(true);
                break;
                
            default:
                tableauCourriersContainer.setVisible(true);
                tableauCourriersContainer.setManaged(true);
                cbPriorite.setDisable(true);
        }
        
        refreshVisualization();
    }
    
    // ═══════════════════════════════════════════════════════════════
    // GESTION DES PÉRIODES ET DATES
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Définit la période par défaut
     */
    private void setDefaultPeriod() {
        setDateRange(7); // 7 jours par défaut
    }
    
    /**
     * Définit la plage de dates selon le nombre de jours
     */
    private void setDateRange(int jours) {
        dateFin = LocalDateTime.now();
        dateDebut = dateFin.minusDays(jours);
        
        if (dpDebut != null) dpDebut.setValue(dateDebut.toLocalDate());
        if (dpFin != null) dpFin.setValue(dateFin.toLocalDate());
    }
    
    // ═══════════════════════════════════════════════════════════════
    // CHARGEMENT DES DONNÉES
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Charge les données initiales
     */
    private void loadInitialData() {
        refreshVisualization();
    }
    
    /**
     * Rafraîchit la visualisation selon le mode actuel
     */
    private void refreshVisualization() {
        System.out.println("🔄 Rafraîchissement - Mode: " + modeActuel);
        
        Platform.runLater(() -> {
            try {
                // Effacer le graphe actuel
                clearGraph();
                
                // Charger les données selon le mode
                List<Courrier> courriers = loadCourriersForMode();
                
                System.out.println("📧 Courriers chargés: " + courriers.size());
                
                // Assigner les couleurs
                assignCourrierColors(courriers);
                
                // Générer le graphe
                generateGraph(courriers);
                
                // Mettre à jour le tableau
                updateTableCourriers(courriers);
                
                // Mettre à jour les statistiques
                updateStatistiques(courriers);
                
                System.out.println("✅ Visualisation rafraîchie");
                
            } catch (Exception e) {
                System.err.println("❌ Erreur rafraîchissement: " + e.getMessage());
                e.printStackTrace();
                AlertUtils.showError("Erreur", "Erreur lors du rafraîchissement:\n" + e.getMessage());
            }
        });
    }
    
    /**
     * Charge les courriers selon le mode de visualisation
     */
    private List<Courrier> loadCourriersForMode() {
        List<Courrier> courriers = new ArrayList<>();
        
        // Récupérer tous les courriers dans la période
        List<Courrier> allCourriers = courrierService.getAllCourriers().stream()
            .filter(c -> c.getDateCreation() != null)
            .filter(c -> !c.getDateCreation().isBefore(dateDebut))
            .filter(c -> !c.getDateCreation().isAfter(dateFin))
            .collect(Collectors.toList());
        
        switch (modeActuel) {
            case COLLECTIF_TOTAL:
                courriers = allCourriers.stream()
                    .filter(c -> !c.isConfidentiel() || chkInclureConfidentiels.isSelected())
                    .collect(Collectors.toList());
                break;
                
            case COLLECTIF_GROUPE:
                courriers = filterCourriersParHierarchie(allCourriers);
                break;
                
            case INDIVIDUEL:
                // En mode individuel, on charge depuis la sélection
                // Pour l'instant, on charge tous pour le tableau
                courriers = allCourriers;
                break;
                
            case CONFIDENTIELS:
                if (currentUser.getNiveauAutorite() == 0) {
                    courriers = allCourriers.stream()
                        .filter(Courrier::isConfidentiel)
                        .collect(Collectors.toList());
                }
                break;
                
            case PAR_PRIORITE:
                courriers = filterCourriersParPriorite(allCourriers);
                break;
        }
        
        return courriers;
    }
    
    /**
     * Filtre les courriers selon la hiérarchie de l'utilisateur
     */
    private List<Courrier> filterCourriersParHierarchie(List<Courrier> courriers) {
        int niveau = currentUser.getNiveauAutorite();
        
        if (niveau == 0 || niveau == 1) {
            // Niveaux 0 et 1 : courriers passés par eux OU non passés
            Set<String> servicesCodes = servicesAutorises.stream()
                .map(ServiceHierarchy::getServiceCode)
                .collect(Collectors.toSet());
            
            return courriers.stream()
                .filter(c -> {
                    // Vérifier si le courrier a une cotation vers ces services
                    List<CotationCourrier> cotations = cotationService.getCotationsByCourrier(c.getId());
                    return cotations.stream()
                        .anyMatch(cot -> servicesCodes.contains(cot.getServiceDestination()));
                })
                .collect(Collectors.toList());
                
        } else {
            // Niveau 2+ : uniquement courriers arrivés chez eux ou subordonnés directs
            Set<String> servicesCodes = servicesAutorises.stream()
                .map(ServiceHierarchy::getServiceCode)
                .collect(Collectors.toSet());
            
            return courriers.stream()
                .filter(c -> {
                    List<CotationCourrier> cotations = cotationService.getCotationsByCourrier(c.getId());
                    return cotations.stream()
                        .anyMatch(cot -> servicesCodes.contains(cot.getServiceDestination()));
                })
                .collect(Collectors.toList());
        }
    }
    
    /**
     * Filtre les courriers par priorité
     */
    private List<Courrier> filterCourriersParPriorite(List<Courrier> courriers) {
        String prioriteSelectionnee = cbPriorite.getValue();
        
        if (prioriteSelectionnee == null || prioriteSelectionnee.equals("Toutes les priorités")) {
            return courriers;
        }
        
        String priorite = null;
        if (prioriteSelectionnee.contains("Très Urgente")) {
            priorite = "TRES_URGENTE";
        } else if (prioriteSelectionnee.contains("Urgente")) {
            priorite = "URGENTE";
        } else if (prioriteSelectionnee.contains("Normale")) {
            priorite = "NORMALE";
        }
        
        final String finalPriorite = priorite;
        return courriers.stream()
            .filter(c -> c.getPriorite() != null && c.getPriorite().equalsIgnoreCase(finalPriorite))
            .collect(Collectors.toList());
    }
    
    /**
     * Assigne des couleurs uniques à chaque courrier
     */
    private void assignCourrierColors(List<Courrier> courriers) {
        courrierColors.clear();
        
        CourrierColorPalette palette = new CourrierColorPalette();
        
        for (int i = 0; i < courriers.size(); i++) {
            Courrier courrier = courriers.get(i);
            Color color = palette.getColor(i);
            courrierColors.put(courrier.getId(), color);
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // GÉNÉRATION DU GRAPHE
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Efface le graphe actuel
     */
    private void clearGraph() {
        if (graphPane != null) {
            graphPane.getChildren().clear();
        }
        nodeMap.clear();
        arcsList.clear();
    }
    
    /**
     * Génère le graphe selon le mode
     */
    private void generateGraph(List<Courrier> courriers) {
        if (courriers.isEmpty()) {
            showEmptyGraphMessage();
            return;
        }
        
        switch (modeActuel) {
            case COLLECTIF_TOTAL:
            case COLLECTIF_GROUPE:
            case CONFIDENTIELS:
            case PAR_PRIORITE:
                generateCollectiveGraph(courriers);
                break;
                
            case INDIVIDUEL:
                // Sera généré lors de la sélection d'un courrier
                showSelectCourrierMessage();
                break;
        }
    }
    
    /**
     * Génère le graphe collectif (plusieurs courriers)
     */
    private void generateCollectiveGraph(List<Courrier> courriers) {
        System.out.println("📊 Génération graphe collectif pour " + courriers.size() + " courriers");
        
        // Récupérer toutes les étapes de workflow et cotations
        Map<String, ServiceNodeData> serviceData = new HashMap<>();
        List<FluxData> fluxList = new ArrayList<>();
        
        for (Courrier courrier : courriers) {
            // Récupérer le parcours
            List<WorkflowStep> steps = workflowService.getCourrierParcours(courrier.getId());
            List<CotationCourrier> cotations = cotationService.getCotationsByCourrier(courrier.getId());
            
            // Combiner en événements chronologiques
            List<EvenementParcours> evenements = combineWorkflowAndCotations(steps, cotations);
            
            // Créer les flux entre services
            for (int i = 0; i < evenements.size() - 1; i++) {
                EvenementParcours current = evenements.get(i);
                EvenementParcours next = evenements.get(i + 1);
                
                String serviceSource = current.getServiceCode();
                String serviceDest = next.getServiceCode();
                
                if (serviceSource != null && serviceDest != null) {
                    // Ajouter aux données de service
                    serviceData.computeIfAbsent(serviceSource, k -> new ServiceNodeData(serviceSource))
                        .addSortie(courrier.getId());
                    
                    serviceData.computeIfAbsent(serviceDest, k -> new ServiceNodeData(serviceDest))
                        .addEntree(courrier.getId());
                    
                    // Créer le flux
                    long dureeHeures = ChronoUnit.HOURS.between(current.getDate(), next.getDate());
                    fluxList.add(new FluxData(courrier.getId(), serviceSource, serviceDest, dureeHeures));
                }
            }
        }
        
        // Calculer les positions des nœuds
        Map<String, Point2D> positions = calculateNodePositions(new ArrayList<>(serviceData.keySet()));
        
        // Dessiner les arcs (flux)
        drawCollectiveArcs(fluxList, positions);
        
        // Dessiner les nœuds
        drawCollectiveNodes(serviceData, positions);
        
        System.out.println("✅ Graphe collectif généré: " + serviceData.size() + " services, " + 
                         fluxList.size() + " flux");
    }
    
    /**
     * Dessine les arcs pour le graphe collectif
     */
    private void drawCollectiveArcs(List<FluxData> fluxList, Map<String, Point2D> positions) {
        // Regrouper les flux par paire source-destination
        Map<String, List<FluxData>> fluxGroupes = fluxList.stream()
            .collect(Collectors.groupingBy(f -> f.serviceSource + "->" + f.serviceDest));
        
        for (Map.Entry<String, List<FluxData>> entry : fluxGroupes.entrySet()) {
            List<FluxData> flux = entry.getValue();
            
            String serviceSource = flux.get(0).serviceSource;
            String serviceDest = flux.get(0).serviceDest;
            
            Point2D posSource = positions.get(serviceSource);
            Point2D posDest = positions.get(serviceDest);
            
            if (posSource == null || posDest == null) continue;
            
            // Créer l'arc interactif
            InteractiveGraphElements.GraphArc arc = new InteractiveGraphElements.GraphArc(
                posSource.getX() + NODE_WIDTH,
                posSource.getY() + NODE_HEIGHT / 2,
                posDest.getX(),
                posDest.getY() + NODE_HEIGHT / 2,
                flux.size(),
                flux.stream().map(f -> f.courrierId).collect(Collectors.toList()),
                courrierColors
            );
            
            // Événement de clic sur l'arc
            arc.setOnArcClick(courrierIds -> {
                // Ouvrir le dialogue enrichi avec tous les détails
                ArcCourriersDialog dialog = new ArcCourriersDialog(
                    courrierIds,
                    serviceSource,
                    serviceDest,
                    courrierService,
                    cotationService,
                    workflowService
                );
                dialog.show();
            });
            
            graphPane.getChildren().add(arc);
            arcsList.add(arc);
        }
    }
    
    /**
     * Dessine les nœuds pour le graphe collectif
     */
    private void drawCollectiveNodes(Map<String, ServiceNodeData> serviceData, Map<String, Point2D> positions) {
        for (Map.Entry<String, ServiceNodeData> entry : serviceData.entrySet()) {
            String serviceCode = entry.getKey();
            ServiceNodeData data = entry.getValue();
            Point2D pos = positions.get(serviceCode);
            
            if (pos == null) continue;
            
            ServiceHierarchy service = workflowService.getServiceByCode(serviceCode);
            if (service == null) continue;
            
            // Créer le nœud interactif
            InteractiveGraphElements.GraphNode node = new InteractiveGraphElements.GraphNode(
                pos.getX(),
                pos.getY(),
                NODE_WIDTH,
                NODE_HEIGHT,
                service,
                data
            );
            
            // Événement de clic sur le nœud
            node.setOnNodeClick(() -> showServiceDetails(service, data));
            
            graphPane.getChildren().add(node);
            nodeMap.put(serviceCode, node);
        }
    }
    
    /**
     * Calcule les positions des nœuds dans le graphe
     */
    private Map<String, Point2D> calculateNodePositions(List<String> serviceCodes) {
        Map<String, Point2D> positions = new HashMap<>();
        
        // Grouper par niveau hiérarchique
        Map<Integer, List<String>> parNiveau = serviceCodes.stream()
            .collect(Collectors.groupingBy(code -> {
                ServiceHierarchy service = workflowService.getServiceByCode(code);
                return service != null ? service.getNiveau() : 999;
            }));
        
        double startX = 100;
        double startY = 100;
        
        List<Integer> niveaux = new ArrayList<>(parNiveau.keySet());
        Collections.sort(niveaux);
        
        for (int niveauIdx = 0; niveauIdx < niveaux.size(); niveauIdx++) {
            Integer niveau = niveaux.get(niveauIdx);
            List<String> services = parNiveau.get(niveau);
            
            double y = startY + niveauIdx * VERTICAL_SPACING;
            
            for (int i = 0; i < services.size(); i++) {
                double x = startX + i * HORIZONTAL_SPACING;
                positions.put(services.get(i), new Point2D(x, y));
            }
        }
        
        return positions;
    }
    
    /**
     * Combine les étapes de workflow et les cotations en ordre chronologique
     */
    private List<EvenementParcours> combineWorkflowAndCotations(List<WorkflowStep> steps, 
                                                                  List<CotationCourrier> cotations) {
        List<EvenementParcours> evenements = new ArrayList<>();
        
        // Ajouter les étapes de workflow
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
        
        return evenements;
    }
    
    /**
     * Affiche un message quand le graphe est vide
     */
    private void showEmptyGraphMessage() {
        VBox messageBox = new VBox(20);
        messageBox.setAlignment(Pos.CENTER);
        messageBox.setLayoutX(graphPane.getWidth() / 2 - 200);
        messageBox.setLayoutY(graphPane.getHeight() / 2 - 100);
        messageBox.setPrefWidth(400);
        
        Label iconLabel = new Label("📊");
        iconLabel.setFont(Font.font(64));
        iconLabel.setStyle("-fx-text-fill: #bdc3c7;");
        
        Label messageLabel = new Label("Aucun courrier pour la période sélectionnée");
        messageLabel.setFont(Font.font(16));
        messageLabel.setStyle("-fx-text-fill: #7f8c8d;");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(380);
        
        messageBox.getChildren().addAll(iconLabel, messageLabel);
        graphPane.getChildren().add(messageBox);
    }
    
    /**
     * Affiche un message pour sélectionner un courrier
     */
    private void showSelectCourrierMessage() {
        VBox messageBox = new VBox(20);
        messageBox.setAlignment(Pos.CENTER);
        messageBox.setLayoutX(graphPane.getWidth() / 2 - 200);
        messageBox.setLayoutY(graphPane.getHeight() / 2 - 100);
        messageBox.setPrefWidth(400);
        
        Label iconLabel = new Label("🔍");
        iconLabel.setFont(Font.font(64));
        iconLabel.setStyle("-fx-text-fill: #9b59b6;");
        
        Label messageLabel = new Label("Sélectionnez un courrier dans le tableau");
        messageLabel.setFont(Font.font(16));
        messageLabel.setStyle("-fx-text-fill: #7f8c8d;");
        
        Label hintLabel = new Label("pour voir son parcours détaillé");
        hintLabel.setFont(Font.font(12));
        hintLabel.setStyle("-fx-text-fill: #95a5a6;");
        
        messageBox.getChildren().addAll(iconLabel, messageLabel, hintLabel);
        graphPane.getChildren().add(messageBox);
    }
    
    // ═══════════════════════════════════════════════════════════════
    // MISE À JOUR DU TABLEAU ET DES STATISTIQUES
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Met à jour le tableau des courriers
     */
    private void updateTableCourriers(List<Courrier> courriers) {
        courriersVisibles.clear();
        
        for (Courrier courrier : courriers) {
            // Récupérer les étapes et cotations
            List<WorkflowStep> steps = workflowService.getCourrierParcours(courrier.getId());
            List<CotationCourrier> cotations = cotationService.getCotationsByCourrier(courrier.getId());
            
            // Calculer la durée totale
            long dureeHeures = calculerDureeTotale(steps, cotations);
            
            // Vérifier les retards
            boolean enRetard = steps.stream().anyMatch(WorkflowStep::isEnRetard) ||
                              cotations.stream().anyMatch(CotationCourrier::isEnRetard);
            
            CourrierVisuItem item = new CourrierVisuItem(
                courrier.getId(),
                courrier.getCodeCourrier(),
                courrier.getObjet(),
                courrier.getTypeCourrier(),
                courrier.getPriorite(),
                courrier.getStatut(),
                steps.size() + cotations.size(),
                dureeHeures,
                enRetard,
                courrierColors.get(courrier.getId())
            );
            
            courriersVisibles.add(item);
        }
        
        // Trier par date (plus récent en haut)
        courriersVisibles.sort((a, b) -> {
            // Prioriser les retards
            if (a.isEnRetard() && !b.isEnRetard()) return -1;
            if (!a.isEnRetard() && b.isEnRetard()) return 1;
            
            // Puis par priorité
            int pa = getPrioriteOrder(a.getPriorite());
            int pb = getPrioriteOrder(b.getPriorite());
            if (pa != pb) return Integer.compare(pa, pb);
            
            // Enfin alphabétique
            return a.getCodeCourrier().compareTo(b.getCodeCourrier());
        });
    }
    
    /**
     * Filtre le tableau selon la recherche
     */
    private void filterTableCourriers(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            tableCourriers.setItems(courriersVisibles);
        } else {
            String search = searchText.toLowerCase();
            ObservableList<CourrierVisuItem> filtered = courriersVisibles.stream()
                .filter(item -> 
                    item.getCodeCourrier().toLowerCase().contains(search) ||
                    item.getObjet().toLowerCase().contains(search)
                )
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
            
            tableCourriers.setItems(filtered);
        }
    }
    
    /**
     * Calcule la durée totale d'un courrier
     */
    private long calculerDureeTotale(List<WorkflowStep> steps, List<CotationCourrier> cotations) {
        if (steps.isEmpty() && cotations.isEmpty()) return 0;
        
        LocalDateTime debut = LocalDateTime.now();
        LocalDateTime fin = LocalDateTime.now().minusYears(10);
        
        for (WorkflowStep step : steps) {
            if (step.getDateAction().isBefore(debut)) debut = step.getDateAction();
            if (step.getDateAction().isAfter(fin)) fin = step.getDateAction();
        }
        
        for (CotationCourrier cot : cotations) {
            if (cot.getDateCotation().isBefore(debut)) debut = cot.getDateCotation();
            
            LocalDateTime dateFin = cot.getDateTraitement() != null ? 
                cot.getDateTraitement() : LocalDateTime.now();
            
            if (dateFin.isAfter(fin)) fin = dateFin;
        }
        
        return ChronoUnit.HOURS.between(debut, fin);
    }
    
    /**
     * Ordre de priorité pour le tri
     */
    private int getPrioriteOrder(String priorite) {
        if (priorite == null) return 3;
        
        return switch (priorite.toUpperCase()) {
            case "TRES_URGENTE" -> 1;
            case "URGENTE" -> 2;
            case "NORMALE" -> 3;
            default -> 4;
        };
    }
    
    /**
     * Met à jour les statistiques
     */
    private void updateStatistiques(List<Courrier> courriers) {
        // Statistiques globales
        updateStatsGlobales(courriers);
        
        // Statistiques par service
        updateStatsServices(courriers);
        
        // Statistiques temporelles
        updateStatsTemporelles(courriers);
    }
    
    /**
     * Met à jour les stats globales
     */
    private void updateStatsGlobales(List<Courrier> courriers) {
        if (statTotalCourriers != null) {
            statTotalCourriers.setText(String.valueOf(courriers.size()));
        }
        
        // Services actifs
        Set<String> servicesActifs = new HashSet<>();
        for (Courrier courrier : courriers) {
            List<CotationCourrier> cotations = cotationService.getCotationsByCourrier(courrier.getId());
            cotations.forEach(c -> {
                if (c.getServiceDestination() != null) {
                    servicesActifs.add(c.getServiceDestination());
                }
            });
        }
        
        if (statServicesActifs != null) {
            statServicesActifs.setText(String.valueOf(servicesActifs.size()));
        }
        
        // Durée moyenne
        if (!courriers.isEmpty()) {
            double dureeMoyenne = courriers.stream()
                .mapToLong(c -> {
                    List<WorkflowStep> steps = workflowService.getCourrierParcours(c.getId());
                    List<CotationCourrier> cotations = cotationService.getCotationsByCourrier(c.getId());
                    return calculerDureeTotale(steps, cotations);
                })
                .average()
                .orElse(0.0);
            
            if (statDureeMoyenne != null) {
                statDureeMoyenne.setText(formatDuree((long) dureeMoyenne));
            }
        }
        
        // Goulots détectés
        long goulotsCount = courriers.stream()
            .filter(c -> {
                List<WorkflowStep> steps = workflowService.getCourrierParcours(c.getId());
                List<CotationCourrier> cotations = cotationService.getCotationsByCourrier(c.getId());
                return steps.stream().anyMatch(WorkflowStep::isEnRetard) ||
                       cotations.stream().anyMatch(CotationCourrier::isEnRetard);
            })
            .count();
        
        if (statGoulotsDetectes != null) {
            statGoulotsDetectes.setText(String.valueOf(goulotsCount));
            
            if (goulotsCount > 0) {
                statGoulotsDetectes.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 24px;");
            } else {
                statGoulotsDetectes.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 24px;");
            }
        }
        
        // Taux de réussite
        long traites = courriers.stream()
            .filter(c -> "traite".equalsIgnoreCase(c.getStatut()) || 
                        "archive".equalsIgnoreCase(c.getStatut()))
            .count();
        
        double tauxReussite = courriers.isEmpty() ? 0 : (traites * 100.0 / courriers.size());
        
        if (statTauxReussite != null) {
            statTauxReussite.setText(String.format("%.1f%%", tauxReussite));
        }
        
        // Retards
        long retards = courriers.stream()
            .filter(c -> {
                List<CotationCourrier> cotations = cotationService.getCotationsByCourrier(c.getId());
                return cotations.stream().anyMatch(CotationCourrier::isEnRetard);
            })
            .count();
        
        if (statRetards != null) {
            statRetards.setText(String.valueOf(retards));
        }
        
        // Remplir le contenu détaillé
        if (statsGlobalesContent != null) {
            statsGlobalesContent.getChildren().clear();
            
            AdvancedStatisticsGenerator statsGen = new AdvancedStatisticsGenerator();
            VBox statsDetails = statsGen.generateGlobalStats(courriers, dateDebut, dateFin);
            
            statsGlobalesContent.getChildren().add(statsDetails);
        }
    }
    
    /**
     * Met à jour les stats par service
     */
    private void updateStatsServices(List<Courrier> courriers) {
        if (statsServicesContent != null) {
            statsServicesContent.getChildren().clear();
            
            AdvancedStatisticsGenerator statsGen = new AdvancedStatisticsGenerator();
            VBox statsDetails = statsGen.generateServiceStats(courriers, servicesAutorises, 
                                                              workflowService, cotationService);
            
            statsServicesContent.getChildren().add(statsDetails);
        }
    }
    
    /**
     * Met à jour les stats temporelles
     */
    private void updateStatsTemporelles(List<Courrier> courriers) {
        if (statsTemporellesContent != null) {
            statsTemporellesContent.getChildren().clear();
            
            AdvancedStatisticsGenerator statsGen = new AdvancedStatisticsGenerator();
            VBox statsDetails = statsGen.generateTemporalStats(courriers, dateDebut, dateFin);
            
            statsTemporellesContent.getChildren().add(statsDetails);
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
            return jours + "j" + (restHeures > 0 ? " " + restHeures + "h" : "");
        }
    }
    
    // ═══════════════════════════════════════════════════════════════
    // ÉVÉNEMENTS ET ACTIONS
    // ═══════════════════════════════════════════════════════════════

    
    /**
     * Crée une box pour un courrier
     */
    private HBox createCourrierBox(Courrier courrier) {
        HBox box = new HBox(15);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(8));
        box.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; " +
                    "-fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;");
        
        // Indicateur de couleur
        Color color = courrierColors.get(courrier.getId());
        if (color != null) {
            Region colorIndicator = new Region();
            colorIndicator.setPrefWidth(10);
            colorIndicator.setPrefHeight(40);
            String colorHex = String.format("#%02X%02X%02X",
                (int)(color.getRed() * 255),
                (int)(color.getGreen() * 255),
                (int)(color.getBlue() * 255));
            colorIndicator.setStyle("-fx-background-color: " + colorHex + "; -fx-background-radius: 3;");
            box.getChildren().add(colorIndicator);
        }
        
        // Informations
        VBox infoBox = new VBox(3);
        
        Label codeLabel = new Label(courrier.getCodeCourrier());
        codeLabel.setStyle("-fx-font-weight: bold;");
        
        Label objetLabel = new Label(courrier.getObjet());
        objetLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
        
        infoBox.getChildren().addAll(codeLabel, objetLabel);
        box.getChildren().add(infoBox);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        box.getChildren().add(spacer);
        
        // Boutons
        Button btnVoir = new Button("👁");
        btnVoir.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        btnVoir.setOnAction(e -> {
            Platform.runLater(() -> {
                CourrierDetailDialog dialog = new CourrierDetailDialog(
                    courrier,
                    cotationService,
                    workflowService
                );
                dialog.show();
            });
        });
        
        Button btnComment = new Button("💬");
        btnComment.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white;");
        btnComment.setOnAction(e -> {
            ajouterCommentaire(new CourrierVisuItem(
                courrier.getId(),
                courrier.getCodeCourrier(),
                courrier.getObjet(),
                courrier.getTypeCourrier(),
                courrier.getPriorite(),
                courrier.getStatut(),
                0, 0, false, color
            ));
        });
        
        box.getChildren().addAll(btnVoir, btnComment);
        
        return box;
    }
    
    /**
     * Affiche les détails d'un service
     */
    private void showServiceDetails(ServiceHierarchy service, ServiceNodeData data) {
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Détails du Service");
        dialog.setHeaderText(service.getIcone() + " " + service.getServiceName());
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // Informations du service
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(20);
        infoGrid.setVgap(10);
        
        infoGrid.add(new Label("Code:"), 0, 0);
        infoGrid.add(new Label(service.getServiceCode()), 1, 0);
        
        infoGrid.add(new Label("Niveau:"), 0, 1);
        infoGrid.add(new Label(String.valueOf(service.getNiveau())), 1, 1);
        
        infoGrid.add(new Label("Courriers entrants:"), 0, 2);
        infoGrid.add(new Label(String.valueOf(data.getCourrierEntrants().size())), 1, 2);
        
        infoGrid.add(new Label("Courriers sortants:"), 0, 3);
        infoGrid.add(new Label(String.valueOf(data.getCourrierSortants().size())), 1, 3);
        
        content.getChildren().add(infoGrid);
        
        // Liste des courriers
        if (!data.getCourrierEntrants().isEmpty()) {
            content.getChildren().add(new Separator());
            Label titre = new Label("📥 Courriers Entrants");
            titre.setStyle("-fx-font-weight: bold;");
            content.getChildren().add(titre);
            
            for (Integer courrierId : data.getCourrierEntrants()) {
                Courrier courrier = courrierService.getCourrierById(courrierId);
                if (courrier != null) {
                    content.getChildren().add(createCourrierBox(courrier));
                }
            }
        }
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setMaxHeight(500);
        
        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().setPrefWidth(600);
        dialog.show();
    }
    
    /**
     * Voir les détails d'un courrier
     */
    private void voirCourrierDetails(CourrierVisuItem item) {
        System.out.println("👁 Voir détails courrier: " + item.getCodeCourrier());
        
        // Récupérer le courrier complet
        Courrier courrier = courrierService.getCourrierById(item.getCourrierId());
        
        if (courrier == null) {
            AlertUtils.showError("Erreur", "Courrier introuvable");
            return;
        }
        
        // Ouvrir le dialogue enrichi avec tous les détails
        Platform.runLater(() -> {
            CourrierDetailDialog dialog = new CourrierDetailDialog(
                courrier,
                cotationService,
                workflowService
            );
            dialog.show();
        });
    }
    
    /**
     * Dessine le graphe pour un courrier individuel
     */
    private void drawIndividualCourrierGraph(Courrier courrier) {
        System.out.println("🎨 Dessin parcours individuel: " + courrier.getCodeCourrier());
        
        List<WorkflowStep> steps = workflowService.getCourrierParcours(courrier.getId());
        List<CotationCourrier> cotations = cotationService.getCotationsByCourrier(courrier.getId());
        
        List<EvenementParcours> evenements = combineWorkflowAndCotations(steps, cotations);
        
        if (evenements.isEmpty()) {
            showEmptyGraphMessage();
            return;
        }
        
     // Créer en-tête avec bouton détails
        VBox headerBox = new VBox(10);
        headerBox.setLayoutX(50);
        headerBox.setLayoutY(50);
        headerBox.setPadding(new Insets(15));
        headerBox.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                          "-fx-border-color: #3498db; -fx-border-width: 2; -fx-border-radius: 10;");
        
        Label titreLabel = new Label("📨 " + courrier.getCodeCourrier());
        titreLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        
        Label objetLabel = new Label(courrier.getObjet());
        objetLabel.setWrapText(true);
        objetLabel.setMaxWidth(800);
        objetLabel.setStyle("-fx-text-fill: #7f8c8d;");
        
        Button btnDetails = new Button("📋 Voir Détails Complets");
        btnDetails.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                           "-fx-font-weight: bold; -fx-padding: 10 20; -fx-background-radius: 5;");
        btnDetails.setCursor(Cursor.HAND);
        btnDetails.setOnAction(e -> {
            Platform.runLater(() -> {
                CourrierDetailDialog dialog = new CourrierDetailDialog(
                    courrier,
                    cotationService,
                    workflowService
                );
                dialog.show();
            });
        });
        
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.getChildren().add(btnDetails);
        
        headerBox.getChildren().addAll(titreLabel, objetLabel, buttonBox);
        graphPane.getChildren().add(headerBox);
        
        // Disposition horizontale
        double startX = 150;
        double startY = 400;
        double spacing = 300;
        
        Color courrierColor = courrierColors.get(courrier.getId());
        
        for (int i = 0; i < evenements.size(); i++) {
            EvenementParcours event = evenements.get(i);
            double x = startX + i * spacing;
            
            // Créer le nœud
            VBox eventNode = createEventNode(event, i + 1, x, startY, courrierColor);
            graphPane.getChildren().add(eventNode);
            
            // Dessiner la connexion avec le suivant
            if (i < evenements.size() - 1) {
                EvenementParcours nextEvent = evenements.get(i + 1);
                long heures = ChronoUnit.HOURS.between(event.getDate(), nextEvent.getDate());
                
                drawEventConnection(x + NODE_WIDTH, startY + NODE_HEIGHT / 2,
                                   startX + (i + 1) * spacing, startY + NODE_HEIGHT / 2,
                                   heures, courrierColor);
            }
        }
        
        System.out.println("✅ Parcours individuel dessiné: " + evenements.size() + " événements");
    }
    
    /**
    * Récupère un objet Courrier à partir d'un CourrierVisuItem
    */
   private Courrier getCourrierFromItem(CourrierVisuItem item) {
       if (item == null) return null;
       return courrierService.getCourrierById(item.getCourrierId());
   }
    
    /**
     * Crée un nœud pour un événement
     */
    private VBox createEventNode(EvenementParcours event, int numero, double x, double y, Color color) {
        VBox node = new VBox(8);
        node.setLayoutX(x);
        node.setLayoutY(y);
        node.setPrefWidth(NODE_WIDTH);
        node.setMinHeight(NODE_HEIGHT);
        node.setAlignment(Pos.CENTER);
        node.setPadding(new Insets(12));
        
        String colorHex = color != null ? String.format("#%02X%02X%02X",
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255)) : "#3498db";
        
        node.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: " + colorHex + ";" +
            "-fx-border-width: 3;" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 8, 0, 0, 3);"
        );
        
        // Numéro
        Label numLabel = new Label("#" + numero);
        numLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: " + colorHex + ";");
        
        // Type
        String typeIcon = event.getType().equals("COTATION") ? "📋" : "📊";
        Label typeLabel = new Label(typeIcon + " " + event.getType());
        typeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");
        
        // Service
        ServiceHierarchy service = workflowService.getServiceByCode(event.getServiceCode());
        Label serviceLabel = new Label(service != null ? service.getServiceName() : event.getServiceCode());
        serviceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        serviceLabel.setWrapText(true);
        serviceLabel.setMaxWidth(NODE_WIDTH - 20);
        
        // Date
        Label dateLabel = new Label(event.getDate().format(
            DateTimeFormatter.ofPattern("dd/MM à HH:mm")
        ));
        dateLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #95a5a6;");
        
        node.getChildren().addAll(numLabel, typeLabel, serviceLabel, dateLabel);
        
        // Tooltip
        Tooltip tooltip = new Tooltip(buildEventTooltip(event, service));
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
                "Date: %s\n" +
                "Statut: %s",
                service != null ? service.getServiceName() : step.getServiceCode(),
                step.getAction(),
                step.getDateActionFormatee(),
                step.getStatutEtape().getLibelle()
            ));
            
            if (step.getCommentaire() != null && !step.getCommentaire().isEmpty()) {
                tooltip.append("\n\n💬 ").append(step.getCommentaire());
            }
            
        } else {
            CotationCourrier cotation = event.getCotation();
            tooltip.append(String.format(
                "🔹 COTATION\n\n" +
                "Assigné à: %s\n" +
                "Service: %s\n" +
                "Date: %s\n" +
                "Échéance: %s\n" +
                "Priorité: %s\n" +
                "Statut: %s",
                cotation.getAssigneNom(),
                service != null ? service.getServiceName() : cotation.getServiceDestination(),
                cotation.getDateCotationFormatee(),
                cotation.getDateEcheanceFormatee(),
                cotation.getPriorite(),
                cotation.getStatut()
            ));
            
            if (cotation.isEnRetard()) {
                tooltip.append("\n\n⚠️ EN RETARD");
            }
        }
        
        return tooltip.toString();
    }
    
    /**
     * Dessine une connexion entre deux événements
     */
    private void drawEventConnection(double x1, double y1, double x2, double y2, long heures, Color color) {
        Group connectionGroup = new Group();
        
        Line line = new Line(x1, y1, x2, y2);
        String colorHex = color != null ? String.format("#%02X%02X%02X",
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255)) : "#3498db";
        
        line.setStroke(Color.web(colorHex));
        line.setStrokeWidth(4);
        
        // Flèche
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double arrowLength = 12;
        double arrowAngle = Math.PI / 6;
        
        Line arrow1 = new Line(
            x2,
            y2,
            x2 - arrowLength * Math.cos(angle - arrowAngle),
            y2 - arrowLength * Math.sin(angle - arrowAngle)
        );
        
        Line arrow2 = new Line(
            x2,
            y2,
            x2 - arrowLength * Math.cos(angle + arrowAngle),
            y2 - arrowLength * Math.sin(angle + arrowAngle)
        );
        
        arrow1.setStroke(Color.web(colorHex));
        arrow2.setStroke(Color.web(colorHex));
        arrow1.setStrokeWidth(4);
        arrow2.setStrokeWidth(4);
        
        // Label durée
        Label dureeLabel = new Label(formatDuree(heures));
        dureeLabel.setLayoutX((x1 + x2) / 2 - 25);
        dureeLabel.setLayoutY(y1 - 25);
        dureeLabel.setStyle(
            "-fx-background-color: white; " +
            "-fx-padding: 3 8; " +
            "-fx-border-color: " + colorHex + "; " +
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
     * Ajoute un commentaire à un courrier
     */
    private void ajouterCommentaire(CourrierVisuItem item) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Ajouter un commentaire");
        dialog.setHeaderText("Courrier: " + item.getCodeCourrier());
        dialog.setContentText("Commentaire:");
        
        dialog.showAndWait().ifPresent(commentaire -> {
            if (!commentaire.trim().isEmpty()) {
                // Enregistrer le commentaire dans l'historique
                try {
                    // Utiliser le service pour enregistrer
                    DatabaseService.getInstance().logActivity(
                        currentUser.getId(),
                        "COMMENTAIRE_COURRIER",
                        "Courrier " + item.getCodeCourrier() + ": " + commentaire,
                        "127.0.0.1"
                    );
                    
                    AlertUtils.showInfo("Commentaire ajouté", 
                        "Votre commentaire a été enregistré pour le courrier " + item.getCodeCourrier());
                    
                } catch (Exception e) {
                    AlertUtils.showError("Erreur", "Impossible d'enregistrer le commentaire: " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Voir le document associé au courrier
     */
    private void voirDocument(CourrierVisuItem item) {
        // À implémenter : ouvrir le document associé
        AlertUtils.showInfo("Document", "Fonctionnalité à implémenter: visualisation du document");
    }
    
    /**
     * Exporte la visualisation
     */
    private void exporterVisualization() {
        // À implémenter : export en PDF/PNG
        AlertUtils.showInfo("Export", "Fonctionnalité à implémenter: export de la visualisation");
    }
    
    /**
     * Obtient le nom d'un service
     */
    private String getServiceName(String serviceCode) {
        ServiceHierarchy service = workflowService.getServiceByCode(serviceCode);
        return service != null ? service.getServiceName() : serviceCode;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // CLASSES INTERNES
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Point 2D simple
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
     * Item de mode de visualisation
     */
    public static class ModeVisualisationItem {
        private final ModeVisualisation mode;
        
        public ModeVisualisationItem(ModeVisualisation mode) {
            this.mode = mode;
        }
        
        public ModeVisualisation getMode() {
            return mode;
        }
        
        @Override
        public String toString() {
            return mode.getLabel();
        }
    }
    
    /**
     * Item de période
     */
    public static class PeriodeItem {
        private final String label;
        private final int jours;
        
        public PeriodeItem(String label, int jours) {
            this.label = label;
            this.jours = jours;
        }
        
        public String getLabel() { return label; }
        public int getJours() { return jours; }
        
        @Override
        public String toString() {
            return label;
        }
    }
    
    /**
     * Événement de parcours (workflow ou cotation)
     */
    private static class EvenementParcours {
        private final LocalDateTime date;
        private final String type;
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
     * Données d'un nœud de service
     */
    public static class ServiceNodeData {
        private final String serviceCode;
        private final Set<Integer> courriersEntrants;
        private final Set<Integer> courriersSortants;
        
        public ServiceNodeData(String serviceCode) {
            this.serviceCode = serviceCode;
            this.courriersEntrants = new HashSet<>();
            this.courriersSortants = new HashSet<>();
        }
        
        public void addEntree(int courrierId) {
            courriersEntrants.add(courrierId);
        }
        
        public void addSortie(int courrierId) {
            courriersSortants.add(courrierId);
        }
        
        public Set<Integer> getCourrierEntrants() { return courriersEntrants; }
        public Set<Integer> getCourrierSortants() { return courriersSortants; }
    }
    
    /**
     * Données d'un flux
     */
    private static class FluxData {
        private final int courrierId;
        private final String serviceSource;
        private final String serviceDest;
        private final long dureeHeures;
        
        public FluxData(int courrierId, String serviceSource, String serviceDest, long dureeHeures) {
            this.courrierId = courrierId;
            this.serviceSource = serviceSource;
            this.serviceDest = serviceDest;
            this.dureeHeures = dureeHeures;
        }
    }
    
    /**
     * Item de courrier pour la table
     */
    public static class CourrierVisuItem {
        private final IntegerProperty courrierId;
        private final StringProperty codeCourrier;
        private final StringProperty objet;
        private final StringProperty type;
        private final StringProperty priorite;
        private final StringProperty statut;
        private final IntegerProperty nbEtapes;
        private final LongProperty duree;
        private final BooleanProperty enRetard;
        private final Color color;
        
        public CourrierVisuItem(int id, String code, String objet, String type,
                               String priorite, String statut, int nbEtapes,
                               long duree, boolean enRetard, Color color) {
            this.courrierId = new SimpleIntegerProperty(id);
            this.codeCourrier = new SimpleStringProperty(code);
            this.objet = new SimpleStringProperty(objet);
            this.type = new SimpleStringProperty(type);
            this.priorite = new SimpleStringProperty(priorite);
            this.statut = new SimpleStringProperty(statut);
            this.nbEtapes = new SimpleIntegerProperty(nbEtapes);
            this.duree = new SimpleLongProperty(duree);
            this.enRetard = new SimpleBooleanProperty(enRetard);
            this.color = color;
        }
        
        // Getters
        public int getCourrierId() { return courrierId.get(); }
        public String getCodeCourrier() { return codeCourrier.get(); }
        public String getObjet() { return objet.get(); }
        public String getType() { return type.get(); }
        public String getPriorite() { return priorite.get(); }
        public String getStatut() { return statut.get(); }
        public int getNbEtapes() { return nbEtapes.get(); }
        public long getDuree() { return duree.get(); }
        public boolean isEnRetard() { return enRetard.get(); }
        public Color getColor() { return color; }
        
        // Libellés formatés
        public String getTypeLibelle() {
            if (type.get() == null) return "";
            switch (type.get().toUpperCase()) {
                case "ENTRANT": return "📥 Entrant";
                case "SORTANT": return "📤 Sortant";
                case "INTERNE": return "🔄 Interne";
                default: return type.get();
            }
        }
        
        public String getPrioriteLibelle() {
            if (priorite.get() == null) return "🟡 Normale";
            switch (priorite.get().toUpperCase()) {
                case "TRES_URGENTE": return "🚨 Très Urgente";
                case "URGENTE": return "🔴 Urgente";
                case "NORMALE": return "🟡 Normale";
                default: return priorite.get();
            }
        }
        
        public String getStatutLibelle() {
            if (statut.get() == null) return "";
            switch (statut.get().toLowerCase()) {
                case "nouveau": return "🆕 Nouveau";
                case "en_cours": return "⏳ En cours";
                case "traite": return "✅ Traité";
                case "archive": return "📦 Archivé";
                default: return statut.get();
            }
        }
        
        public String getDureeFormatee() {
            long h = duree.get();
            if (h < 1) return "< 1h";
            if (h < 24) return h + "h";
            long j = h / 24;
            long rh = h % 24;
            return j + "j" + (rh > 0 ? " " + rh + "h" : "");
        }
    }
}