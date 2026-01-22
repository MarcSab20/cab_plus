package application.services;

import application.models.Courrier;
import application.models.User;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de gestion des courriers - VERSION ADAPTÉE À LA VRAIE DB
 * Gère les courriers partagés entre les deux applications
 */
public class CourrierService {
    private static CourrierService instance;
    
    private CourrierService() {}
    
    public static synchronized CourrierService getInstance() {
        if (instance == null) {
            instance = new CourrierService();
        }
        return instance;
    }
    
    // ============================================================================
    // RÉCUPÉRATION DES COURRIERS
    // ============================================================================
    
    /**
     * Récupère tous les courriers
     */
    public List<Courrier> getAllCourriers() {
        List<Courrier> courriers = new ArrayList<>();
        
        String sql = """
            SELECT 
                c.*,
                CONCAT(u.prenom, ' ', u.nom) as createur_nom,
                (SELECT COUNT(*) FROM cotations_courriers WHERE courrier_id = c.id) as nb_cotations
            FROM courriers c
            LEFT JOIN users u ON c.cree_par = u.id
            ORDER BY c.date_creation DESC
        """;
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Courrier courrier = mapResultSetToCourrier(rs);
                courriers.add(courrier);
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération courriers: " + e.getMessage());
            e.printStackTrace();
        }
        
