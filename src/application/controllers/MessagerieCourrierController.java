package application.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

import application.models.Courrier;
import application.models.CotationCourrier;
import application.models.User;
import application.services.CourrierService;
import application.services.CotationService;
import application.utils.SessionManager;
import application.utils.AlertUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Contrôleur pour la messagerie des courriers
 * Affiche les courriers assignés à l'utilisateur connecté
 */
public class MessagerieCourrierController implements Initializable {
    
    // ============================================================================
    // COMPOSANTS FXML - FILTRES
    // ============================================================================
    
    @FXML private ComboBox<String> filtreStatutCotation;
    @FXML private CheckBox filtreEnRetard;
    @FXML private CheckBox filtreNonLus;
    @FXML private TextField champRecherche;
    
    // ============================================================================
    // COMPOSANTS FXML - TABLEAU
    // ============================================================================
    
    @FXML private TableView<CotationCourrier> tableauCotations;
    @FXML private TableColumn<CotationCourrier, String> colonneCode;
    @FXML private TableColumn<CotationCourrier, String> colonneObjet;
    @FXML private TableColumn<CotationCourrier, String> colonnePriorite;
    @FXML private TableColumn<CotationCourrier, String> colonneStatut;
    @FXML private TableColumn<CotationCourrier, String> colonneEcheance;
    @FXML private TableColumn<CotationCourrier, String> colonneDelai;
    @FXML private TableColumn<CotationCourrier, String> colonneCotePar;
    @FXML private TableColumn<CotationCourrier, String> colonneDateCotation;
    
    // ============================================================================
    // COMPOSANTS FXML - DÉTAILS
    // ============================================================================
    
    @FXML private VBox panneauDetails;
    @FXML private Label labelCodeCourrier;
    @FXML private Label labelObjetCourrier;
    @FXML private Label labelExpediteur;
    @FXML private Label labelDestinataire;
    @FXML private Label labelPriorite;
    @FXML private Label labelStatutCotation;
    @FXML private Label labelEcheance;
    @FXML private Label labelDelai;
    @FXML private Label labelCotePar;
    @FXML private TextArea textAreaInstructions;
    @FXML private TextArea textAreaCommentaireTraitement;
    
    // ============================================================================
    // COMPOSANTS FXML - ACTIONS
    // ============================================================================
    
    @FXML private Button btnPrendreEnCharge;
    @FXML private Button btnMarquerTraite;
    @FXML private Button btnTraiterSelection;
    @FXML private Button btnActualiser;
    @FXML private Label labelNombreCotations;
    @FXML private Label labelNombreEnRetard;
    @FXML private Label labelNombreSelection;
    
    // ============================================================================
    // DONNÉES
    // ============================================================================
    
    private User currentUser;
    private CourrierService courrierService;
    private CotationService cotationService;
    private ObservableList<CotationCourrier> cotations;
    private CotationCourrier selectedCotation;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("MessagerieCourrierController.initialize() - Début");
        
