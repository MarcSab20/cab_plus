package application.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.print.PrinterJob;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;

import application.models.Courrier;
import application.models.CotationCourrier;
import application.models.User;
import application.services.CourrierService;
import application.services.CotationService;
import application.utils.SessionManager;
import application.utils.AlertUtils;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Contrôleur AMÉLIORÉ pour la messagerie des courriers
 * Fusionne les fonctionnalités de CourrierController et MessagerieCourrierController
 */
public class MessagerieCourrierController implements Initializable {
    
    // ============================================================================
    // COMPOSANTS FXML - FILTRES
    // ============================================================================
    
    @FXML private ComboBox<String> filtreStatutCotation;
    @FXML private ComboBox<String> filtrePriorite;
    @FXML private ComboBox<String> filtreTypeCourrier;
    @FXML private CheckBox filtreEnRetard;
    @FXML private CheckBox filtreNonLus;
    @FXML private DatePicker dateDebut;
    @FXML private DatePicker dateFin;
    @FXML private TextField champRecherche;
    
    // ============================================================================
    // COMPOSANTS FXML - TABLEAU
    // ============================================================================
    
    @FXML private TableView<CotationCourrier> tableauCotations;
    @FXML private TableColumn<CotationCourrier, String> colonneCode;
    @FXML private TableColumn<CotationCourrier, String> colonneObjet;
    @FXML private TableColumn<CotationCourrier, String> colonneType;
    @FXML private TableColumn<CotationCourrier, String> colonneExpediteur;
    @FXML private TableColumn<CotationCourrier, String> colonnePriorite;
    @FXML private TableColumn<CotationCourrier, String> colonneStatut;
    @FXML private TableColumn<CotationCourrier, String> colonneEcheance;
    @FXML private TableColumn<CotationCourrier, String> colonneDelai;
    @FXML private TableColumn<CotationCourrier, String> colonneCotePar;
    
    // ============================================================================
    // COMPOSANTS FXML - DÉTAILS
    // ============================================================================
    
    @FXML private VBox panneauDetails;
    @FXML private Label labelCodeCourrier;
    @FXML private Label labelTypeCourrier;
    @FXML private Label labelObjetCourrier;
    @FXML private Label labelExpediteur;
    @FXML private Label labelDestinataire;
    @FXML private Label labelDateCourrier;
    @FXML private Label labelPrioriteCourrier;
    @FXML private Label labelStatutCourrier;
    @FXML private Label labelStatutCotation;
    @FXML private Label labelPriorite;
    @FXML private Label labelEcheance;
    @FXML private Label labelDelai;
    @FXML private Label labelCotePar;
    @FXML private Label labelDateCotation;
    @FXML private TextArea textAreaInstructions;
    @FXML private TextArea textAreaObservations;
    @FXML private TextArea textAreaCommentaireTraitement;
    
    // ============================================================================
    // COMPOSANTS FXML - ACTIONS
    // ============================================================================
    
    @FXML private Button btnPrendreEnCharge;
    @FXML private Button btnMarquerTraite;
    @FXML private Button btnMarquerTraiteRapide;
    @FXML private Button btnCoter;
    @FXML private Button btnCoterRapide;
    @FXML private Button btnCoterSelection;
    @FXML private Button btnTraiterSelection;
    @FXML private Button btnImprimer;
    @FXML private Button btnArchiver;
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
    private Courrier selectedCourrier;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("MessagerieCourrierController.initialize() - Début (VERSION AMÉLIORÉE)");
        
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
            loadMesCourriers();
            
