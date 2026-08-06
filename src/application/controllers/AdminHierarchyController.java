package application.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.Group;
import javafx.scene.transform.Scale;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import application.models.*;
import application.services.WorkflowService;
import application.utils.AlertUtils;
import application.utils.SessionManager;
import javafx.geometry.Orientation;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import java.util.function.Consumer;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Contrôleur AMÉLIORÉ pour la gestion de la hiérarchie administrative
 * NOUVELLES FONCTIONNALITÉS:
 * - Édition complète des services
 * - Sélection de service parent existant
 * - Visualisation graphique de l'organigramme
 */
public class AdminHierarchyController implements Initializable {
    
    // TreeView pour la hiérarchie
    @FXML private TreeView<ServiceHierarchy> hierarchyTreeView;
    
    // Tableau des services
    @FXML private TableView<ServiceHierarchy> servicesTable;
    @FXML private TableColumn<ServiceHierarchy, String> colServiceCode;
    @FXML private TableColumn<ServiceHierarchy, String> colServiceName;
    @FXML private TableColumn<ServiceHierarchy, String> colParent;
    @FXML private TableColumn<ServiceHierarchy, String> colNiveau;
    @FXML private TableColumn<ServiceHierarchy, String> colActif;
    
    // Champs du formulaire
    @FXML private TextField tfServiceCode;
    @FXML private TextField tfServiceName;
    @FXML private ComboBox<ServiceHierarchy> cbParentService;
    @FXML private ComboBox<String> cbNiveau;
    @FXML private TextField tfOrdreAffichage;
    @FXML private CheckBox chkActif;
    
    // Recherche et filtres
    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbFilterNiveau;
    @FXML private CheckBox chkFilterActifs;
    
    // Boutons
    @FXML private Button btnSaveService;
    @FXML private Button btnDeleteService;
    @FXML private Button btnNewService;
    
    // Statistiques
    @FXML private Label statTotalServices;
    @FXML private Label statServicesActifs;
    @FXML private Label statNiveaux;
    
    // Visualisation
    @FXML private VBox hierarchyVisualization;
    
    // Services
    private WorkflowService workflowService;
    private User currentUser;
    
    // Données
    private ObservableList<ServiceHierarchy> servicesData;
    private ServiceHierarchy selectedService;
    private boolean isEditMode = false;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("AdminHierarchyController.initialize()");
        
        try {
            workflowService = WorkflowService.getInstance();
            currentUser = SessionManager.getInstance().getCurrentUser();
            servicesData = FXCollections.observableArrayList();
            
            if (currentUser == null) {
                System.err.println("ERREUR: Aucun utilisateur en session");
                return;
            }
            
            // Configuration de l'interface
            setupTreeView();
            setupTableView();
            setupForm();
            setupFilters();
            
            // Chargement des données
            loadHierarchy();
            updateStatistics();
            
            System.out.println("✅ AdminHierarchyController initialisé avec succès");
            
        } catch (Exception e) {
            System.err.println("Erreur dans AdminHierarchyController.initialize(): " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur d'initialisation", e.getMessage());
        }
    }
    