        try {
            // Initialisation des services
            currentUser = SessionManager.getInstance().getCurrentUser();
            courrierService = CourrierService.getInstance();
            cotationService = CotationService.getInstance();
            cotations = FXCollections.observableArrayList();
            
            if (currentUser == null) {
                System.err.println("ERREUR: Aucun utilisateur en session");
                return;
            }
            
            // Configuration de l'interface
            setupTableColumns();
            setupFilters();
            setupButtons();
            setupSelectionMode();
            
            // Chargement des données
            loadMesCotations();
            
            System.out.println("MessagerieCourrierController.initialize() - Terminé");
            
        } catch (Exception e) {
            System.err.println("Erreur initialisation MessagerieCourrierController: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ============================================================================
    // CONFIGURATION DE L'INTERFACE
    // ============================================================================
    
    /**
     * Configure les colonnes du tableau
     */
    private void setupTableColumns() {
        colonneCode.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getCourrierCode()));
        
        colonneObjet.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getCourrierObjet()));
        
        colonnePriorite.setCellValueFactory(cellData -> 
            new SimpleStringProperty(
                cellData.getValue().getPrioriteIcone() + " " + 
                cellData.getValue().getPrioriteLibelle()
            ));
        
        colonneStatut.setCellValueFactory(cellData -> 
            new SimpleStringProperty(
                cellData.getValue().getStatutIcone() + " " + 
                cellData.getValue().getStatutLibelle()
            ));
        
        colonneEcheance.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDateEcheanceFormatee()));
        
        colonneDelai.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDelaiDescription()));
        
        colonneCotePar.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getCoteurNom()));
        
        colonneDateCotation.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDateCotationFormatee()));
        
        // Coloration des lignes en retard
        tableauCotations.setRowFactory(tv -> new TableRow<CotationCourrier>() {
            @Override
            protected void updateItem(CotationCourrier item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty || item == null) {
                    setStyle("");
                } else if (item.isEnRetard()) {
                    setStyle("-fx-background-color: #ffcccc;");
                } else if (item.getJoursRestants() <= 1) {
                    setStyle("-fx-background-color: #fff3cd;");
                } else {
                    setStyle("");
                }
            }
        });
        
        // Listener de sélection
        tableauCotations.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    showCotationDetails(newSelection);
                }
            }
        );
    }
    
    /**
     * Configure les filtres
     */
    private void setupFilters() {
        // Filtre statut
        filtreStatutCotation.setItems(FXCollections.observableArrayList(
            "Tous", "En attente", "En cours", "Traité"
        ));
        filtreStatutCotation.setValue("Tous");
        filtreStatutCotation.setOnAction(e -> applyFilters());
        
        // Filtres checkbox
        if (filtreEnRetard != null) filtreEnRetard.setOnAction(e -> applyFilters());
        if (filtreNonLus != null) filtreNonLus.setOnAction(e -> applyFilters());
    }
    
    /**
     * Configure les boutons
     */
    private void setupButtons() {
        if (btnPrendreEnCharge != null) btnPrendreEnCharge.setOnAction(e -> handlePrendreEnCharge());
        if (btnMarquerTraite != null) btnMarquerTraite.setOnAction(e -> handleMarquerTraite());
        if (btnTraiterSelection != null) btnTraiterSelection.setOnAction(e -> handleTraiterSelection());
        if (btnActualiser != null) btnActualiser.setOnAction(e -> handleActualiser());
    }
    
    /**
     * Configure la sélection multiple
     */
    private void setupSelectionMode() {
        tableauCotations.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        // Mise à jour du label de sélection
        tableauCotations.getSelectionModel().getSelectedItems().addListener(
            (javafx.collections.ListChangeListener.Change<? extends CotationCourrier> c) -> {
                int nombreSelection = tableauCotations.getSelectionModel().getSelectedItems().size();
                if (labelNombreSelection != null) {
                    labelNombreSelection.setText(nombreSelection + " sélectionné(s)");
                }
                if (btnTraiterSelection != null) {
                    btnTraiterSelection.setDisable(nombreSelection == 0);
                }
            }
        );
    }
    
    // ============================================================================
    // CHARGEMENT DES DONNÉES
    // ============================================================================
    
    /**
     * Charge les cotations assignées à l'utilisateur connecté
     */
    private void loadMesCotations() {
        try {
            List<CotationCourrier> list = cotationService.getCotationsAssigneesA(currentUser.getId());
            
            cotations.clear();
            cotations.addAll(list);
            tableauCotations.setItems(cotations);
            
            // Mise à jour des statistiques
            updateStatistiques();
            
            System.out.println("✓ " + cotations.size() + " cotations chargées");
            
        } catch (Exception e) {
            System.err.println("Erreur chargement cotations: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur lors du chargement des cotations");
        }
    }
    
    /**
     * Met à jour les statistiques affichées
     */
    private void updateStatistiques() {
        if (labelNombreCotations != null) {
            labelNombreCotations.setText("(" + cotations.size() + " cotations)");
        }
        
        if (labelNombreEnRetard != null) {
            long nombreEnRetard = cotations.stream().filter(CotationCourrier::isEnRetard).count();
            labelNombreEnRetard.setText(nombreEnRetard + " en retard");
            
            if (nombreEnRetard > 0) {
                labelNombreEnRetard.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            } else {
                labelNombreEnRetard.setStyle("-fx-text-fill: #27ae60;");
            }
        }
    }
    
    // ============================================================================
    // FILTRAGE
    // ============================================================================
    
    /**
     * Applique les filtres sélectionnés
     */
    @FXML
    private void applyFilters() {
        try {
            String statutFilter = filtreStatutCotation.getValue();
            boolean filtrerRetard = filtreEnRetard != null && filtreEnRetard.isSelected();
            String searchText = champRecherche != null ? champRecherche.getText().toLowerCase() : "";
            
            List<CotationCourrier> allCotations = cotationService.getCotationsAssigneesA(currentUser.getId());
            
            List<CotationCourrier> filtered = allCotations.stream()
                .filter(c -> {
                    // Filtre statut
                    if (!statutFilter.equals("Tous")) {
                        if (!c.getStatutLibelle().equals(statutFilter)) {
                            return false;
                        }
                    }
                    
                    // Filtre en retard
                    if (filtrerRetard && !c.isEnRetard()) {
                        return false;
                    }
                    
                    // Recherche textuelle
                    if (!searchText.isEmpty()) {
                        boolean textMatch = 
                            (c.getCourrierCode() != null && c.getCourrierCode().toLowerCase().contains(searchText)) ||
                            (c.getCourrierObjet() != null && c.getCourrierObjet().toLowerCase().contains(searchText)) ||
                            (c.getCoteurNom() != null && c.getCoteurNom().toLowerCase().contains(searchText));
                        
                        if (!textMatch) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
            
            cotations.clear();
            cotations.addAll(filtered);
            
            updateStatistiques();
            
        } catch (Exception e) {
            System.err.println("Erreur application filtres: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleRecherche() {
        applyFilters();
    }
    
    // ============================================================================
    // AFFICHAGE DES DÉTAILS
    // ============================================================================
    
    /**
     * Affiche les détails d'une cotation sélectionnée
     */
    private void showCotationDetails(CotationCourrier cotation) {
        selectedCotation = cotation;
        
        if (labelCodeCourrier != null) labelCodeCourrier.setText(cotation.getCourrierCode());
        if (labelObjetCourrier != null) labelObjetCourrier.setText(cotation.getCourrierObjet());
        
        // Récupérer le courrier complet pour plus de détails
        Courrier courrier = courrierService.getCourrierById(cotation.getCourrierId());
        if (courrier != null) {
            if (labelExpediteur != null) labelExpediteur.setText(courrier.getExpediteur());
            if (labelDestinataire != null) labelDestinataire.setText(courrier.getDestinataire());
        }
        
        if (labelPriorite != null) {
            labelPriorite.setText(cotation.getPrioriteIcone() + " " + cotation.getPrioriteLibelle());
        }
        
        if (labelStatutCotation != null) {
            labelStatutCotation.setText(cotation.getStatutIcone() + " " + cotation.getStatutLibelle());
        }
        
        if (labelEcheance != null) {
            labelEcheance.setText(cotation.getDateEcheanceAvecHeureFormatee());
        }
        
        if (labelDelai != null) {
            labelDelai.setText(cotation.getDelaiDescription());
            labelDelai.setStyle("-fx-text-fill: " + cotation.getDelaiCouleur() + "; -fx-font-weight: bold;");
        }
        
        if (labelCotePar != null) {
            labelCotePar.setText(cotation.getCoteurNom());
        }
        
        if (textAreaInstructions != null) {
            textAreaInstructions.setText(cotation.getCommentaire() != null ? cotation.getCommentaire() : "");
        }
        
        if (textAreaCommentaireTraitement != null) {
            textAreaCommentaireTraitement.setText(
                cotation.getCommentaireTraitement() != null ? cotation.getCommentaireTraitement() : ""
            );
        }
        
        // Activer/désactiver les boutons selon le statut
        updateButtonStates();
    }
    
    /**
     * Met à jour l'état des boutons selon la cotation sélectionnée
     */
    private void updateButtonStates() {
        if (selectedCotation == null) {
            if (btnPrendreEnCharge != null) btnPrendreEnCharge.setDisable(true);
            if (btnMarquerTraite != null) btnMarquerTraite.setDisable(true);
            return;
        }
        
        if (btnPrendreEnCharge != null) {
            btnPrendreEnCharge.setDisable(!selectedCotation.peutEtrePriseEnCharge());
        }
        
        if (btnMarquerTraite != null) {
            btnMarquerTraite.setDisable(!selectedCotation.peutEtreTraitee());
        }
    }
    
    // ============================================================================
    // GESTION DES ACTIONS
    // ============================================================================
    
    /**
     * Prendre en charge une cotation
     */
    @FXML
    private void handlePrendreEnCharge() {
        if (selectedCotation == null) {
            AlertUtils.showWarning("Veuillez sélectionner une cotation");
            return;
        }
        
        boolean success = cotationService.prendreEnCharge(selectedCotation.getId(), currentUser.getId());
        
        if (success) {
            AlertUtils.showInfo("Cotation prise en charge");
            loadMesCotations();
        } else {
            AlertUtils.showError("Erreur lors de la prise en charge");
        }
    }
    
    /**
     * Marquer une cotation comme traitée
     */
    @FXML
    private void handleMarquerTraite() {
        if (selectedCotation == null) {
            AlertUtils.showWarning("Veuillez sélectionner une cotation");
            return;
        }
        
        // Dialogue pour le commentaire de traitement
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Marquer comme traité");
        dialog.setHeaderText("Traitement du courrier " + selectedCotation.getCourrierCode());
        dialog.setContentText("Commentaire de traitement:");
        
        Optional<String> result = dialog.showAndWait();
        
        result.ifPresent(commentaire -> {
            boolean success = cotationService.marquerTraitee(
                selectedCotation.getId(), 
                currentUser.getId(), 
                commentaire
            );
            
            if (success) {
                AlertUtils.showInfo("Cotation marquée comme traitée");
                loadMesCotations();
            } else {
                AlertUtils.showError("Erreur lors du traitement");
            }
        });
    }
    
    /**
     * Traiter plusieurs cotations sélectionnées en batch
     */
    @FXML
    private void handleTraiterSelection() {
        ObservableList<CotationCourrier> selection = 
            tableauCotations.getSelectionModel().getSelectedItems();
        
        if (selection.isEmpty()) {
            AlertUtils.showWarning("Veuillez sélectionner au moins une cotation");
            return;
        }
        
        // Vérifier que toutes les cotations peuvent être traitées
        List<CotationCourrier> nonTraitables = selection.stream()
            .filter(c -> !c.peutEtreTraitee())
            .collect(Collectors.toList());
        
        if (!nonTraitables.isEmpty()) {
            AlertUtils.showWarning(
                nonTraitables.size() + " cotation(s) ne peuvent pas être traitées.\n" +
                "Seules les cotations 'en attente' ou 'en cours' peuvent être traitées."
            );
            return;
        }
        
        // Dialogue pour le commentaire
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Traitement en batch");
        dialog.setHeaderText("Traiter " + selection.size() + " cotations");
        dialog.setContentText("Commentaire de traitement:");
        
        Optional<String> result = dialog.showAndWait();
        
        result.ifPresent(commentaire -> {
            List<CotationCourrier> list = new ArrayList<>(selection);
            CotationService.BatchOperationResult batchResult = 
                cotationService.traiterCotationsEnBatch(list, currentUser, commentaire);
            
            // Afficher le résultat
            String message = String.format(
                "Traitement terminé:\n\n" +
                "Total: %d cotations\n" +
                "Réussis: %d\n" +
                "Échecs: %d\n" +
                "Taux de réussite: %.1f%%",
                batchResult.getTotalCourriers(),
                batchResult.getCourriersTraites(),
                batchResult.getCourrierEchecs(),
                batchResult.getTauxReussite()
            );
            
            if (batchResult.hasErrors()) {
                message += "\n\nErreurs:\n" + String.join("\n", batchResult.getErreurs());
                AlertUtils.showWarning(message);
            } else {
                AlertUtils.showInfo(message);
            }
            
            loadMesCotations();
        });
    }
    
    /**
     * Actualiser la liste
     */
    @FXML
    private void handleActualiser() {
        loadMesCotations();
        AlertUtils.showInfo("Liste actualisée");
    }
}