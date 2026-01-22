package application.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.print.*;
import javafx.scene.Node;
import javafx.scene.text.Text;
import application.models.*;
import application.services.*;
import application.utils.*;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Contrôleur pour la gestion des courriers - VERSION CORRIGÉE
 * Compatible avec le nouveau modèle Courrier adapté à la vraie DB
 */
public class CourrierController implements Initializable {
    
    // Filtres
    @FXML private ComboBox<String> filtreStatut;
    @FXML private ComboBox<String> filtrePriorite;
    @FXML private DatePicker dateDebut;
    @FXML private DatePicker dateFin;
    @FXML private TextField champRecherche;
    
    // Tableau
    @FXML private TableView<Courrier> tableauCourriers;
    @FXML private TableColumn<Courrier, String> colonneNumero;
    @FXML private TableColumn<Courrier, String> colonneType;
    @FXML private TableColumn<Courrier, String> colonneObjet;
    @FXML private TableColumn<Courrier, String> colonneExpediteur;
    @FXML private TableColumn<Courrier, String> colonneDate;
    @FXML private TableColumn<Courrier, String> colonnePriorite;
    @FXML private TableColumn<Courrier, String> colonneStatut;
    
    // Détails
    @FXML private VBox panneauDetails;
    @FXML private Label labelNumero;
    @FXML private Label labelType;
    @FXML private Label labelObjet;
    @FXML private Label labelExpediteur;
    @FXML private Label labelDate;
    @FXML private Label labelPriorite;
    @FXML private Label labelStatut;
    @FXML private Label labelTraitePar;
    @FXML private Label labelDateDebut;
    @FXML private Label labelEcheance;
    @FXML private TextArea textAreaNotes;
    @FXML private VBox listePiecesJointes;
    
    // Boutons d'action
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private Button btnMarquerTraite;
    @FXML private Button btnCoter;
    @FXML private Button btnCoterSelection;
    @FXML private Button btnArchiver;
    @FXML private Button btnTransferer;
    @FXML private Button btnImprimer;
    @FXML private Label nombreCourriers;
    
    private User currentUser;
    private CourrierService courrierService;
    private CotationService cotationService;
    private ObservableList<Courrier> courriers;
    private Courrier selectedCourrier;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("CourrierController.initialize() appelé");
        
