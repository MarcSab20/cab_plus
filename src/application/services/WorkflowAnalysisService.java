package application.services;

import application.models.*;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service d'analyse approfondie du workflow des courriers
 * Utilise historique_courriers et cotations_courriers pour tracer les flux
 */
public class WorkflowAnalysisService {
    private static WorkflowAnalysisService instance;
    private Map<String, ServiceHierarchy> hierarchyCache;
    private WorkflowAnalysisService() {}
    
    public static synchronized WorkflowAnalysisService getInstance() {
        if (instance == null) {
            instance = new WorkflowAnalysisService();
        }
        return instance;
    }
    
    /**
     * Récupère le parcours complet d'un courrier via historique et cotations
     */
    public List<WorkflowStep> getCourrierParcours(int courrierId) {
        List<WorkflowStep> steps = new ArrayList<>();
        
        String sql = """
            SELECT 
                h.id,
                h.courrier_id,
                h.action,
                h.description,
                h.date_action,
                h.user_id,
                CONCAT(u.prenom, ' ', u.nom) as user_name,
                c.service_destination,
                s.service_name
            FROM historique_courriers h
            LEFT JOIN users u ON h.user_id = u.id
            LEFT JOIN cotations_courriers c ON h.courrier_id = c.courrier_id 
                AND h.action IN ('cotation', 'traitement', 'transfert')
            LEFT JOIN service_hierarchy s ON c.service_destination = s.service_code
            WHERE h.courrier_id = ?
            ORDER BY h.date_action ASC
        """;
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, courrierId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                int etapeNum = 1;
                while (rs.next()) {
                    WorkflowStep step = new WorkflowStep();
                    step.setId(rs.getInt("id"));
                    step.setCourrierId(rs.getInt("courrier_id"));
                    step.setEtapeNumero(etapeNum++);
                    step.setAction(rs.getString("action"));
                    step.setCommentaire(rs.getString("description"));
                    step.setDateAction(rs.getTimestamp("date_action").toLocalDateTime());
                    
                    int userId = rs.getInt("user_id");
                    if (!rs.wasNull()) {
                        step.setUserId(userId);
                        step.setUserName(rs.getString("user_name"));
                    }
                    
                    String serviceCode = rs.getString("service_destination");
                    if (serviceCode != null) {
                        step.setServiceCode(serviceCode);
                        step.setServiceName(rs.getString("service_name"));
                    }
                    
                    // Déterminer le statut de l'étape
                    String action = rs.getString("action");
                    if (action.equalsIgnoreCase("traitement")) {
                        step.setStatutEtape(StatutEtapeWorkflow.TERMINE);
                    } else if (action.equalsIgnoreCase("cotation")) {
                        step.setStatutEtape(StatutEtapeWorkflow.EN_ATTENTE);
                    } else {
                        step.setStatutEtape(StatutEtapeWorkflow.EN_COURS);
                    }
                    
                    steps.add(step);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération parcours courrier: " + e.getMessage());
            e.printStackTrace();
        }
        
        return steps;
    }
    
    /**
     * Récupère les statistiques de flux entre services
     */
    public Map<String, ServiceFlowStats> getServicesFlowStats(LocalDateTime dateDebut, LocalDateTime dateFin) {
        Map<String, ServiceFlowStats> stats = new HashMap<>();
        
        String sql = """
            SELECT 
                c.service_destination as service_code,
                s.service_name,
                COUNT(DISTINCT c.courrier_id) as total_courriers,
                COUNT(CASE WHEN cot.statut = 'en_cours' THEN 1 END) as en_cours,
                COUNT(CASE WHEN cot.statut = 'en_attente' THEN 1 END) as en_attente,
                COUNT(CASE WHEN cot.statut = 'traite' THEN 1 END) as traites,
                AVG(TIMESTAMPDIFF(HOUR, cot.date_cotation, COALESCE(cot.date_traitement, NOW()))) as duree_moyenne,
                SUM(CASE WHEN cot.date_echeance < NOW() AND cot.statut != 'traite' THEN 1 ELSE 0 END) as retards
            FROM cotations_courriers cot
            INNER JOIN courriers c ON cot.courrier_id = c.id
            LEFT JOIN service_hierarchy s ON cot.service_destination = s.service_code
            WHERE cot.date_cotation BETWEEN ? AND ?
            GROUP BY c.service_destination, s.service_name
        """;
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, Timestamp.valueOf(dateDebut));
            stmt.setTimestamp(2, Timestamp.valueOf(dateFin));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String serviceCode = rs.getString("service_code");
                    if (serviceCode == null) continue;
                    