            System.out.println("✓ MessagerieCourrierController initialisé avec succès");
            
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
        // Code courrier
        colonneCode.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getCourrierCode()));
        
        // Objet
        colonneObjet.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getCourrierObjet()));
        
        // Type courrier - CORRECTION: Utiliser les méthodes de CotationCourrier
        colonneType.setCellValueFactory(cellData -> {
            CotationCourrier cotation = cellData.getValue();
            if (cotation.getTypeCourrier() != null) {
                // Utiliser les méthodes déjà présentes dans CotationCourrier
                return new SimpleStringProperty(
                    cotation.getTypeCourrierIcone() + " " + cotation.getTypeCourrierLibelle()
                );
            }
            return new SimpleStringProperty("📧 Courrier");
        });
        
        // Expéditeur - CORRECTION: Utiliser directement expediteurCourrier
        colonneExpediteur.setCellValueFactory(cellData -> {
            String expediteur = cellData.getValue().getExpediteurCourrier();
            return new SimpleStringProperty(expediteur != null ? expediteur : "--");
        });
        
        // Priorité cotation
        colonnePriorite.setCellValueFactory(cellData -> 
            new SimpleStringProperty(
                cellData.getValue().getPrioriteIcone() + " " + 
                cellData.getValue().getPrioriteLibelle()
            ));
        
        // Statut cotation
        colonneStatut.setCellValueFactory(cellData -> 
            new SimpleStringProperty(
                cellData.getValue().getStatutIcone() + " " + 
                cellData.getValue().getStatutLibelle()
            ));
        
        // Échéance
        colonneEcheance.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDateEcheanceFormatee()));
        
        // Délai
        colonneDelai.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDelaiDescription()));
        
        // Coté par
        colonneCotePar.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getCoteurNom()));
        
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
                } else if (!item.getStatut().equalsIgnoreCase("traite")) {
                    setStyle("");
                } else {
                    setStyle("-fx-background-color: #d4edda;");
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
        if (filtreStatutCotation != null) {
            filtreStatutCotation.setValue("Tous");
            filtreStatutCotation.setOnAction(e -> applyFilters());
        }
        
        // Filtre priorité
        if (filtrePriorite != null) {
            filtrePriorite.setValue("Toutes");
            filtrePriorite.setOnAction(e -> applyFilters());
        }
        
        // Filtre type courrier (NOUVEAU)
        if (filtreTypeCourrier != null) {
            filtreTypeCourrier.setValue("Tous");
            filtreTypeCourrier.setOnAction(e -> applyFilters());
        }
        
        // Filtres checkbox
        if (filtreEnRetard != null) filtreEnRetard.setOnAction(e -> applyFilters());
        if (filtreNonLus != null) filtreNonLus.setOnAction(e -> applyFilters());
        
        // Dates
        if (dateDebut != null) dateDebut.setOnAction(e -> applyFilters());
        if (dateFin != null) dateFin.setOnAction(e -> applyFilters());
    }
    
    /**
     * Configure les boutons
     */
    private void setupButtons() {
        if (btnPrendreEnCharge != null) btnPrendreEnCharge.setOnAction(e -> handlePrendreEnCharge());
        if (btnMarquerTraite != null) btnMarquerTraite.setOnAction(e -> handleMarquerTraite());
        if (btnMarquerTraiteRapide != null) btnMarquerTraiteRapide.setOnAction(e -> handleMarquerTraite());
        if (btnCoter != null) btnCoter.setOnAction(e -> handleCoter());
        if (btnCoterRapide != null) btnCoterRapide.setOnAction(e -> handleCoter());
        if (btnCoterSelection != null) btnCoterSelection.setOnAction(e -> handleCoterSelection());
        if (btnTraiterSelection != null) btnTraiterSelection.setOnAction(e -> handleTraiterSelection());
        if (btnImprimer != null) btnImprimer.setOnAction(e -> handleImprimer());
        if (btnArchiver != null) btnArchiver.setOnAction(e -> handleArchiver());
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
                if (btnCoterSelection != null) {
                    btnCoterSelection.setDisable(nombreSelection == 0);
                }
            }
        );
    }
    
    // ============================================================================
    // CHARGEMENT DES DONNÉES
    // ============================================================================
    
    /**
     * Charge les courriers assignés à l'utilisateur
     * LOGIQUE AMÉLIORÉE: Responsables courrier + cotations
     */
    private void loadMesCourriers() {
        try {
            // NOUVELLE LOGIQUE: Combiner cotations + courriers nouveaux si responsable
            List<CotationCourrier> list = cotationService.getMesCourriersEtCotations(currentUser.getId());
            
            cotations.clear();
            cotations.addAll(list);
            tableauCotations.setItems(cotations);
            
            // Mise à jour des statistiques
            updateStatistiques();
            
            System.out.println("✓ " + cotations.size() + " courriers chargés");
            
        } catch (Exception e) {
            System.err.println("Erreur chargement courriers: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur lors du chargement des courriers");
        }
    }
    
    /**
     * Met à jour les statistiques affichées
     */
    private void updateStatistiques() {
        if (labelNombreCotations != null) {
            labelNombreCotations.setText("(" + cotations.size() + " courriers)");
        }
        
        if (labelNombreEnRetard != null) {
            long nombreEnRetard = cotations.stream()
                .filter(CotationCourrier::isEnRetard)
                .count();
            labelNombreEnRetard.setText(nombreEnRetard + " en retard");
            
            if (nombreEnRetard > 0) {
                labelNombreEnRetard.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            } else {
                labelNombreEnRetard.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
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
            String statutFilter = filtreStatutCotation != null ? filtreStatutCotation.getValue() : "Tous";
            String prioriteFilter = filtrePriorite != null ? filtrePriorite.getValue() : "Toutes";
            String typeFilter = filtreTypeCourrier != null ? filtreTypeCourrier.getValue() : "Tous";
            boolean filtrerRetard = filtreEnRetard != null && filtreEnRetard.isSelected();
            String searchText = champRecherche != null ? champRecherche.getText().toLowerCase() : "";
            LocalDate debut = dateDebut != null ? dateDebut.getValue() : null;
            LocalDate fin = dateFin != null ? dateFin.getValue() : null;
            
            List<CotationCourrier> allCotations = cotationService.getMesCourriersEtCotations(currentUser.getId());
            
            List<CotationCourrier> filtered = allCotations.stream()
                .filter(c -> {
                    // Filtre statut cotation
                    if (!statutFilter.equals("Tous")) {
                        if (!c.getStatutLibelle().equals(statutFilter)) {
                            return false;
                        }
                    }
                    
                    // Filtre priorité
                    if (!prioriteFilter.equals("Toutes")) {
                        if (!c.getPrioriteLibelle().equals(prioriteFilter)) {
                            return false;
                        }
                    }
                    
                    // Filtre type courrier - CORRECTION: Utiliser getTypeCourrier()
                    if (!typeFilter.equals("Tous")) {
                        if (c.getTypeCourrier() == null || !c.getTypeCourrier().equalsIgnoreCase(typeFilter)) {
                            return false;
                        }
                    }
                    
                    // Filtre en retard
                    if (filtrerRetard && !c.isEnRetard()) {
                        return false;
                    }
                    
                    // Filtre dates
                    if (debut != null || fin != null) {
                        Courrier courrier = courrierService.getCourrierById(c.getCourrierId());
                        if (courrier != null && courrier.getDateCourrier() != null) {
                            if (debut != null && courrier.getDateCourrier().isBefore(debut)) {
                                return false;
                            }
                            if (fin != null && courrier.getDateCourrier().isAfter(fin)) {
                                return false;
                            }
                        }
                    }
                    
                    // Recherche textuelle
                    if (!searchText.isEmpty()) {
                        boolean textMatch = 
                            (c.getCourrierCode() != null && c.getCourrierCode().toLowerCase().contains(searchText)) ||
                            (c.getCourrierObjet() != null && c.getCourrierObjet().toLowerCase().contains(searchText)) ||
                            (c.getCoteurNom() != null && c.getCoteurNom().toLowerCase().contains(searchText)) ||
                            (c.getExpediteurCourrier() != null && c.getExpediteurCourrier().toLowerCase().contains(searchText));
                        
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
            e.printStackTrace();
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
        
        // Récupérer le courrier complet
        selectedCourrier = courrierService.getCourrierById(cotation.getCourrierId());
        
        // Informations du courrier
        if (labelCodeCourrier != null) labelCodeCourrier.setText(cotation.getCourrierCode());
        
        // Type - CORRECTION: Utiliser les méthodes de CotationCourrier
        if (labelTypeCourrier != null) {
            if (cotation.getTypeCourrier() != null) {
                labelTypeCourrier.setText(
                    cotation.getTypeCourrierIcone() + " " + cotation.getTypeCourrierLibelle()
                );
            } else {
                labelTypeCourrier.setText("📧 Courrier");
            }
        }
        
        if (labelObjetCourrier != null) labelObjetCourrier.setText(cotation.getCourrierObjet());
        
        // Expéditeur - CORRECTION: Utiliser expediteurCourrier de CotationCourrier
        if (labelExpediteur != null) {
            String expediteur = cotation.getExpediteurCourrier();
            labelExpediteur.setText(expediteur != null ? expediteur : "--");
        }
        
        // Reste du code comme avant...
        if (selectedCourrier != null) {
            if (labelDestinataire != null) labelDestinataire.setText(selectedCourrier.getDestinataire());
            
            if (labelDateCourrier != null) {
                labelDateCourrier.setText(selectedCourrier.getDateCourrierFormatee());
            }
            
            if (labelPrioriteCourrier != null) {
                labelPrioriteCourrier.setText(
                    selectedCourrier.getPrioriteIcone() + " " + selectedCourrier.getPrioriteLibelle()
                );
                labelPrioriteCourrier.setStyle("-fx-text-fill: " + selectedCourrier.getPrioriteCouleur() + "; -fx-font-weight: bold;");
            }
            
            if (labelStatutCourrier != null) {
                labelStatutCourrier.setText(
                    selectedCourrier.getStatutIcone() + " " + selectedCourrier.getStatutLibelle()
                );
                labelStatutCourrier.setStyle("-fx-text-fill: " + selectedCourrier.getStatutCouleur() + "; -fx-font-weight: bold;");
            }
            
            if (textAreaObservations != null) {
                textAreaObservations.setText(
                    selectedCourrier.getObservations() != null ? selectedCourrier.getObservations() : ""
                );
            }
        }
        
        // Informations de la cotation
        if (labelStatutCotation != null) {
            labelStatutCotation.setText(cotation.getStatutIcone() + " " + cotation.getStatutLibelle());
            labelStatutCotation.setStyle("-fx-text-fill: " + cotation.getStatutCouleur() + "; -fx-font-weight: bold;");
        }
        
        if (labelPriorite != null) {
            labelPriorite.setText(cotation.getPrioriteIcone() + " " + cotation.getPrioriteLibelle());
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
        
        if (labelDateCotation != null) {
            labelDateCotation.setText(cotation.getDateCotationFormatee());
        }
        
        if (textAreaInstructions != null) {
            textAreaInstructions.setText(cotation.getCommentaire() != null ? cotation.getCommentaire() : "");
        }
        
        if (textAreaCommentaireTraitement != null) {
            textAreaCommentaireTraitement.setText(
                cotation.getCommentaireTraitement() != null ? cotation.getCommentaireTraitement() : ""
            );
        }
        
        // Activer/désactiver les boutons
        updateButtonStates();
    }
    
    /**
     * Met à jour l'état des boutons
     */
    private void updateButtonStates() {
        if (selectedCotation == null) {
            if (btnPrendreEnCharge != null) btnPrendreEnCharge.setDisable(true);
            if (btnMarquerTraite != null) btnMarquerTraite.setDisable(true);
            if (btnMarquerTraiteRapide != null) btnMarquerTraiteRapide.setDisable(true);
            if (btnCoter != null) btnCoter.setDisable(true);
            if (btnCoterRapide != null) btnCoterRapide.setDisable(true);
            if (btnImprimer != null) btnImprimer.setDisable(true);
            if (btnArchiver != null) btnArchiver.setDisable(true);
            return;
        }
        
        if (btnPrendreEnCharge != null) {
            btnPrendreEnCharge.setDisable(!selectedCotation.peutEtrePriseEnCharge());
        }
        
        if (btnMarquerTraite != null) {
            btnMarquerTraite.setDisable(!selectedCotation.peutEtreTraitee());
        }
        
        if (btnMarquerTraiteRapide != null) {
            btnMarquerTraiteRapide.setDisable(!selectedCotation.peutEtreTraitee());
        }
        
        if (btnCoter != null) {
            btnCoter.setDisable(false); // On peut toujours re-coter
        }
        
        if (btnCoterRapide != null) {
            btnCoterRapide.setDisable(false);
        }
        
        if (btnImprimer != null) {
            btnImprimer.setDisable(false);
        }
        
        if (btnArchiver != null) {
            btnArchiver.setDisable(selectedCotation.getStatut().equalsIgnoreCase("traite") == false);
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
            AlertUtils.showWarning("Veuillez sélectionner un courrier");
            return;
        }
        
        boolean success = cotationService.prendreEnCharge(selectedCotation.getId(), currentUser.getId());
        
        if (success) {
            AlertUtils.showInfo("Courrier pris en charge");
            loadMesCourriers();
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
            AlertUtils.showWarning("Veuillez sélectionner un courrier");
            return;
        }
        
        // Dialogue pour le commentaire
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
                AlertUtils.showInfo("Courrier marqué comme traité");
                loadMesCourriers();
            } else {
                AlertUtils.showError("Erreur lors du traitement");
            }
        });
    }
    
    /**
     * Coter un courrier à quelqu'un d'autre (NOUVEAU - fusionné depuis CourrierController)
     */
    @FXML
    private void handleCoter() {
        if (selectedCotation == null || selectedCourrier == null) {
            AlertUtils.showWarning("Veuillez sélectionner un courrier");
            return;
        }
        
        CoterCourrierDialog dialog = new CoterCourrierDialog(selectedCourrier);
        Optional<CotationInfo> result = dialog.showAndWait();
        
        result.ifPresent(cotation -> {
            boolean success = cotationService.coterCourrier(
                selectedCourrier,
                currentUser,
                cotation.getUtilisateur(),
                cotation.getCommentaire(),
                cotation.getPriorite() != null ? cotation.getPriorite() : "NORMALE",
                cotation.getDelaiJours(),
                cotation.isNotifierUtilisateur()
            );
            
            if (success) {
                AlertUtils.showInfo("Courrier coté à " + cotation.getUtilisateur().getNomComplet());
                loadMesCourriers();
            } else {
                AlertUtils.showError("Erreur lors de la cotation");
            }
        });
    }
    
    /**
     * Coter plusieurs courriers (NOUVEAU - fusionné depuis CourrierController)
     */
    @FXML
    private void handleCoterSelection() {
        ObservableList<CotationCourrier> selection = 
            tableauCotations.getSelectionModel().getSelectedItems();
        
        if (selection.isEmpty()) {
            AlertUtils.showWarning("Veuillez sélectionner au moins un courrier");
            return;
        }
        
        // Récupérer les courriers complets
        List<Courrier> courriers = new ArrayList<>();
        for (CotationCourrier cotation : selection) {
            Courrier courrier = courrierService.getCourrierById(cotation.getCourrierId());
            if (courrier != null) {
                courriers.add(courrier);
            }
        }
        
        if (courriers.isEmpty()) {
            AlertUtils.showError("Impossible de récupérer les courriers");
            return;
        }
        
        // Dialogue de cotation
        CoterCourrierDialog dialog = new CoterCourrierDialog(courriers.get(0));
        Optional<CotationInfo> result = dialog.showAndWait();
        
        result.ifPresent(cotation -> {
            CotationService.BatchOperationResult batchResult = 
                cotationService.coterCourriersEnBatch(
                    courriers, 
                    currentUser, 
                    cotation.getUtilisateur(),
                    cotation.getCommentaire(),
                    cotation.getPriorite() != null ? cotation.getPriorite() : "NORMALE",
                    cotation.getDelaiJours(),
                    cotation.isNotifierUtilisateur()
                );
            
            // Afficher le résultat
            String message = String.format(
                "Cotation en batch terminée:\n\n" +
                "Total: %d courriers\n" +
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
            
            loadMesCourriers();
        });
    }
    
    /**
     * Traiter plusieurs cotations en batch
     */
    @FXML
    private void handleTraiterSelection() {
        ObservableList<CotationCourrier> selection = 
            tableauCotations.getSelectionModel().getSelectedItems();
        
        if (selection.isEmpty()) {
            AlertUtils.showWarning("Veuillez sélectionner au moins un courrier");
            return;
        }
        
        // Vérifier que toutes peuvent être traitées
        List<CotationCourrier> nonTraitables = selection.stream()
            .filter(c -> !c.peutEtreTraitee())
            .collect(Collectors.toList());
        
        if (!nonTraitables.isEmpty()) {
            AlertUtils.showWarning(
                nonTraitables.size() + " courrier(s) ne peuvent pas être traités.\n" +
                "Seuls les courriers 'en attente' ou 'en cours' peuvent être traités."
            );
            return;
        }
        
        // Dialogue pour le commentaire
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Traitement en batch");
        dialog.setHeaderText("Traiter " + selection.size() + " courriers");
        dialog.setContentText("Commentaire de traitement:");
        
        Optional<String> result = dialog.showAndWait();
        
        result.ifPresent(commentaire -> {
            List<CotationCourrier> list = new ArrayList<>(selection);
            CotationService.BatchOperationResult batchResult = 
                cotationService.traiterCotationsEnBatch(list, currentUser, commentaire);
            
            // Afficher le résultat
            String message = String.format(
                "Traitement terminé:\n\n" +
                "Total: %d courriers\n" +
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
            
            loadMesCourriers();
        });
    }
    
    /**
     * Imprimer un courrier (NOUVEAU - fusionné depuis CourrierController)
     */
    @FXML
    private void handleImprimer() {
        if (selectedCotation == null || selectedCourrier == null) {
            AlertUtils.showWarning("Veuillez sélectionner un courrier");
            return;
        }
        
        try {
            // Créer le contenu à imprimer
            VBox printContent = createPrintContent(selectedCourrier, selectedCotation);
            
            // Configurer l'impression
            PrinterJob printerJob = PrinterJob.createPrinterJob();
            
            if (printerJob != null && printerJob.showPrintDialog(tableauCotations.getScene().getWindow())) {
                boolean success = printerJob.printPage(printContent);
                
                if (success) {
                    printerJob.endJob();
                    AlertUtils.showInfo("Impression lancée avec succès");
                } else {
                    AlertUtils.showError("Erreur lors de l'impression");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'impression: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur lors de l'impression: " + e.getMessage());
        }
    }
    
    /**
     * Crée le contenu à imprimer
     */
    private VBox createPrintContent(Courrier courrier, CotationCourrier cotation) {
        VBox content = new VBox(15);
        content.setPadding(new javafx.geometry.Insets(20));
        content.setStyle("-fx-background-color: white;");
        
        // En-tête
        Text titre = new Text("FICHE DE COURRIER");
        titre.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        content.getChildren().add(titre);
        
        // Informations courrier
        content.getChildren().add(new Text("═══════════════════════════════════════"));
        content.getChildren().add(new Text("INFORMATIONS DU COURRIER"));
        content.getChildren().add(new Text("═══════════════════════════════════════"));
        content.getChildren().add(new Text("N° Courrier: " + courrier.getCodeCourrier()));
        content.getChildren().add(new Text("Type: " + courrier.getTypeCourrierLibelle()));
        content.getChildren().add(new Text("Objet: " + courrier.getObjet()));
        content.getChildren().add(new Text("Expéditeur: " + courrier.getExpediteur()));
        content.getChildren().add(new Text("Destinataire: " + (courrier.getDestinataire() != null ? courrier.getDestinataire() : "N/A")));
        content.getChildren().add(new Text("Date: " + courrier.getDateCourrierFormatee()));
        content.getChildren().add(new Text("Priorité: " + courrier.getPrioriteLibelle()));
        content.getChildren().add(new Text("Statut: " + courrier.getStatutLibelle()));
        
        // Informations cotation
        content.getChildren().add(new Text("\n═══════════════════════════════════════"));
        content.getChildren().add(new Text("DÉTAILS DE L'ASSIGNATION"));
        content.getChildren().add(new Text("═══════════════════════════════════════"));
        content.getChildren().add(new Text("Coté à: " + currentUser.getNomComplet()));
        content.getChildren().add(new Text("Coté par: " + cotation.getCoteurNom()));
        content.getChildren().add(new Text("Date cotation: " + cotation.getDateCotationFormatee()));
        content.getChildren().add(new Text("Échéance: " + cotation.getDateEcheanceAvecHeureFormatee()));
        content.getChildren().add(new Text("Délai: " + cotation.getDelaiDescription()));
        content.getChildren().add(new Text("Statut: " + cotation.getStatutLibelle()));
        
        if (cotation.getCommentaire() != null && !cotation.getCommentaire().isEmpty()) {
            content.getChildren().add(new Text("\nInstructions:\n" + cotation.getCommentaire()));
        }
        
        if (courrier.getObservations() != null && !courrier.getObservations().isEmpty()) {
            content.getChildren().add(new Text("\nObservations:\n" + courrier.getObservations()));
        }
        
        if (cotation.getCommentaireTraitement() != null && !cotation.getCommentaireTraitement().isEmpty()) {
            content.getChildren().add(new Text("\nCommentaire de traitement:\n" + cotation.getCommentaireTraitement()));
        }
        
        // Pied de page
        content.getChildren().add(new Text("\n═══════════════════════════════════════"));
        content.getChildren().add(new Text("Imprimé le: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        
        return content;
    }
    
    /**
     * Archiver un courrier (NOUVEAU)
     */
    @FXML
    private void handleArchiver() {
        if (selectedCotation == null || selectedCourrier == null) {
            AlertUtils.showWarning("Veuillez sélectionner un courrier");
            return;
        }
        
        // Vérifier que le courrier est traité
        if (!selectedCotation.getStatut().equalsIgnoreCase("traite")) {
            AlertUtils.showWarning("Seuls les courriers traités peuvent être archivés");
            return;
        }
        
        boolean confirm = AlertUtils.showConfirmation(
            "Archiver le courrier",
            "Êtes-vous sûr de vouloir archiver ce courrier ?\n" +
            "Code: " + selectedCourrier.getCodeCourrier()
        );
        
        if (confirm) {
            if (courrierService.archiverCourrier(selectedCourrier.getId())) {
                AlertUtils.showInfo("Courrier archivé avec succès");
                loadMesCourriers();
            } else {
                AlertUtils.showError("Erreur lors de l'archivage");
            }
        }
    }
    
    /**
     * Actualiser la liste
     */
    @FXML
    private void handleActualiser() {
        loadMesCourriers();
        AlertUtils.showInfo("Liste actualisée");
    }
}