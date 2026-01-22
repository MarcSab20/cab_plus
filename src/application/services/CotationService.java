package application.services;

import application.models.Courrier;
import application.models.CotationCourrier;
import application.models.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service de gestion des cotations de courriers
 * Permet les opérations individuelles et en batch
 */
public class CotationService {
    private static CotationService instance;
    
    private CotationService() {}
    
    public static synchronized CotationService getInstance() {
        if (instance == null) {
            instance = new CotationService();
        }
        return instance;
    }
    
    // ============================================================================
    // COTATION INDIVIDUELLE
    // ============================================================================
    
    /**
     * Cote un courrier à un utilisateur
     */
    public boolean coterCourrier(Courrier courrier, User cotePar, User coteA, 
                                 String commentaire, String priorite, int delaiJours, 
                                 boolean notifier) {
        
        String sql = """
            INSERT INTO cotations_courriers 
            (courrier_id, cote_par, cote_a, service_destination, commentaire, 
             priorite, date_echeance, delai_jours, statut, notifier_utilisateur)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'en_attente', ?)
        """;
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            LocalDateTime dateEcheance = LocalDateTime.now().plusDays(delaiJours);
            
            stmt.setInt(1, courrier.getId());
            stmt.setInt(2, cotePar.getId());
            stmt.setInt(3, coteA.getId());
            stmt.setString(4, coteA.getServiceCode());
            stmt.setString(5, commentaire);
            stmt.setString(6, priorite);
            stmt.setTimestamp(7, Timestamp.valueOf(dateEcheance));
            stmt.setInt(8, delaiJours);
            stmt.setBoolean(9, notifier);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                // Mettre à jour le statut du courrier
                updateCourrierStatut(courrier.getId(), "en_cours");
                
                // Enregistrer dans l'historique
                enregistrerHistorique(courrier.getId(), cotePar.getId(), "cotation",
                    "Courrier coté à " + coteA.getNomComplet());
                
                System.out.println("✓ Cotation créée avec succès");
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur lors de la cotation: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    // ============================================================================
    // COTATION EN BATCH
    // ============================================================================
    
    /**
     * Cote plusieurs courriers à la fois
     */
    public BatchOperationResult coterCourriersEnBatch(List<Courrier> courriers, User cotePar, 
                                                       User coteA, String commentaire, 
                                                       String priorite, int delaiJours, 
                                                       boolean notifier) {
        
        BatchOperationResult result = new BatchOperationResult();
        result.setOperationType("cotation_batch");
        result.setTotalCourriers(courriers.size());
        result.setDateDebut(LocalDateTime.now());
        
        int reussis = 0;
        List<String> erreurs = new ArrayList<>();
        
        for (Courrier courrier : courriers) {
            try {
                boolean success = coterCourrier(courrier, cotePar, coteA, commentaire, 
                                              priorite, delaiJours, notifier);
                if (success) {
                    reussis++;
                } else {
                    erreurs.add("Échec cotation courrier " + courrier.getCodeCourrier());
                }
            } catch (Exception e) {
                erreurs.add("Erreur courrier " + courrier.getCodeCourrier() + ": " + e.getMessage());
            }
        }
        
        result.setCourriersTraites(reussis);
        result.setErreurs(erreurs);
        result.setDateFin(LocalDateTime.now());
        result.setStatut(erreurs.isEmpty() ? "termine" : "termine_avec_erreurs");
        
        // Enregistrer l'opération batch
        enregistrerOperationBatch(result, cotePar.getId());
        
        return result;
    }
    
    // ============================================================================
    // TRAITEMENT DE COTATIONS
    // ============================================================================
    
    /**
     * Prendre en charge une cotation
     */
    public boolean prendreEnCharge(int cotationId, int userId) {
        String sql = """
            UPDATE cotations_courriers 
            SET statut = 'en_cours', 
                date_prise_en_charge = NOW()
            WHERE id = ? AND cote_a = ?
        """;
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, cotationId);
            stmt.setInt(2, userId);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("✓ Cotation prise en charge");
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur prise en charge: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Marquer une cotation comme traitée
     */
    public boolean marquerTraitee(int cotationId, int userId, String commentaire) {
        String sql = """
            UPDATE cotations_courriers 
            SET statut = 'traite', 
                date_traitement = NOW(),
                commentaire_traitement = ?
            WHERE id = ? AND cote_a = ?
        """;
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, commentaire);
            stmt.setInt(2, cotationId);
            stmt.setInt(3, userId);
            
            int rowsAffected = stmt.executeUpdate();
            
            if (rowsAffected > 0) {
                // Récupérer le courrier_id pour mettre à jour le courrier
                CotationCourrier cotation = getCotationById(cotationId);
                if (cotation != null) {
                    updateCourrierStatut(cotation.getCourrierId(), "traite");
                }
                
                System.out.println("✓ Cotation traitée");
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur traitement: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Traiter plusieurs cotations en batch
     */
    public BatchOperationResult traiterCotationsEnBatch(List<CotationCourrier> cotations, 
                                                         User user, String commentaire) {
        
        BatchOperationResult result = new BatchOperationResult();
        result.setOperationType("traitement_batch");
        result.setTotalCourriers(cotations.size());
        result.setDateDebut(LocalDateTime.now());
        
        int reussis = 0;
        List<String> erreurs = new ArrayList<>();
        
        for (CotationCourrier cotation : cotations) {
            try {
                boolean success = marquerTraitee(cotation.getId(), user.getId(), commentaire);
                if (success) {
                    reussis++;
                } else {
                    erreurs.add("Échec traitement cotation " + cotation.getId());
                }
            } catch (Exception e) {
                erreurs.add("Erreur cotation " + cotation.getId() + ": " + e.getMessage());
            }
        }
        
        result.setCourriersTraites(reussis);
        result.setErreurs(erreurs);
        result.setDateFin(LocalDateTime.now());
        result.setStatut(erreurs.isEmpty() ? "termine" : "termine_avec_erreurs");
        
        enregistrerOperationBatch(result, user.getId());
        
        return result;
    }
    
    // ============================================================================
    // CONSULTATION DES COTATIONS
    // ============================================================================
    
    /**
     * Récupère toutes les cotations assignées à un utilisateur
     */
    public List<CotationCourrier> getCotationsAssigneesA(int userId) {
        List<CotationCourrier> cotations = new ArrayList<>();
        
        String sql = """
            SELECT 
                cot.*,
                c.code_courrier, c.objet as courrier_objet, c.priorite as priorite_courrier, c.statut as statut_courrier,
                CONCAT(u_coteur.prenom, ' ', u_coteur.nom) as coteur_nom,
                CONCAT(u_assigne.prenom, ' ', u_assigne.nom) as assigne_nom,
                sh.service_name
            FROM cotations_courriers cot
            INNER JOIN courriers c ON cot.courrier_id = c.id
            INNER JOIN users u_coteur ON cot.cote_par = u_coteur.id
            INNER JOIN users u_assigne ON cot.cote_a = u_assigne.id
            LEFT JOIN service_hierarchy sh ON cot.service_destination = sh.service_code
            WHERE cot.cote_a = ?
            ORDER BY 
                CASE WHEN cot.date_echeance < NOW() AND cot.statut != 'traite' THEN 0 ELSE 1 END,
                cot.date_echeance ASC
        """;
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    CotationCourrier cotation = mapResultSetToCotation(rs);
                    cotations.add(cotation);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération cotations: " + e.getMessage());
            e.printStackTrace();
        }
        
        return cotations;
    }
    
    /**
     * Récupère les cotations par statut
     */
    public List<CotationCourrier> getCotationsByStatut(int userId, String statut) {
        return getCotationsAssigneesA(userId).stream()
            .filter(c -> c.getStatut().equalsIgnoreCase(statut))
            .collect(Collectors.toList());
    }
    
    /**
     * Récupère les cotations en retard
     */
    public List<CotationCourrier> getCotationsEnRetard(int userId) {
        return getCotationsAssigneesA(userId).stream()
            .filter(CotationCourrier::isEnRetard)
            .collect(Collectors.toList());
    }
    
    /**
     * Récupère une cotation par ID
     */
    public CotationCourrier getCotationById(int cotationId) {
        String sql = """
            SELECT 
                cot.*,
                c.code_courrier, c.objet as courrier_objet, c.priorite as priorite_courrier, c.statut as statut_courrier,
                CONCAT(u_coteur.prenom, ' ', u_coteur.nom) as coteur_nom,
                CONCAT(u_assigne.prenom, ' ', u_assigne.nom) as assigne_nom,
                sh.service_name
            FROM cotations_courriers cot
            INNER JOIN courriers c ON cot.courrier_id = c.id
            INNER JOIN users u_coteur ON cot.cote_par = u_coteur.id
            INNER JOIN users u_assigne ON cot.cote_a = u_assigne.id
            LEFT JOIN service_hierarchy sh ON cot.service_destination = sh.service_code
            WHERE cot.id = ?
        """;
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, cotationId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCotation(rs);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération cotation: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Récupère les cotations d'un courrier spécifique
     */
    public List<CotationCourrier> getCotationsByCourrier(int courrierId) {
        List<CotationCourrier> cotations = new ArrayList<>();
        
        String sql = """
            SELECT 
                cot.*,
                c.code_courrier, c.objet as courrier_objet, c.priorite as priorite_courrier, c.statut as statut_courrier,
                CONCAT(u_coteur.prenom, ' ', u_coteur.nom) as coteur_nom,
                CONCAT(u_assigne.prenom, ' ', u_assigne.nom) as assigne_nom,
                sh.service_name
            FROM cotations_courriers cot
            INNER JOIN courriers c ON cot.courrier_id = c.id
            INNER JOIN users u_coteur ON cot.cote_par = u_coteur.id
            INNER JOIN users u_assigne ON cot.cote_a = u_assigne.id
            LEFT JOIN service_hierarchy sh ON cot.service_destination = sh.service_code
            WHERE cot.courrier_id = ?
            ORDER BY cot.date_cotation DESC
        """;
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, courrierId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    CotationCourrier cotation = mapResultSetToCotation(rs);
                    cotations.add(cotation);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération cotations du courrier: " + e.getMessage());
        }
        
        return cotations;
    }
    
    // ============================================================================
    // MÉTHODES UTILITAIRES PRIVÉES
    // ============================================================================
    
    /**
     * Mappe un ResultSet vers un objet CotationCourrier
     */
    private CotationCourrier mapResultSetToCotation(ResultSet rs) throws SQLException {
        CotationCourrier cotation = new CotationCourrier();
        
        cotation.setId(rs.getInt("id"));
        cotation.setCourrierId(rs.getInt("courrier_id"));
        cotation.setCotePar(rs.getInt("cote_par"));
        cotation.setCoteA(rs.getInt("cote_a"));
        cotation.setServiceDestination(rs.getString("service_destination"));
        cotation.setCommentaire(rs.getString("commentaire"));
        cotation.setPriorite(rs.getString("priorite"));
        
        Timestamp dateEcheance = rs.getTimestamp("date_echeance");
        if (dateEcheance != null) {
            cotation.setDateEcheance(dateEcheance.toLocalDateTime());
        }
        
        cotation.setDelaiJours(rs.getInt("delai_jours"));
        cotation.setStatut(rs.getString("statut"));
        
        Timestamp dateCotation = rs.getTimestamp("date_cotation");
        if (dateCotation != null) {
            cotation.setDateCotation(dateCotation.toLocalDateTime());
        }
        
        Timestamp datePriseEnCharge = rs.getTimestamp("date_prise_en_charge");
        if (datePriseEnCharge != null) {
            cotation.setDatePriseEnCharge(datePriseEnCharge.toLocalDateTime());
        }
        
        Timestamp dateTraitement = rs.getTimestamp("date_traitement");
        if (dateTraitement != null) {
            cotation.setDateTraitement(dateTraitement.toLocalDateTime());
        }
        
        cotation.setCommentaireTraitement(rs.getString("commentaire_traitement"));
        cotation.setNotifierUtilisateur(rs.getBoolean("notifier_utilisateur"));
        
        // Champs additionnels
        cotation.setCourrierCode(rs.getString("code_courrier"));
        cotation.setCourrierObjet(rs.getString("courrier_objet"));
        cotation.setCoteurNom(rs.getString("coteur_nom"));
        cotation.setAssigneNom(rs.getString("assigne_nom"));
        cotation.setServiceName(rs.getString("service_name"));
        cotation.setPrioriteCourrier(rs.getString("priorite_courrier"));
        cotation.setStatutCourrier(rs.getString("statut_courrier"));
        
        return cotation;
    }
    
    /**
     * Met à jour le statut d'un courrier
     */
    private void updateCourrierStatut(int courrierId, String nouveauStatut) {
        String sql = "UPDATE courriers SET statut = ?, date_modification = NOW() WHERE id = ?";
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, nouveauStatut);
            stmt.setInt(2, courrierId);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Erreur mise à jour statut courrier: " + e.getMessage());
        }
    }
    
    /**
     * Enregistre une opération dans l'historique
     */
    private void enregistrerHistorique(int courrierId, int userId, String action, String description) {
        String sql = """
            INSERT INTO historique_courriers (courrier_id, user_id, action, description)
            VALUES (?, ?, ?, ?)
        """;
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, courrierId);
            stmt.setInt(2, userId);
            stmt.setString(3, action);
            stmt.setString(4, description);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Erreur enregistrement historique: " + e.getMessage());
        }
    }
    
    /**
     * Enregistre une opération batch
     */
    private void enregistrerOperationBatch(BatchOperationResult result, int userId) {
        String sql = """
            INSERT INTO batch_operations_courriers 
            (operation_type, user_id, courriers_total, courriers_traites, statut, date_fin)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, result.getOperationType());
            stmt.setInt(2, userId);
            stmt.setInt(3, result.getTotalCourriers());
            stmt.setInt(4, result.getCourriersTraites());
            stmt.setString(5, result.getStatut());
            stmt.setTimestamp(6, Timestamp.valueOf(result.getDateFin()));
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Erreur enregistrement batch: " + e.getMessage());
        }
    }
    
    // ============================================================================
    // CLASSE INTERNE: BatchOperationResult
    // ============================================================================
    
    /**
     * Résultat d'une opération en batch
     */
    public static class BatchOperationResult {
        private String operationType;
        private int totalCourriers;
        private int courriersTraites;
        private String statut;
        private LocalDateTime dateDebut;
        private LocalDateTime dateFin;
        private List<String> erreurs;
        
        public BatchOperationResult() {
            this.erreurs = new ArrayList<>();
        }
        
        // Getters et setters
        public String getOperationType() { return operationType; }
        public void setOperationType(String operationType) { this.operationType = operationType; }
        
        public int getTotalCourriers() { return totalCourriers; }
        public void setTotalCourriers(int totalCourriers) { this.totalCourriers = totalCourriers; }
        
        public int getCourriersTraites() { return courriersTraites; }
        public void setCourriersTraites(int courriersTraites) { this.courriersTraites = courriersTraites; }
        
        public String getStatut() { return statut; }
        public void setStatut(String statut) { this.statut = statut; }
        
        public LocalDateTime getDateDebut() { return dateDebut; }
        public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }
        
        public LocalDateTime getDateFin() { return dateFin; }
        public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }
        
        public List<String> getErreurs() { return erreurs; }
        public void setErreurs(List<String> erreurs) { this.erreurs = erreurs; }
        
        public boolean hasErrors() { return !erreurs.isEmpty(); }
        public int getCourrierEchecs() { return totalCourriers - courriersTraites; }
        public double getTauxReussite() { 
            return totalCourriers > 0 ? (courriersTraites * 100.0 / totalCourriers) : 0; 
        }
    }
}