    private void setupTreeView() {
        if (hierarchyTreeView == null) return;
        
        hierarchyTreeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(ServiceHierarchy service, boolean empty) {
                super.updateItem(service, empty);
                
                if (empty || service == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String icon = service.getIcone();
                    String text = icon + " " + service.getServiceName() + " (" + service.getServiceCode() + ")";
                    setText(text);
                    setStyle("-fx-text-fill: " + service.getCouleur() + ";");
                }
            }
        });
        
        hierarchyTreeView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null && newVal.getValue() != null) {
                    loadServiceDetails(newVal.getValue());
                }
            }
        );
    }
    
    private void setupTableView() {
        if (servicesTable == null) return;
        
        colServiceCode.setCellValueFactory(new PropertyValueFactory<>("serviceCode"));
        colServiceName.setCellValueFactory(new PropertyValueFactory<>("serviceName"));
        
        colParent.setCellValueFactory(cellData -> {
            String parentCode = cellData.getValue().getParentServiceCode();
            return new SimpleStringProperty(parentCode != null ? parentCode : "Racine");
        });
        
        colNiveau.setCellValueFactory(cellData -> 
            new SimpleStringProperty("Niveau " + cellData.getValue().getNiveau())
        );
        
        colActif.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().isActif() ? "✅ Oui" : "❌ Non")
        );
        
        servicesTable.setItems(servicesData);
        
        servicesTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    loadServiceDetails(newVal);
                }
            }
        );
    }
    
    /**
     * AMÉLIORATION: Configure le formulaire avec services parents existants uniquement
     */
    private void setupForm() {
        // Niveaux hiérarchiques
        if (cbNiveau != null) {
            cbNiveau.setItems(FXCollections.observableArrayList(
                "Niveau -1: Service Courrier",
                "Niveau 0: CEMAA, CSP",
                "Niveau 1: MAGE, CSA",
                "Niveau 2: Sous-directions",
                "Niveau 3: Cellules",
                "Niveau 4: Chefs d'Équipe",
                "Niveau 5: Chefs d'Équipe Adjoints"
            ));
        }
        
        // Service parent - avec affichage personnalisé
        if (cbParentService != null) {
            cbParentService.setCellFactory(param -> new ListCell<>() {
                @Override
                protected void updateItem(ServiceHierarchy service, boolean empty) {
                    super.updateItem(service, empty);
                    if (empty || service == null) {
                        setText(null);
                    } else {
                        setText(service.getIcone() + " " + service.getServiceName() + 
                               " (" + service.getServiceCode() + ") - Niveau " + service.getNiveau());
                    }
                }
            });
            
            cbParentService.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(ServiceHierarchy service, boolean empty) {
                    super.updateItem(service, empty);
                    if (empty || service == null) {
                        setText("Aucun (Service racine)");
                    } else {
                        setText(service.getServiceName() + " (" + service.getServiceCode() + ")");
                    }
                }
            });
        }
        
        // Désactiver les champs par défaut
        setFieldsEditable(false);
    }
    
    private void setupFilters() {
        if (cbFilterNiveau != null) {
            cbFilterNiveau.setItems(FXCollections.observableArrayList(
                "Tous les niveaux",
                "Niveau -1", "Niveau 0", "Niveau 1", "Niveau 2", 
                "Niveau 3", "Niveau 4", "Niveau 5"
            ));
            cbFilterNiveau.setValue("Tous les niveaux");
        }
        
        if (chkFilterActifs != null) {
            chkFilterActifs.setSelected(true);
        }
    }
    
    private void loadHierarchy() {
        try {
            workflowService.loadHierarchyCache();
            loadTreeView();
            loadTableView();
            
            System.out.println("✅ Hiérarchie chargée avec succès");
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de la hiérarchie: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur", "Impossible de charger la hiérarchie: " + e.getMessage());
        }
    }
    
    private void loadTreeView() {
        if (hierarchyTreeView == null) return;
        
        TreeItem<ServiceHierarchy> root = new TreeItem<>(null);
        root.setExpanded(true);
        
        List<ServiceHierarchy> rootServices = workflowService.getRootServices();
        
        for (ServiceHierarchy service : rootServices) {
            TreeItem<ServiceHierarchy> item = createTreeItem(service);
            root.getChildren().add(item);
        }
        
        hierarchyTreeView.setRoot(root);
        hierarchyTreeView.setShowRoot(false);
    }
    
    private TreeItem<ServiceHierarchy> createTreeItem(ServiceHierarchy service) {
        TreeItem<ServiceHierarchy> item = new TreeItem<>(service);
        item.setExpanded(false);
        
        for (ServiceHierarchy enfant : service.getEnfants()) {
            TreeItem<ServiceHierarchy> childItem = createTreeItem(enfant);
            item.getChildren().add(childItem);
        }
        
        return item;
    }
    
    private void loadTableView() {
        if (servicesTable == null) return;
        
        List<ServiceHierarchy> allServices = workflowService.getAllServices();
        servicesData.clear();
        servicesData.addAll(allServices);
        
        applyFilters();
    }
    
    /**
     * AMÉLIORATION: Charge les détails d'un service avec possibilité d'édition
     */
    private void loadServiceDetails(ServiceHierarchy service) {
        selectedService = service;
        isEditMode = false;
        
        if (tfServiceCode != null) tfServiceCode.setText(service.getServiceCode());
        if (tfServiceName != null) tfServiceName.setText(service.getServiceName());
        
        // AMÉLIORATION: Charger TOUS les services existants pour le parent
        if (cbParentService != null) {
            List<ServiceHierarchy> allServices = workflowService.getAllServices();
            
            // Exclure le service lui-même et ses descendants
            List<ServiceHierarchy> availableParents = allServices.stream()
                .filter(s -> !s.equals(service))
                .filter(s -> !s.estDescendantDe(service))
                .filter(ServiceHierarchy::isActif)
                .collect(Collectors.toList());
            
            cbParentService.setItems(FXCollections.observableArrayList(availableParents));
            
            // Sélectionner le parent actuel
            if (service.getParent() != null) {
                cbParentService.setValue(service.getParent());
            } else {
                cbParentService.setValue(null);
            }
        }
        
        // Niveau
        if (cbNiveau != null) {
            String niveauStr = "Niveau " + service.getNiveau();
            for (String item : cbNiveau.getItems()) {
                if (item.startsWith(niveauStr)) {
                    cbNiveau.setValue(item);
                    break;
                }
            }
        }
        
        if (tfOrdreAffichage != null) {
            tfOrdreAffichage.setText(String.valueOf(service.getOrdreAffichage()));
        }
        
        if (chkActif != null) {
            chkActif.setSelected(service.isActif());
        }
        
        // Activer les boutons
        if (btnDeleteService != null) {
            btnDeleteService.setDisable(false);
        }
        
        if (btnSaveService != null) {
            btnSaveService.setText("💾 Modifier le Service");
        }
        
        setFieldsEditable(true);
    }
    
    private void setFieldsEditable(boolean editable) {
        if (tfServiceCode != null) tfServiceCode.setEditable(editable);
        if (tfServiceName != null) tfServiceName.setEditable(editable);
        if (cbParentService != null) cbParentService.setDisable(!editable);
        if (cbNiveau != null) cbNiveau.setDisable(!editable);
        if (tfOrdreAffichage != null) tfOrdreAffichage.setDisable(!editable);
        if (chkActif != null) chkActif.setDisable(!editable);
    }
    
    private void updateStatistics() {
        List<ServiceHierarchy> allServices = workflowService.getAllServices();
        
        if (statTotalServices != null) {
            statTotalServices.setText(String.valueOf(allServices.size()));
        }
        
        if (statServicesActifs != null) {
            long actifs = allServices.stream().filter(ServiceHierarchy::isActif).count();
            statServicesActifs.setText(String.valueOf(actifs));
        }
        
        if (statNiveaux != null) {
            int maxNiveau = allServices.stream()
                .mapToInt(ServiceHierarchy::getNiveau)
                .max()
                .orElse(0) + 1;
            statNiveaux.setText(String.valueOf(maxNiveau));
        }
    }
    
    // ==================== HANDLERS ====================
    
    @FXML
    private void handleRefresh() {
        loadHierarchy();
        updateStatistics();
        AlertUtils.showInfo("Hiérarchie actualisée avec succès");
    }
    
    @FXML
    private void handleExportHierarchy() {
        AlertUtils.showInfo("Fonction d'export", 
            "L'export de la hiérarchie sera disponible prochainement.\n" +
            "Format : CSV avec tous les services et leurs relations.");
    }
    
    @FXML
    private void handleImportHierarchy() {
        AlertUtils.showInfo("Fonction d'import", 
            "L'import de la hiérarchie sera disponible prochainement.\n" +
            "Format accepté : CSV avec colonnes code, nom, parent, niveau.");
    }
    
    /**
     * NOUVEAU: Affiche une visualisation graphique de l'organigramme
     */
    @FXML
    private void handleShowVisualization() {
        try {
            new OrganigrammeView().show();
        } catch (Exception e) {
            System.err.println("Erreur lors de la visualisation: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Impossible d'afficher la visualisation");
        }
    }

    /**
     * Vue interactive de l'organigramme.
     *
     * Rendu en positionnement absolu (Pane + connecteurs dessinés) afin d'obtenir
     * un espacement lisible et professionnel — corrige l'affichage « resserré ».
     *
     * Fonctionnalités :
     *  - zoom (boutons, curseur, molette Ctrl, « Ajuster à la fenêtre ») et déplacement (pan) ;
     *  - repli / dépli des branches (badge « N sous-services » cliquable) ;
     *  - « Tout déplier » / « Tout replier » ;
     *  - recherche instantanée avec surbrillance et estompage des cartes non concernées ;
     *  - clic sur une carte : mise en évidence de toute la lignée (jusqu'à la racine) ;
     *  - infobulle détaillée par service et légende des niveaux.
     *
     * Cette classe est autonome et ne modifie pas la logique métier existante.
     */
    private class OrganigrammeView {

        // --- Paramètres de mise en page (en pixels) ---
        private final double CARD_W   = 210;  // largeur d'une carte
        private final double CARD_H   = 104;  // hauteur d'une carte
        private final double H_GAP    = 30;   // écart horizontal entre sous-arbres frères
        private final double V_GAP    = 68;   // écart vertical entre niveaux
        private final double PAD       = 70;  // marge autour de l'organigramme
        private final double ROOT_GAP  = 80;  // écart entre deux arbres racines

        // --- État de la vue ---
        private final Set<String> collapsed = new HashSet<>();
        private String selectedCode = null;
        private String searchQuery = "";
        private double zoom = 1.0;

        // --- Résultats de calcul de disposition (recalculés à chaque rendu) ---
        private final Map<String, Double> width = new HashMap<>();
        private final Map<String, Double> centerX = new HashMap<>();
        private final Map<String, Double> topY = new HashMap<>();
        private double totalW = 0, totalH = 0;
        private int maxDepth = 0;

        // --- Composants graphiques ---
        private final Pane canvas = new Pane();
        private final Scale scaleTx = new Scale(1, 1, 0, 0); // zoom ancré en haut-gauche
        private ScrollPane scrollPane;
        private Label zoomLabel;
        private Slider zoomSlider;
        private TextField searchField;

        // ────────────────────────────────────────────────────────────────
        // Fenêtre
        // ────────────────────────────────────────────────────────────────
        void show() {
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Organigramme — Structure hiérarchique des services");

            BorderPane root = new BorderPane();
            root.setStyle("-fx-background-color:#eef1f5;");
            root.setTop(buildToolbar(stage));

            canvas.setStyle("-fx-background-color:transparent;");
            canvas.getTransforms().add(scaleTx);

            // Un Group répercute les bornes transformées (zoom) au ScrollPane :
            // les barres de défilement restent correctes quel que soit le zoom.
            Group zoomGroup = new Group(canvas);
            scrollPane = new ScrollPane(zoomGroup);
            scrollPane.setPannable(true);
            scrollPane.setStyle("-fx-background:#eef1f5; -fx-background-color:#eef1f5; -fx-border-color:transparent;");
            scrollPane.setOnScroll(ev -> {
                if (ev.isControlDown()) {
                    ev.consume();
                    setZoom(zoom + (ev.getDeltaY() > 0 ? 0.1 : -0.1));
                }
            });
            root.setCenter(scrollPane);
            root.setBottom(buildLegendBar());

            render();

            Scene scene = new Scene(root, 1280, 860);
            stage.setScene(scene);
            stage.show();

            // Ajustement automatique une fois la fenêtre dimensionnée
            javafx.application.Platform.runLater(this::fitToWindow);
        }

        // ────────────────────────────────────────────────────────────────
        // Barre d'outils (haut)
        // ────────────────────────────────────────────────────────────────
        private HBox buildToolbar(Stage stage) {
            HBox bar = new HBox(10);
            bar.setPadding(new Insets(12, 16, 12, 16));
            bar.setAlignment(Pos.CENTER_LEFT);
            bar.setStyle("-fx-background-color:white; -fx-border-color:#d9dee6; -fx-border-width:0 0 1 0;");

            Label title = new Label("Organigramme des services");
            title.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:#2c3e50;");

            searchField = new TextField();
            searchField.setPromptText("Rechercher (code ou nom)…");
            searchField.setPrefWidth(220);
            searchField.textProperty().addListener((o, ov, nv) -> {
                searchQuery = (nv == null) ? "" : nv.trim().toLowerCase();
                render();
            });

            Button btnExpand = pill("Tout déplier", "#3498db");
            btnExpand.setOnAction(e -> { collapsed.clear(); render(); });
            Button btnCollapse = pill("Tout replier", "#7f8c8d");
            btnCollapse.setOnAction(e -> { collapseAll(); render(); });

            Button btnFit = pill("Ajuster", "#16a085");
            btnFit.setOnAction(e -> fitToWindow());
            Button btnZoomOut = pill("−", "#95a5a6");
            btnZoomOut.setOnAction(e -> setZoom(zoom - 0.1));
            Button btnZoomIn = pill("+", "#2980b9");
            btnZoomIn.setOnAction(e -> setZoom(zoom + 0.1));
            Button btnReset = pill("100 %", "#e67e22");
            btnReset.setOnAction(e -> setZoom(1.0));

            zoomSlider = new Slider(0.3, 2.0, 1.0);
            zoomSlider.setPrefWidth(140);
            zoomSlider.valueProperty().addListener((o, ov, nv) -> {
                if (Math.abs(nv.doubleValue() - zoom) > 0.001) setZoom(nv.doubleValue());
            });
            zoomLabel = new Label("100 %");
            zoomLabel.setStyle("-fx-font-weight:bold; -fx-text-fill:#2c3e50; -fx-min-width:44px;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button btnClose = pill("Fermer", "#c0392b");
            btnClose.setOnAction(e -> stage.close());

            bar.getChildren().addAll(
                title, new Separator(Orientation.VERTICAL), searchField,
                btnExpand, btnCollapse, new Separator(Orientation.VERTICAL),
                btnFit, btnZoomOut, zoomSlider, btnZoomIn, zoomLabel, btnReset,
                spacer, btnClose
            );
            return bar;
        }

        // ────────────────────────────────────────────────────────────────
        // Légende (bas)
        // ────────────────────────────────────────────────────────────────
        private HBox buildLegendBar() {
            HBox bar = new HBox(16);
            bar.setPadding(new Insets(10, 16, 10, 16));
            bar.setAlignment(Pos.CENTER_LEFT);
            bar.setStyle("-fx-background-color:white; -fx-border-color:#d9dee6; -fx-border-width:1 0 0 0;");

            Label lg = new Label("Légende :");
            lg.setStyle("-fx-font-weight:bold; -fx-text-fill:#2c3e50;");
            bar.getChildren().add(lg);

            int[] niveaux = {-1, 0, 1, 2, 3, 4, 5};
            String[] libelles = {
                "Courrier", "Direction", "Major Général", "Sous-directions",
                "Cellules", "Chefs d'équipe", "Adjoints"
            };
            Set<Integer> present = new HashSet<>();
            for (ServiceHierarchy s : workflowService.getAllServices()) {
                present.add(s.getNiveau());
            }
            for (int i = 0; i < niveaux.length; i++) {
                if (!present.contains(niveaux[i])) continue;
                HBox item = new HBox(6);
                item.setAlignment(Pos.CENTER_LEFT);
                Region sw = new Region();
                sw.setMinSize(16, 16);
                sw.setPrefSize(16, 16);
                sw.setStyle("-fx-background-color:" + getBackgroundColorForLevel(niveaux[i]) + ";"
                          + "-fx-border-color:#95a5a6; -fx-border-radius:3; -fx-background-radius:3;");
                Label lb = new Label("N" + niveaux[i] + " · " + libelles[i]);
                lb.setStyle("-fx-font-size:11px; -fx-text-fill:#5d6d7e;");
                item.getChildren().addAll(sw, lb);
                bar.getChildren().add(item);
            }

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label hint = new Label("Ctrl + molette : zoom · glisser : déplacer · clic sur une carte : afficher la lignée");
            hint.setStyle("-fx-font-size:11px; -fx-font-style:italic; -fx-text-fill:#95a5a6;");
            bar.getChildren().addAll(spacer, hint);
            return bar;
        }

        private Button pill(String text, String color) {
            Button b = new Button(text);
            b.setStyle("-fx-background-color:" + color + "; -fx-text-fill:white; -fx-font-weight:bold; "
                     + "-fx-background-radius:6; -fx-cursor:hand; -fx-padding:6 12 6 12;");
            return b;
        }

        // ────────────────────────────────────────────────────────────────
        // Zoom / ajustement
        // ────────────────────────────────────────────────────────────────
        private void setZoom(double z) {
            zoom = Math.max(0.3, Math.min(2.0, z));
            scaleTx.setX(zoom);
            scaleTx.setY(zoom);
            if (zoomLabel != null) zoomLabel.setText(String.format("%.0f %%", zoom * 100));
            if (zoomSlider != null && Math.abs(zoomSlider.getValue() - zoom) > 0.001) {
                zoomSlider.setValue(zoom);
            }
        }

        private void fitToWindow() {
            if (scrollPane == null) return;
            double vw = scrollPane.getViewportBounds().getWidth();
            double vh = scrollPane.getViewportBounds().getHeight();
            if (vw <= 0 || vh <= 0 || totalW <= 0 || totalH <= 0) return;
            double z = Math.min(vw / totalW, vh / totalH);
            z = Math.max(0.3, Math.min(1.0, z)); // ne pas sur-agrandir au-delà de 100 %
            setZoom(z);
        }

        private void collapseAll() {
            collapsed.clear();
            for (ServiceHierarchy s : workflowService.getAllServices()) {
                if (!s.getEnfants().isEmpty()) collapsed.add(s.getServiceCode());
            }
            // Garder les racines dépliées pour un rendu lisible
            for (ServiceHierarchy r : workflowService.getRootServices()) {
                collapsed.remove(r.getServiceCode());
            }
        }

        // ────────────────────────────────────────────────────────────────
        // Rendu
        // ────────────────────────────────────────────────────────────────
        private void render() {
            computeLayout();
            canvas.getChildren().clear();
            canvas.setMinSize(totalW, totalH);
            canvas.setPrefSize(totalW, totalH);
            canvas.setMaxSize(totalW, totalH);

            // Lignée mise en évidence (service sélectionné + ancêtres)
            Set<String> lineage = new HashSet<>();
            if (selectedCode != null) {
                ServiceHierarchy s = workflowService.getServiceByCode(selectedCode);
                while (s != null) {
                    lineage.add(s.getServiceCode());
                    s = s.getParent();
                }
            }

            // Connecteurs d'abord (en dessous), puis cartes (au-dessus)
            for (ServiceHierarchy r : workflowService.getRootServices()) drawConnectors(r, lineage);
            for (ServiceHierarchy r : workflowService.getRootServices()) drawCards(r, lineage);
        }

        private List<ServiceHierarchy> visibleChildren(ServiceHierarchy s) {
            if (collapsed.contains(s.getServiceCode())) return java.util.Collections.emptyList();
            return s.getEnfants();
        }

        private void computeLayout() {
            width.clear();
            centerX.clear();
            topY.clear();
            maxDepth = 0;

            List<ServiceHierarchy> roots = workflowService.getRootServices();
            for (ServiceHierarchy r : roots) computeWidth(r);

            double left = PAD;
            for (ServiceHierarchy r : roots) {
                assign(r, left, 0);
                left += width.get(r.getServiceCode()) + ROOT_GAP;
            }

            totalW = roots.isEmpty() ? (2 * PAD) : (left - ROOT_GAP + PAD);
            totalH = PAD + (maxDepth + 1) * (CARD_H + V_GAP) - V_GAP + PAD;
        }

        private double computeWidth(ServiceHierarchy s) {
            List<ServiceHierarchy> kids = visibleChildren(s);
            double w;
            if (kids.isEmpty()) {
                w = CARD_W;
            } else {
                double sum = 0;
                for (ServiceHierarchy k : kids) sum += computeWidth(k);
                sum += H_GAP * (kids.size() - 1);
                w = Math.max(CARD_W, sum);
            }
            width.put(s.getServiceCode(), w);
            return w;
        }

        private void assign(ServiceHierarchy s, double leftX, int depth) {
            maxDepth = Math.max(maxDepth, depth);
            String code = s.getServiceCode();
            double w = width.get(code);
            topY.put(code, PAD + depth * (CARD_H + V_GAP));

            List<ServiceHierarchy> kids = visibleChildren(s);
            if (kids.isEmpty()) {
                centerX.put(code, leftX + w / 2);
                return;
            }

            double childrenTotal = 0;
            for (ServiceHierarchy k : kids) childrenTotal += width.get(k.getServiceCode());
            childrenTotal += H_GAP * (kids.size() - 1);

            double childLeft = leftX + (w - childrenTotal) / 2;
            double firstCx = 0, lastCx = 0;
            for (int i = 0; i < kids.size(); i++) {
                ServiceHierarchy k = kids.get(i);
                assign(k, childLeft, depth + 1);
                double kcx = centerX.get(k.getServiceCode());
                if (i == 0) firstCx = kcx;
                if (i == kids.size() - 1) lastCx = kcx;
                childLeft += width.get(k.getServiceCode()) + H_GAP;
            }
            centerX.put(code, (firstCx + lastCx) / 2);
        }

        private void drawConnectors(ServiceHierarchy s, Set<String> lineage) {
            List<ServiceHierarchy> kids = visibleChildren(s);
            if (!kids.isEmpty()) {
                double pcx = centerX.get(s.getServiceCode());
                double pBottom = topY.get(s.getServiceCode()) + CARD_H;
                double busY = pBottom + V_GAP / 2;
                boolean parentInLineage = lineage.contains(s.getServiceCode());

                // Segment vertical parent → bus
                addLine(pcx, pBottom, pcx, busY, parentInLineage && anyChildInLineage(kids, lineage));

                // Barre horizontale reliant les enfants
                double minX = pcx, maxX = pcx;
                for (ServiceHierarchy k : kids) {
                    double kcx = centerX.get(k.getServiceCode());
                    minX = Math.min(minX, kcx);
                    maxX = Math.max(maxX, kcx);
                }
                addLine(minX, busY, maxX, busY, false);

                // Bus → chaque enfant
                for (ServiceHierarchy k : kids) {
                    double kcx = centerX.get(k.getServiceCode());
                    double kTop = topY.get(k.getServiceCode());
                    boolean edgeHi = parentInLineage && lineage.contains(k.getServiceCode());
                    addLine(kcx, busY, kcx, kTop, edgeHi);
                }
            }
            for (ServiceHierarchy k : kids) drawConnectors(k, lineage);
        }

        private boolean anyChildInLineage(List<ServiceHierarchy> kids, Set<String> lineage) {
            for (ServiceHierarchy k : kids) {
                if (lineage.contains(k.getServiceCode())) return true;
            }
            return false;
        }

        private void addLine(double x1, double y1, double x2, double y2, boolean highlight) {
            Line ln = new Line(x1, y1, x2, y2);
            if (highlight) {
                ln.setStroke(Color.web("#e67e22"));
                ln.setStrokeWidth(3);
            } else {
                ln.setStroke(Color.web("#b0bac5"));
                ln.setStrokeWidth(1.6);
            }
            canvas.getChildren().add(ln);
        }

        private void drawCards(ServiceHierarchy s, Set<String> lineage) {
            Region card = buildCard(s, lineage);
            card.setLayoutX(centerX.get(s.getServiceCode()) - CARD_W / 2);
            card.setLayoutY(topY.get(s.getServiceCode()));
            canvas.getChildren().add(card);
            for (ServiceHierarchy k : visibleChildren(s)) drawCards(k, lineage);
        }

        private Region buildCard(ServiceHierarchy s, Set<String> lineage) {
            VBox card = new VBox(4);
            card.setAlignment(Pos.CENTER);
            card.setPrefSize(CARD_W, CARD_H);
            card.setMinSize(CARD_W, CARD_H);
            card.setMaxSize(CARD_W, CARD_H);
            card.setPadding(new Insets(10, 12, 10, 12));

            String bg = getBackgroundColorForLevel(s.getNiveau());
            String accent = s.getCouleur();
            boolean isSelected = s.getServiceCode().equals(selectedCode);
            boolean inLineage = lineage.contains(s.getServiceCode());
            boolean matches = !searchQuery.isEmpty()
                && (s.getServiceCode().toLowerCase().contains(searchQuery)
                    || (s.getServiceName() != null && s.getServiceName().toLowerCase().contains(searchQuery)));
            boolean dimmed = !searchQuery.isEmpty() && !matches;

            String border = matches ? "#f1c40f" : ((isSelected || inLineage) ? "#e67e22" : accent);
            double borderW = (matches || isSelected) ? 3.5 : (inLineage ? 3 : 2);

            String base = "-fx-background-color:" + bg + ";"
                + "-fx-background-radius:12;"
                + "-fx-border-color:" + border + ";"
                + "-fx-border-width:" + borderW + ";"
                + "-fx-border-radius:12;"
                + "-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.18), 8, 0, 0, 3);";
            card.setStyle(base);
            if (dimmed) card.setOpacity(0.35);

            HBox header = new HBox(6);
            header.setAlignment(Pos.CENTER);
            Label icon = new Label(s.getIcone());
            icon.setStyle("-fx-font-size:18px;");
            Label code = new Label(s.getServiceCode());
            code.setStyle("-fx-font-weight:bold; -fx-font-size:14px; -fx-text-fill:#2c3e50;");
            header.getChildren().addAll(icon, code);

            Label name = new Label(s.getServiceName());
            name.setWrapText(true);
            name.setMaxWidth(CARD_W - 24);
            name.setAlignment(Pos.CENTER);
            name.setStyle("-fx-font-size:11px; -fx-text-fill:#34495e; -fx-text-alignment:center;");

            card.getChildren().addAll(header, name);

            int nb = s.getEnfants().size();
            if (nb > 0) {
                boolean isCollapsed = collapsed.contains(s.getServiceCode());
                Label toggle = new Label(
                    (isCollapsed ? "▸ " : "▾ ") + nb + (nb > 1 ? " sous-services" : " sous-service"));
                toggle.setStyle("-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:white; "
                    + "-fx-background-color:" + accent + "; -fx-background-radius:10; "
                    + "-fx-padding:2 8 2 8; -fx-cursor:hand;");
                toggle.setOnMouseClicked(ev -> {
                    ev.consume(); // ne pas déclencher la sélection de la carte
                    if (collapsed.contains(s.getServiceCode())) collapsed.remove(s.getServiceCode());
                    else collapsed.add(s.getServiceCode());
                    render();
                });
                card.getChildren().add(toggle);
            }

            Tooltip tip = new Tooltip(
                s.getServiceName() + " (" + s.getServiceCode() + ")\n"
                + "Niveau : " + s.getNiveau() + "\n"
                + "Parent : " + (s.getParent() != null ? s.getParent().getServiceCode() : "—") + "\n"
                + "Sous-services : " + nb + " · Descendants : " + s.getNombreDescendants());
            tip.setShowDelay(javafx.util.Duration.millis(250));
            Tooltip.install(card, tip);

            card.setOnMouseEntered(ev -> card.setStyle(base
                + "-fx-effect:dropshadow(gaussian, rgba(0,0,0,0.28), 14, 0, 0, 5);"
                + "-fx-scale-x:1.03; -fx-scale-y:1.03;"));
            card.setOnMouseExited(ev -> card.setStyle(base));

            card.setOnMouseClicked(ev -> {
                selectedCode = s.getServiceCode().equals(selectedCode) ? null : s.getServiceCode();
                render();
            });

            return card;
        }
    }

    /**
     * Retourne une couleur de fond selon le niveau hiérarchique
     */
    private String getBackgroundColorForLevel(int niveau) {
        switch (niveau) {
            case -1: return "#fef5e7";  // Jaune pâle (courrier)
            case 0:  return "#fadbd8";  // Rouge pâle (direction)
            case 1:  return "#d6eaf8";  // Bleu pâle (sous-direction)
            case 2:  return "#d5f4e6";  // Vert pâle (services)
            case 3:  return "#fdebd0";  // Orange pâle (cellules)
            case 4:  return "#e8daef";  // Violet pâle (chefs équipe)
            case 5:  return "#d5dbdb";  // Gris pâle (adjoints)
            default: return "#ffffff";
        }
    }
    
    @FXML
    private void handleSearch() {
        applyFilters();
    }
    
    @FXML
    private void handleClearFilters() {
        if (tfSearch != null) tfSearch.clear();
        if (cbFilterNiveau != null) cbFilterNiveau.setValue("Tous les niveaux");
        if (chkFilterActifs != null) chkFilterActifs.setSelected(true);
        applyFilters();
    }
    
    @FXML
    private void handleNewService() {
        selectedService = null;
        isEditMode = false;
        
        // Vider les champs
        if (tfServiceCode != null) tfServiceCode.clear();
        if (tfServiceName != null) tfServiceName.clear();
        if (cbParentService != null) {
            // Charger TOUS les services actifs comme parents potentiels
            List<ServiceHierarchy> allServices = workflowService.getAllServices()
                .stream()
                .filter(ServiceHierarchy::isActif)
                .collect(Collectors.toList());
            cbParentService.setItems(FXCollections.observableArrayList(allServices));
            cbParentService.setValue(null);
        }
        if (cbNiveau != null) cbNiveau.setValue(null);
        if (tfOrdreAffichage != null) tfOrdreAffichage.setText("1");
        if (chkActif != null) chkActif.setSelected(true);
        
        setFieldsEditable(true);
        
        if (btnSaveService != null) {
            btnSaveService.setText("💾 Créer le Service");
        }
        
        if (btnDeleteService != null) {
            btnDeleteService.setDisable(true);
        }
    }
    
    @FXML
    private void handleSaveService() {
        try {
            // Validation
            if (tfServiceCode == null || tfServiceCode.getText().trim().isEmpty()) {
                AlertUtils.showWarning("Le code du service est obligatoire");
                return;
            }
            
            if (tfServiceName == null || tfServiceName.getText().trim().isEmpty()) {
                AlertUtils.showWarning("Le nom du service est obligatoire");
                return;
            }
            
            if (cbNiveau == null || cbNiveau.getValue() == null) {
                AlertUtils.showWarning("Le niveau hiérarchique est obligatoire");
                return;
            }
            
            // Extraire le numéro de niveau
            String niveauStr = cbNiveau.getValue();
            int niveau = Integer.parseInt(niveauStr.substring(niveauStr.indexOf("Niveau ") + 7, 
                                         niveauStr.indexOf(":")));
            
            // Créer ou mettre à jour
            if (selectedService == null) {
                AlertUtils.showInfo("Création de service", 
                    "La création de service nécessite une implémentation en base de données.\n" +
                    "Fonctionnalité en cours de développement.");
            } else {
                // Mise à jour
                selectedService.setServiceCode(tfServiceCode.getText().trim());
                selectedService.setServiceName(tfServiceName.getText().trim());
                selectedService.setNiveau(niveau);
                
                if (cbParentService.getValue() != null) {
                    selectedService.setParentServiceCode(cbParentService.getValue().getServiceCode());
                }
                
                if (tfOrdreAffichage.getText() != null && !tfOrdreAffichage.getText().trim().isEmpty()) {
                    selectedService.setOrdreAffichage(Integer.parseInt(tfOrdreAffichage.getText().trim()));
                }
                
                selectedService.setActif(chkActif.isSelected());
                
                AlertUtils.showInfo("Service sauvegardé", 
                    "Les modifications ont été appliquées.\n" +
                    "Note: La persistance en base nécessite une implémentation supplémentaire.");
                
                loadHierarchy();
                setFieldsEditable(false);
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur", "Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleDeleteService() {
        if (selectedService == null) {
            AlertUtils.showWarning("Aucun service sélectionné");
            return;
        }
        
        // Vérifier qu'il n'a pas d'enfants
        if (!selectedService.getEnfants().isEmpty()) {
            AlertUtils.showWarning("Impossible de supprimer ce service car il a des services enfants");
            return;
        }
        
        boolean confirm = AlertUtils.showConfirmation(
            "Confirmation",
            "Êtes-vous sûr de vouloir supprimer le service " + selectedService.getServiceName() + " ?"
        );
        
        if (confirm) {
            AlertUtils.showInfo("Suppression de service", 
                "La suppression en base de données nécessite une implémentation.\n" +
                "Fonctionnalité en cours de développement.");
            loadHierarchy();
        }
    }
    
    private void applyFilters() {
        if (servicesTable == null) return;
        
        List<ServiceHierarchy> allServices = workflowService.getAllServices();
        List<ServiceHierarchy> filtered = new ArrayList<>();
        
        String searchText = tfSearch != null ? tfSearch.getText().toLowerCase() : "";
        String niveauFilter = cbFilterNiveau != null ? cbFilterNiveau.getValue() : "Tous les niveaux";
        boolean onlyActifs = chkFilterActifs != null && chkFilterActifs.isSelected();
        
        for (ServiceHierarchy service : allServices) {
            boolean matches = true;
            
            // Filtre recherche
            if (!searchText.isEmpty()) {
                if (!service.getServiceCode().toLowerCase().contains(searchText) &&
                    !service.getServiceName().toLowerCase().contains(searchText)) {
                    matches = false;
                }
            }
            
            // Filtre niveau
            if (niveauFilter != null && !niveauFilter.equals("Tous les niveaux")) {
                int niveau = Integer.parseInt(niveauFilter.replace("Niveau ", ""));
                if (service.getNiveau() != niveau) {
                    matches = false;
                }
            }
            
            // Filtre actifs uniquement
            if (onlyActifs && !service.isActif()) {
                matches = false;
            }
            
            if (matches) {
                filtered.add(service);
            }
        }
        
        servicesData.clear();
        servicesData.addAll(filtered);
    }
}