        return courriers;
    }
    
    /**
     * Récupère les courriers assignés à un utilisateur (via notifications ou cotations)
     */
    public List<Courrier> getCourriersAssignesA(int userId) {
        List<Courrier> courriers = new ArrayList<>();
        
        String sql = """
            SELECT DISTINCT
                c.*,
                CONCAT(u.prenom, ' ', u.nom) as createur_nom,
                (SELECT COUNT(*) FROM cotations_courriers WHERE courrier_id = c.id) as nb_cotations,
                nc.lu as notification_lue,
                nc.date_notification,
                cot.statut as statut_cotation,
                cot.date_echeance,
                cot.priorite as priorite_cotation
            FROM courriers c
            LEFT JOIN users u ON c.cree_par = u.id
            LEFT JOIN notifications_courrier nc ON c.id = nc.courrier_id AND nc.user_id = ?
            LEFT JOIN cotations_courriers cot ON c.id = cot.courrier_id AND cot.cote_a = ?
            WHERE nc.user_id = ? OR cot.cote_a = ?
            ORDER BY 
                CASE WHEN cot.date_echeance < NOW() AND cot.statut != 'traite' THEN 0 ELSE 1 END,
                c.date_creation DESC
        """;
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            stmt.setInt(4, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Courrier courrier = mapResultSetToCourrier(rs);
                    courriers.add(courrier);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération courriers assignés: " + e.getMessage());
            e.printStackTrace();
        }
        
        return courriers;
    }
    
    /**
     * Récupère les courriers destinés à un utilisateur spécifique (par destinataire)
     */
    public List<Courrier> getCourriersDestinataireNom(String destinataireNom) {
        List<Courrier> courriers = new ArrayList<>();
        
        String sql = """
            SELECT 
                c.*,
                CONCAT(u.prenom, ' ', u.nom) as createur_nom,
                (SELECT COUNT(*) FROM cotations_courriers WHERE courrier_id = c.id) as nb_cotations
            FROM courriers c
            LEFT JOIN users u ON c.cree_par = u.id
            WHERE c.destinataire LIKE ?
            ORDER BY c.date_creation DESC
        """;
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, "%" + destinataireNom + "%");
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Courrier courrier = mapResultSetToCourrier(rs);
                    courriers.add(courrier);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération courriers par destinataire: " + e.getMessage());
        }
        
        return courriers;
    }
    
    /**
     * Récupère un courrier par son ID
     */
    public Courrier getCourrierById(int id) {
        String sql = """
            SELECT 
                c.*,
                CONCAT(u.prenom, ' ', u.nom) as createur_nom,
                (SELECT COUNT(*) FROM cotations_courriers WHERE courrier_id = c.id) as nb_cotations
            FROM courriers c
            LEFT JOIN users u ON c.cree_par = u.id
            WHERE c.id = ?
        """;
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCourrier(rs);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération courrier: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Récupère un courrier par son code
     */
    public Courrier getCourrierByCode(String codeCourrier) {
        String sql = """
            SELECT 
                c.*,
                CONCAT(u.prenom, ' ', u.nom) as createur_nom,
                (SELECT COUNT(*) FROM cotations_courriers WHERE courrier_id = c.id) as nb_cotations
            FROM courriers c
            LEFT JOIN users u ON c.cree_par = u.id
            WHERE c.code_courrier = ?
        """;
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, codeCourrier);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCourrier(rs);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération courrier par code: " + e.getMessage());
        }
        
        return null;
    }
    
    // ============================================================================
    // RECHERCHE ET FILTRAGE
    // ============================================================================
    
    /**
     * Recherche des courriers selon des critères
     */
    public List<Courrier> searchCourriers(String keyword, String statut, String typeCourrier, 
                                          String priorite, LocalDate dateDebut, LocalDate dateFin) {
        List<Courrier> courriers = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT 
                c.*,
                CONCAT(u.prenom, ' ', u.nom) as createur_nom,
                (SELECT COUNT(*) FROM cotations_courriers WHERE courrier_id = c.id) as nb_cotations
            FROM courriers c
            LEFT JOIN users u ON c.cree_par = u.id
            WHERE 1=1
        """);
        
        List<Object> params = new ArrayList<>();
        
        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND (c.code_courrier LIKE ? OR c.objet LIKE ? OR c.expediteur LIKE ? OR c.destinataire LIKE ?)");
            String searchPattern = "%" + keyword + "%";
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }
        
        if (statut != null && !statut.isEmpty() && !statut.equals("Tous")) {
            sql.append(" AND c.statut = ?");
            params.add(statut.toLowerCase());
        }
        
        if (typeCourrier != null && !typeCourrier.isEmpty() && !typeCourrier.equals("Tous")) {
            sql.append(" AND c.type_courrier = ?");
            params.add(typeCourrier.toUpperCase());
        }
        
        if (priorite != null && !priorite.isEmpty() && !priorite.equals("Toutes")) {
            sql.append(" AND c.priorite = ?");
            params.add(priorite.toUpperCase());
        }
        
        if (dateDebut != null) {
            sql.append(" AND c.date_courrier >= ?");
            params.add(Date.valueOf(dateDebut));
        }
        
        if (dateFin != null) {
            sql.append(" AND c.date_courrier <= ?");
            params.add(Date.valueOf(dateFin));
        }
        
        sql.append(" ORDER BY c.date_creation DESC");
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Courrier courrier = mapResultSetToCourrier(rs);
                    courriers.add(courrier);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur recherche courriers: " + e.getMessage());
            e.printStackTrace();
        }
        
        return courriers;
    }
    
    // ============================================================================
    // MODIFICATION DES COURRIERS
    // ============================================================================
    
    /**
     * Met à jour un courrier existant
     */
    public boolean updateCourrier(Courrier courrier) {
        String sql = """
            UPDATE courriers 
            SET objet = ?, expediteur = ?, destinataire = ?, reference = ?,
                date_courrier = ?, priorite = ?, observations = ?,
                confidentiel = ?, statut = ?, date_modification = NOW()
            WHERE id = ?
        """;
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, courrier.getObjet());
            stmt.setString(2, courrier.getExpediteur());
            stmt.setString(3, courrier.getDestinataire());
            stmt.setString(4, courrier.getReference());
            
            if (courrier.getDateCourrier() != null) {
                stmt.setDate(5, Date.valueOf(courrier.getDateCourrier()));
            } else {
                stmt.setNull(5, Types.DATE);
            }
            
            stmt.setString(6, courrier.getPriorite());
            stmt.setString(7, courrier.getObservations());
            stmt.setBoolean(8, courrier.isConfidentiel());
            stmt.setString(9, courrier.getStatut());
            stmt.setInt(10, courrier.getId());
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Erreur mise à jour courrier: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Met à jour le statut d'un courrier
     */
    public boolean updateStatut(int courrierId, String nouveauStatut) {
        String sql = "UPDATE courriers SET statut = ?, date_modification = NOW() WHERE id = ?";
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, nouveauStatut);
            stmt.setInt(2, courrierId);
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Erreur mise à jour statut: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Archive un courrier
     */
    public boolean archiverCourrier(int courrierId) {
        String sql = """
            UPDATE courriers 
            SET statut = 'archive', 
                date_archivage = NOW(),
                date_modification = NOW()
            WHERE id = ?
        """;
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, courrierId);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Erreur archivage courrier: " + e.getMessage());
        }
        
        return false;
    }
    
    // ============================================================================
    // NOTIFICATIONS
    // ============================================================================
    
    /**
     * Marque une notification comme lue
     */
    public boolean marquerNotificationLue(int courrierId, int userId) {
        String sql = """
            UPDATE notifications_courrier 
            SET lu = 1, date_lecture = NOW()
            WHERE courrier_id = ? AND user_id = ?
        """;
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, courrierId);
            stmt.setInt(2, userId);
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Erreur marquage notification: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Compte les notifications non lues d'un utilisateur
     */
    public int compterNotificationsNonLues(int userId) {
        String sql = """
            SELECT COUNT(*) 
            FROM notifications_courrier 
            WHERE user_id = ? AND lu = 0
        """;
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur comptage notifications: " + e.getMessage());
        }
        
        return 0;
    }
    
    // ============================================================================
    // MÉTHODES UTILITAIRES PRIVÉES
    // ============================================================================
    
    /**
     * Mappe un ResultSet vers un objet Courrier
     */
    private Courrier mapResultSetToCourrier(ResultSet rs) throws SQLException {
        Courrier courrier = new Courrier();
        
        courrier.setId(rs.getInt("id"));
        courrier.setCodeCourrier(rs.getString("code_courrier"));
        courrier.setDocumentId(rs.getInt("document_id"));
        courrier.setTypeCourrier(rs.getString("type_courrier"));
        courrier.setObjet(rs.getString("objet"));
        courrier.setExpediteur(rs.getString("expediteur"));
        courrier.setDestinataire(rs.getString("destinataire"));
        courrier.setReference(rs.getString("reference"));
        
        Date dateCourrier = rs.getDate("date_courrier");
        if (dateCourrier != null) {
            courrier.setDateCourrier(dateCourrier.toLocalDate());
        }
        
        courrier.setPriorite(rs.getString("priorite"));
        courrier.setObservations(rs.getString("observations"));
        courrier.setConfidentiel(rs.getBoolean("confidentiel"));
        courrier.setStatut(rs.getString("statut"));
        
        Timestamp dateArchivage = rs.getTimestamp("date_archivage");
        if (dateArchivage != null) {
            courrier.setDateArchivage(dateArchivage.toLocalDateTime());
        }
        
        int creePar = rs.getInt("cree_par");
        if (!rs.wasNull()) {
            courrier.setCreePar(creePar);
        }
        
        Timestamp dateCreation = rs.getTimestamp("date_creation");
        if (dateCreation != null) {
            courrier.setDateCreation(dateCreation.toLocalDateTime());
        }
        
        Timestamp dateModification = rs.getTimestamp("date_modification");
        if (dateModification != null) {
            courrier.setDateModification(dateModification.toLocalDateTime());
        }
        
        // Champs additionnels
        try {
            courrier.setCreateurNom(rs.getString("createur_nom"));
            int nbCotations = rs.getInt("nb_cotations");
            courrier.setNombreCotations(nbCotations);
            courrier.setADesCotations(nbCotations > 0);
        } catch (SQLException e) {
            // Colonnes optionnelles, ignorer si absentes
        }
        
        return courrier;
    }
}