                    ServiceFlowStats serviceStat = new ServiceFlowStats(
                        serviceCode,
                        rs.getString("service_name")
                    );
                    
                    int totalCourriers = rs.getInt("total_courriers");
                    int enCours = rs.getInt("en_cours");
                    int enAttente = rs.getInt("en_attente");
                    int traites = rs.getInt("traites");
                    double dureeMoyenne = rs.getDouble("duree_moyenne");
                    int retards = rs.getInt("retards");
                    
                    // Simuler les flux (entrants = nouveau, sortants = traités, internes = en_cours)
                    serviceStat.setFluxEntrants(enAttente);
                    serviceStat.setFluxSortants(traites);
                    serviceStat.setFluxInternes(enCours);
                    serviceStat.setDureeMoyenne(dureeMoyenne);
                    serviceStat.setRetards(retards);
                    serviceStat.setTotalCourriersTraites(totalCourriers);
                    
                    stats.put(serviceCode, serviceStat);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur calcul statistiques flux: " + e.getMessage());
            e.printStackTrace();
        }
        
        return stats;
    }
    
    /**
     * Analyse les goulots d'étranglement dans le workflow
     */
    public List<GoulotInfo> detecterGoulots(LocalDateTime dateDebut, LocalDateTime dateFin) {
        List<GoulotInfo> goulots = new ArrayList<>();
        
        String sql = """
            SELECT 
                cot.service_destination as service_code,
                s.service_name,
                COUNT(*) as courriers_en_retard,
                AVG(TIMESTAMPDIFF(HOUR, cot.date_echeance, NOW())) as heures_retard_moyen,
                AVG(TIMESTAMPDIFF(HOUR, cot.date_cotation, COALESCE(cot.date_traitement, NOW()))) as duree_traitement_moyenne
            FROM cotations_courriers cot
            LEFT JOIN service_hierarchy s ON cot.service_destination = s.service_code
            WHERE cot.date_echeance < NOW()
              AND cot.statut != 'traite'
              AND cot.date_cotation BETWEEN ? AND ?
            GROUP BY cot.service_destination, s.service_name
            HAVING COUNT(*) >= 3
            ORDER BY courriers_en_retard DESC, heures_retard_moyen DESC
        """;
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, Timestamp.valueOf(dateDebut));
            stmt.setTimestamp(2, Timestamp.valueOf(dateFin));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    GoulotInfo goulot = new GoulotInfo();
                    goulot.serviceCode = rs.getString("service_code");
                    goulot.serviceName = rs.getString("service_name");
                    goulot.courriersEnRetard = rs.getInt("courriers_en_retard");
                    goulot.heuresRetardMoyen = rs.getDouble("heures_retard_moyen");
                    goulot.dureeTraitementMoyenne = rs.getDouble("duree_traitement_moyenne");
                    
                    // Calcul du score de sévérité (0-100)
                    goulot.severite = calculateSeverityScore(
                        goulot.courriersEnRetard,
                        goulot.heuresRetardMoyen,
                        goulot.dureeTraitementMoyenne
                    );
                    
                    goulots.add(goulot);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur détection goulots: " + e.getMessage());
            e.printStackTrace();
        }
        
        return goulots;
    }
    
    /**
     * Calcule un score de sévérité pour un goulot (0-100)
     */
    private int calculateSeverityScore(int courriersEnRetard, double heuresRetard, double dureeTraitement) {
        // Composantes du score
        double scoreVolume = Math.min(100, (courriersEnRetard / 10.0) * 30); // Max 30 points
        double scoreRetard = Math.min(100, (heuresRetard / 168.0) * 40); // Max 40 points (168h = 1 semaine)
        double scoreLenteur = Math.min(100, (dureeTraitement / 72.0) * 30); // Max 30 points (72h = 3 jours)
        
        return (int) (scoreVolume + scoreRetard + scoreLenteur);
    }
    
    /**
     * Récupère les statistiques détaillées d'un service incluant ses sous-services
     */
    public ServiceDetailedStats getServiceDetailedStats(String serviceCode, LocalDateTime dateDebut, LocalDateTime dateFin) {
        ServiceDetailedStats stats = new ServiceDetailedStats();
        stats.serviceCode = serviceCode;
        
        // Récupérer le service et ses descendants
        WorkflowService workflowService = WorkflowService.getInstance();
        ServiceHierarchy service = workflowService.getServiceByCode(serviceCode);
        
        if (service == null) {
            return stats;
        }
        
        stats.serviceName = service.getServiceName();
        stats.niveau = service.getNiveau();
        
        // Liste des codes de service à inclure (service + descendants)
        List<String> serviceCodes = new ArrayList<>();
        serviceCodes.add(serviceCode);
        service.getTousLesDescendants().forEach(s -> serviceCodes.add(s.getServiceCode()));
        
        // Construire la clause IN pour SQL
        String inClause = serviceCodes.stream()
            .map(s -> "?")
            .collect(Collectors.joining(","));
        
        String sql = "SELECT " +
                "cot.service_destination as service_code, " +
                "s.service_name, " +
                "s.niveau as service_niveau, " +
                "COUNT(DISTINCT cot.courrier_id) as total_courriers, " +
                "COUNT(CASE WHEN cot.statut = 'traite' THEN 1 END) as courriers_traites, " +
                "COUNT(CASE WHEN cot.statut = 'en_cours' THEN 1 END) as courriers_en_cours, " +
                "COUNT(CASE WHEN cot.statut = 'en_attente' THEN 1 END) as courriers_en_attente, " +
                "AVG(TIMESTAMPDIFF(HOUR, cot.date_cotation, COALESCE(cot.date_traitement, NOW()))) as duree_moyenne_heures, " +
                "SUM(CASE WHEN cot.date_echeance < NOW() AND cot.statut != 'traite' THEN 1 ELSE 0 END) as courriers_en_retard, " +
                "MIN(cot.date_cotation) as premiere_cotation, " +
                "MAX(COALESCE(cot.date_traitement, NOW())) as derniere_action " +
            "FROM cotations_courriers cot " +
            "LEFT JOIN service_hierarchy s ON cot.service_destination = s.service_code " +
            "WHERE cot.service_destination IN (" + inClause + ") " +
            "  AND cot.date_cotation BETWEEN ? AND ? " +
            "GROUP BY cot.service_destination, s.service_name, s.niveau " +
            "ORDER BY s.niveau, s.service_name";
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // Paramètres pour les services
            int paramIndex = 1;
            for (String code : serviceCodes) {
                stmt.setString(paramIndex++, code);
            }
            
            // Paramètres pour les dates
            stmt.setTimestamp(paramIndex++, Timestamp.valueOf(dateDebut));
            stmt.setTimestamp(paramIndex, Timestamp.valueOf(dateFin));
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ServiceStats subStats = new ServiceStats();
                    subStats.serviceCode = rs.getString("service_code");
                    subStats.serviceName = rs.getString("service_name");
                    subStats.niveau = rs.getInt("service_niveau");
                    subStats.totalCourriers = rs.getInt("total_courriers");
                    subStats.courriersTraites = rs.getInt("courriers_traites");
                    subStats.courriersEnCours = rs.getInt("courriers_en_cours");
                    subStats.courriersEnAttente = rs.getInt("courriers_en_attente");
                    subStats.dureeMoyenneHeures = rs.getDouble("duree_moyenne_heures");
                    subStats.courriersEnRetard = rs.getInt("courriers_en_retard");
                    
                    Timestamp premiereCotation = rs.getTimestamp("premiere_cotation");
                    if (premiereCotation != null) {
                        subStats.premiereCotation = premiereCotation.toLocalDateTime();
                    }
                    
                    Timestamp derniereAction = rs.getTimestamp("derniere_action");
                    if (derniereAction != null) {
                        subStats.derniereAction = derniereAction.toLocalDateTime();
                    }
                    
                    // Calculer le taux de réussite
                    if (subStats.totalCourriers > 0) {
                        subStats.tauxReussite = (double) subStats.courriersTraites / subStats.totalCourriers * 100;
                    }
                    
                    stats.sousServices.add(subStats);
                    
                    // Agréger dans les stats principales
                    stats.totalCourriers += subStats.totalCourriers;
                    stats.courriersTraites += subStats.courriersTraites;
                    stats.courriersEnCours += subStats.courriersEnCours;
                    stats.courriersEnAttente += subStats.courriersEnAttente;
                    stats.courriersEnRetard += subStats.courriersEnRetard;
                }
            }
            
            // Calculer la durée moyenne globale
            if (!stats.sousServices.isEmpty()) {
                stats.dureeMoyenneHeures = stats.sousServices.stream()
                    .mapToDouble(s -> s.dureeMoyenneHeures)
                    .average()
                    .orElse(0.0);
            }
            
            // Calculer le taux de réussite global
            if (stats.totalCourriers > 0) {
                stats.tauxReussite = (double) stats.courriersTraites / stats.totalCourriers * 100;
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur stats détaillées service: " + e.getMessage());
            e.printStackTrace();
        }
        
        return stats;
    }
    
    /**
     * Récupère les courriers en cours dans un service
     */
    public List<Courrier> getCourriersEnCoursService(String serviceCode) {
        List<Courrier> courriers = new ArrayList<>();
        
        String sql = """
            SELECT DISTINCT c.*,
                   CONCAT(u.prenom, ' ', u.nom) as createur_nom,
                   cot.date_echeance,
                   cot.priorite as priorite_cotation,
                   cot.statut as statut_cotation
            FROM courriers c
            INNER JOIN cotations_courriers cot ON c.id = cot.courrier_id
            LEFT JOIN users u ON c.cree_par = u.id
            WHERE cot.service_destination = ?
              AND cot.statut IN ('en_attente', 'en_cours')
              AND c.statut != 'archive'
            ORDER BY cot.date_echeance ASC, cot.priorite DESC
        """;
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, serviceCode);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Courrier courrier = mapResultSetToCourrier(rs);
                    courriers.add(courrier);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération courriers en cours: " + e.getMessage());
            e.printStackTrace();
        }
        
        return courriers;
    }
    
    /**
     * Récupère un service par son code
     */
    public ServiceHierarchy getServiceByCode(String serviceCode) {
        if (serviceCode == null || serviceCode.isEmpty()) {
            return null;
        }
        return hierarchyCache.get(serviceCode);
    }
    
    /**
     * Récupère tous les services actifs
     */
    public List<ServiceHierarchy> getAllServices() {
        return new ArrayList<>(hierarchyCache.values());
    }
    
    /**
     * Mappe un ResultSet vers un Courrier
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
        
        Timestamp dateCreation = rs.getTimestamp("date_creation");
        if (dateCreation != null) {
            courrier.setDateCreation(dateCreation.toLocalDateTime());
        }
        
        try {
            courrier.setCreateurNom(rs.getString("createur_nom"));
        } catch (SQLException e) {
            // Colonne optionnelle
        }
        
        return courrier;
    }
    
    // ===== CLASSES INTERNES =====
    
    /**
     * Informations sur un goulot d'étranglement
     */
    public static class GoulotInfo {
        public String serviceCode;
        public String serviceName;
        public int courriersEnRetard;
        public double heuresRetardMoyen;
        public double dureeTraitementMoyenne;
        public int severite; // 0-100
        
        public String getDescription() {
            return String.format("%s: %d courriers en retard (%.1fh de retard moyen)",
                serviceName, courriersEnRetard, heuresRetardMoyen);
        }
        
        public String getSeveriteLabel() {
            if (severite >= 70) return "🔴 CRITIQUE";
            if (severite >= 40) return "🟠 ÉLEVÉ";
            return "🟡 MODÉRÉ";
        }
    }
    
    /**
     * Statistiques détaillées d'un service
     */
    public static class ServiceDetailedStats {
        public String serviceCode;
        public String serviceName;
        public int niveau;
        public int totalCourriers;
        public int courriersTraites;
        public int courriersEnCours;
        public int courriersEnAttente;
        public int courriersEnRetard;
        public double dureeMoyenneHeures;
        public double tauxReussite;
        public List<ServiceStats> sousServices = new ArrayList<>();
    }
    
    /**
     * Statistiques d'un service individuel
     */
    public static class ServiceStats {
        public String serviceCode;
        public String serviceName;
        public int niveau;
        public int totalCourriers;
        public int courriersTraites;
        public int courriersEnCours;
        public int courriersEnAttente;
        public int courriersEnRetard;
        public double dureeMoyenneHeures;
        public double tauxReussite;
        public LocalDateTime premiereCotation;
        public LocalDateTime derniereAction;
        
        public String getDureeMoyenneFormatee() {
            if (dureeMoyenneHeures < 1) {
                return String.format("%.0f min", dureeMoyenneHeures * 60);
            } else if (dureeMoyenneHeures < 24) {
                return String.format("%.1f h", dureeMoyenneHeures);
            } else {
                return String.format("%.1f j", dureeMoyenneHeures / 24);
            }
        }
    }
    
    /**
     * Statistiques de flux pour un service (compatible avec l'ancien code)
     */
    public static class ServiceFlowStats {
        private String serviceCode;
        private String serviceName;
        private int fluxEntrants;
        private int fluxSortants;
        private int fluxInternes;
        private double dureeMoyenne;
        private int retards;
        private int totalCourriersTraites;
        
        public ServiceFlowStats(String serviceCode, String serviceName) {
            this.serviceCode = serviceCode;
            this.serviceName = serviceName;
        }
        
        // Getters et setters
        public String getServiceCode() { return serviceCode; }
        public String getServiceName() { return serviceName; }
        public int getFluxEntrants() { return fluxEntrants; }
        public void setFluxEntrants(int fluxEntrants) { this.fluxEntrants = fluxEntrants; }
        public int getFluxSortants() { return fluxSortants; }
        public void setFluxSortants(int fluxSortants) { this.fluxSortants = fluxSortants; }
        public int getFluxInternes() { return fluxInternes; }
        public void setFluxInternes(int fluxInternes) { this.fluxInternes = fluxInternes; }
        public double getDureeMoyenne() { return dureeMoyenne; }
        public void setDureeMoyenne(double dureeMoyenne) { this.dureeMoyenne = dureeMoyenne; }
        public int getRetards() { return retards; }
        public void setRetards(int retards) { this.retards = retards; }
        public int getTotalCourriersTraites() { return totalCourriersTraites; }
        public void setTotalCourriersTraites(int total) { this.totalCourriersTraites = total; }
        
        public String getDureeMoyenneFormatee() {
            if (dureeMoyenne < 1) {
                return String.format("%.0f min", dureeMoyenne * 60);
            } else if (dureeMoyenne < 24) {
                return String.format("%.1f h", dureeMoyenne);
            } else {
                return String.format("%.1f j", dureeMoyenne / 24);
            }
        }
        
        public boolean estGoulot() {
            int total = fluxEntrants + fluxSortants + fluxInternes;
            return dureeMoyenne > 24 || (total > 0 && retards > total * 0.3);
        }
        
        public int getScorePerformance() {
            double score = 100.0;
            
            int total = fluxEntrants + fluxSortants + fluxInternes;
            if (total > 0) {
                double tauxRetard = (double) retards / total;
                score -= tauxRetard * 50;
            }
            
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
}