        try {
            currentUser = SessionManager.getInstance().getCurrentUser();
            courrierService = CourrierService.getInstance();
            cotationService = CotationService.getInstance();
            courriers = FXCollections.observableArrayList();
            
            if (currentUser == null) {
                System.err.println("ERREUR: Aucun utilisateur en session");
                return;
            }
            
            // Activer la sélection multiple
            tableauCourriers.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            
            setupTableColumns();
            setupFilters();
            setupButtons();
            loadCourriers();
            
        } catch (Exception e) {
            System.err.println("Erreur dans CourrierController.initialize(): " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupTableColumns() {
        // CORRECTION: Utiliser codeCourrier au lieu de numeroCourrier
        colonneNumero.setCellValueFactory(new PropertyValueFactory<>("codeCourrier"));
        
        // CORRECTION: Utiliser les nouvelles méthodes du modèle
        colonneType.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getTypeCourrierIcone() + " " +
                cellData.getValue().getTypeCourrierLibelle()
            )
        );
        
        colonneObjet.setCellValueFactory(new PropertyValueFactory<>("objet"));
        colonneExpediteur.setCellValueFactory(new PropertyValueFactory<>("expediteur"));
        
        // CORRECTION: dateReception n'existe plus, on utilise dateCourrier
        colonneDate.setCellValueFactory(cellData -> {
            if (cellData.getValue().getDateCourrier() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                    cellData.getValue().getDateCourrierFormatee()
                );
            }
            // Fallback sur dateCreation
            else if (cellData.getValue().getDateCreation() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                return new javafx.beans.property.SimpleStringProperty(
                    cellData.getValue().getDateCreation().format(formatter)
                );
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });
        
        colonnePriorite.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getPrioriteIcone() + " " + 
                cellData.getValue().getPrioriteLibelle()
            )
        );
        
        colonneStatut.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getStatutIcone() + " " + 
                cellData.getValue().getStatutLibelle()
            )
        );
        
        // Listener pour la sélection
        tableauCourriers.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    showCourrierDetails(newSelection);
                }
            }
        );
    }
    
    private void setupFilters() {
        // CORRECTION: Utiliser les valeurs de la vraie DB
        filtreStatut.setItems(FXCollections.observableArrayList(
            "Tous", "nouveau", "en_cours", "traite", "archive"
        ));
        filtreStatut.setValue("Tous");
        
        filtrePriorite.setItems(FXCollections.observableArrayList(
            "Toutes", "TRES_URGENTE", "URGENTE", "NORMALE"
        ));
        filtrePriorite.setValue("Toutes");
        
        // Listeners pour filtrage automatique
        filtreStatut.setOnAction(e -> applyFilters());
        filtrePriorite.setOnAction(e -> applyFilters());
    }
    
    private void setupButtons() {
        if (btnMarquerTraite != null) btnMarquerTraite.setOnAction(e -> handleMarquerTraite());
        if (btnCoter != null) btnCoter.setOnAction(e -> handleCoter());
        if (btnCoterSelection != null) btnCoterSelection.setOnAction(e -> handleCoterSelection());
        if (btnImprimer != null) btnImprimer.setOnAction(e -> handleImprimer());
        if (btnArchiver != null) btnArchiver.setOnAction(e -> handleArchiver());
    }
    
    private void loadCourriers() {
        try {
            List<Courrier> list = courrierService.getAllCourriers();
            courriers.clear();
            courriers.addAll(list);
            tableauCourriers.setItems(courriers);
            
            if (nombreCourriers != null) {
                nombreCourriers.setText("(" + courriers.size() + " courriers)");
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des courriers: " + e.getMessage());
            AlertUtils.showError("Erreur lors du chargement des courriers");
        }
    }
    
    @FXML
    private void handleRecherche() {
        applyFilters();
    }
    
    @FXML
    private void handleActualiser() {
        System.out.println("Action: Actualiser");
        loadCourriers();
        AlertUtils.showInfo("Liste des courriers actualisée");
    }
    
    private void applyFilters() {
        try {
            String statutFilter = filtreStatut.getValue();
            String prioriteFilter = filtrePriorite.getValue();
            String searchText = champRecherche != null ? champRecherche.getText().toLowerCase() : "";
            LocalDate debut = dateDebut != null ? dateDebut.getValue() : null;
            LocalDate fin = dateFin != null ? dateFin.getValue() : null;
            
            // CORRECTION: Utiliser le service qui utilise la vraie DB
            List<Courrier> allCourriers = courrierService.searchCourriers(
                searchText.isEmpty() ? null : searchText,
                statutFilter.equals("Tous") ? null : statutFilter,
                null, // type courrier
                prioriteFilter.equals("Toutes") ? null : prioriteFilter,
                debut,
                fin
            );
            
            courriers.clear();
            courriers.addAll(allCourriers);
            
            if (nombreCourriers != null) {
                nombreCourriers.setText("(" + courriers.size() + " courriers)");
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'application des filtres: " + e.getMessage());
        }
    }
    
    private void showCourrierDetails(Courrier courrier) {
        selectedCourrier = courrier;
        
        // CORRECTION: Utiliser codeCourrier
        if (labelNumero != null) labelNumero.setText(courrier.getCodeCourrier());
        
        if (labelType != null) {
            labelType.setText(
                courrier.getTypeCourrierIcone() + " " + 
                courrier.getTypeCourrierLibelle()
            );
        }
        
        if (labelObjet != null) labelObjet.setText(courrier.getObjet());
        if (labelExpediteur != null) labelExpediteur.setText(courrier.getExpediteur());
        
        // CORRECTION: Utiliser dateCourrier ou dateCreation
        if (labelDate != null) {
            if (courrier.getDateCourrier() != null) {
                labelDate.setText(courrier.getDateCourrierFormatee());
            } else if (courrier.getDateCreation() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                labelDate.setText(courrier.getDateCreation().format(formatter));
            }
        }
        
        if (labelPriorite != null) {
            labelPriorite.setText(
                courrier.getPrioriteIcone() + " " + 
                courrier.getPrioriteLibelle()
            );
        }
        
        if (labelStatut != null) {
            labelStatut.setText(
                courrier.getStatutIcone() + " " + 
                courrier.getStatutLibelle()
            );
        }
        
        // CORRECTION: Utiliser observations au lieu de notes
        if (textAreaNotes != null) {
            textAreaNotes.setText(
                courrier.getObservations() != null ? courrier.getObservations() : ""
            );
        }
    }
    
    @FXML
    private void handleMarquerTraite() {
        if (selectedCourrier == null) {
            AlertUtils.showWarning("Veuillez sélectionner un courrier");
            return;
        }
        
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Marquer comme traité");
        dialog.setHeaderText("Marquer le courrier comme traité");
        dialog.setContentText("Commentaire final:");
        
        Optional<String> result = dialog.showAndWait();
        
        result.ifPresent(commentaire -> {
            // CORRECTION: Utiliser les valeurs String de la vraie DB
            selectedCourrier.setStatut("traite");
            
            // Mettre à jour dans la base
            if (courrierService.updateCourrier(selectedCourrier)) {
                AlertUtils.showInfo("Courrier marqué comme traité");
                loadCourriers();
            } else {
                AlertUtils.showError("Erreur lors de la mise à jour");
            }
        });
    }
    
    @FXML
    private void handleCoter() {
        if (selectedCourrier == null) {
            AlertUtils.showWarning("Veuillez sélectionner un courrier");
            return;
        }
        
        CoterCourrierDialog dialog = new CoterCourrierDialog(selectedCourrier);
        Optional<CotationInfo> result = dialog.showAndWait();
        
        result.ifPresent(cotation -> {
            // Utiliser le nouveau service de cotation
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
                loadCourriers();
            } else {
                AlertUtils.showError("Erreur lors de la cotation");
            }
        });
    }
    
    @FXML
    private void handleCoterSelection() {
        ObservableList<Courrier> selection = 
            tableauCourriers.getSelectionModel().getSelectedItems();
        
        if (selection.isEmpty()) {
            AlertUtils.showWarning("Veuillez sélectionner au moins un courrier");
            return;
        }
        
        // Dialogue pour les paramètres de cotation batch
        CoterCourrierDialog dialog = new CoterCourrierDialog(selection.get(0));
        Optional<CotationInfo> result = dialog.showAndWait();
        
        result.ifPresent(cotation -> {
            List<Courrier> courriers = new ArrayList<>(selection);
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
            
            loadCourriers();
        });
    }
    
    @FXML
    private void handleArchiver() {
        if (selectedCourrier == null) {
            AlertUtils.showWarning("Veuillez sélectionner un courrier");
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
                loadCourriers();
            } else {
                AlertUtils.showError("Erreur lors de l'archivage");
            }
        }
    }
      
    @FXML
    private void handleImprimer() {
        if (selectedCourrier == null) {
            AlertUtils.showWarning("Veuillez sélectionner un courrier");
            return;
        }
        
        try {
            // Créer le contenu à imprimer
            VBox printContent = createPrintContent(selectedCourrier);
            
            // Configurer l'impression
            PrinterJob printerJob = PrinterJob.createPrinterJob();
            
            if (printerJob != null && printerJob.showPrintDialog(tableauCourriers.getScene().getWindow())) {
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
     * Crée le contenu à imprimer pour un courrier
     */
    private VBox createPrintContent(Courrier courrier) {
        VBox content = new VBox(15);
        content.setPadding(new javafx.geometry.Insets(20));
        content.setStyle("-fx-background-color: white;");
        
        // En-tête
        Text titre = new Text("FICHE DE COURRIER");
        titre.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        content.getChildren().add(titre);
        
        // Informations - CORRECTION: Utiliser les bonnes méthodes
        content.getChildren().add(new Text("N° Courrier: " + courrier.getCodeCourrier()));
        content.getChildren().add(new Text("Type: " + courrier.getTypeCourrierLibelle()));
        content.getChildren().add(new Text("Objet: " + courrier.getObjet()));
        content.getChildren().add(new Text("Expéditeur: " + courrier.getExpediteur()));
        content.getChildren().add(new Text("Date: " + courrier.getDateCourrierFormatee()));
        content.getChildren().add(new Text("Priorité: " + courrier.getPrioriteLibelle()));
        content.getChildren().add(new Text("Statut: " + courrier.getStatutLibelle()));
        
        if (courrier.getObservations() != null && !courrier.getObservations().isEmpty()) {
            content.getChildren().add(new Text("\nObservations:\n" + courrier.getObservations()));
        }
        
        return content;
    }
}