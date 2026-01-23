package application.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Modèle représentant une cotation (assignation) de courrier
 * Correspond à la table 'cotations_courriers' de la base de données
 */
public class CotationCourrier {
    // Champs correspondant à la table DB
    private int id;
    private int courrierId;
    private int cotePar;                    // ID de l'utilisateur qui a fait la cotation
    private int coteA;                      // ID de l'utilisateur assigné
    private String serviceDestination;
    private String commentaire;
    private String priorite;                // NORMALE, URGENTE, TRES_URGENTE
    private LocalDateTime dateEcheance;
    private int delaiJours;
    private String statut;                  // en_attente, en_cours, traite, refuse
    private LocalDateTime dateCotation;
    private LocalDateTime datePriseEnCharge;
    private LocalDateTime dateTraitement;
    private String commentaireTraitement;
    private boolean notifierUtilisateur;
    
    // Champs additionnels pour l'affichage (issus des JOINs)
    private String courrierCode;            // Code du courrier
    private String courrierObjet;           // Objet du courrier
    private String coteurNom;               // Nom complet de qui a coté
    private String assigneNom;              // Nom complet de l'assigné
    private String serviceName;             // Nom complet du service
    private String prioriteCourrier;        // Priorité du courrier lui-même
    private String statutCourrier;          // Statut du courrier lui-même
    private String typeCourrier;            // Type du courrier (ENTRANT, SORTANT, INTERNE)
    private String expediteurCourrier;      // Expéditeur du courrier
    
    // Constructeurs
    public CotationCourrier() {
        this.statut = "en_attente";
        this.priorite = "NORMALE";
        this.delaiJours = 3;
        this.notifierUtilisateur = true;
        this.dateCotation = LocalDateTime.now();
    }
    
    public CotationCourrier(int courrierId, int cotePar, int coteA) {
        this();
        this.courrierId = courrierId;
        this.cotePar = cotePar;
        this.coteA = coteA;
        this.dateEcheance = LocalDateTime.now().plusDays(delaiJours);
    }
    
    // ============================================================================
    // Getters et Setters
    // ============================================================================
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getCourrierId() {
        return courrierId;
    }
    
    public void setCourrierId(int courrierId) {
        this.courrierId = courrierId;
    }
    
    public int getCotePar() {
        return cotePar;
    }
    
    public void setCotePar(int cotePar) {
        this.cotePar = cotePar;
    }
    
    public int getCoteA() {
        return coteA;
    }
    
    public void setCoteA(int coteA) {
        this.coteA = coteA;
    }
    
    public String getServiceDestination() {
        return serviceDestination;
    }
    
    public void setServiceDestination(String serviceDestination) {
        this.serviceDestination = serviceDestination;
    }
    
    public String getCommentaire() {
        return commentaire;
    }
    
    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }
    
    public String getPriorite() {
        return priorite;
    }
    
    public void setPriorite(String priorite) {
        this.priorite = priorite;
    }
    
    public LocalDateTime getDateEcheance() {
        return dateEcheance;
    }
    
    public void setDateEcheance(LocalDateTime dateEcheance) {
        this.dateEcheance = dateEcheance;
    }
    
    public int getDelaiJours() {
        return delaiJours;
    }
    
    public void setDelaiJours(int delaiJours) {
        this.delaiJours = delaiJours;
        // Recalculer la date d'échéance
        if (this.dateCotation != null) {
            this.dateEcheance = this.dateCotation.plusDays(delaiJours);
        }
    }
    
    public String getStatut() {
        return statut;
    }
    
    public void setStatut(String statut) {
        this.statut = statut;
    }
    
    public LocalDateTime getDateCotation() {
        return dateCotation;
    }
    
    public void setDateCotation(LocalDateTime dateCotation) {
        this.dateCotation = dateCotation;
    }
    
    public LocalDateTime getDatePriseEnCharge() {
        return datePriseEnCharge;
    }
    
    public void setDatePriseEnCharge(LocalDateTime datePriseEnCharge) {
        this.datePriseEnCharge = datePriseEnCharge;
    }
    
    public LocalDateTime getDateTraitement() {
        return dateTraitement;
    }
    
    public void setDateTraitement(LocalDateTime dateTraitement) {
        this.dateTraitement = dateTraitement;
    }
    
    public String getCommentaireTraitement() {
        return commentaireTraitement;
    }
    
    public void setCommentaireTraitement(String commentaireTraitement) {
        this.commentaireTraitement = commentaireTraitement;
    }
    
    public boolean isNotifierUtilisateur() {
        return notifierUtilisateur;
    }
    
    public void setNotifierUtilisateur(boolean notifierUtilisateur) {
        this.notifierUtilisateur = notifierUtilisateur;
    }
    
    // Champs additionnels
    public String getCourrierCode() {
        return courrierCode;
    }
    
    public void setCourrierCode(String courrierCode) {
        this.courrierCode = courrierCode;
    }
    
    public String getCourrierObjet() {
        return courrierObjet;
    }
    
    public void setCourrierObjet(String courrierObjet) {
        this.courrierObjet = courrierObjet;
    }
    
    public String getCoteurNom() {
        return coteurNom;
    }
    
    public void setCoteurNom(String coteurNom) {
        this.coteurNom = coteurNom;
    }
    
    public String getAssigneNom() {
        return assigneNom;
    }
    
    public void setAssigneNom(String assigneNom) {
        this.assigneNom = assigneNom;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public String getPrioriteCourrier() {
        return prioriteCourrier;
    }
    
    public void setPrioriteCourrier(String prioriteCourrier) {
        this.prioriteCourrier = prioriteCourrier;
    }
    
    public String getStatutCourrier() {
        return statutCourrier;
    }
    
    public void setStatutCourrier(String statutCourrier) {
        this.statutCourrier = statutCourrier;
    }
    
    // ============================================================================
    // Méthodes utilitaires
    // ============================================================================
    
    /**
     * Retourne la date d'échéance formatée
     */
    public String getDateEcheanceFormatee() {
        if (dateEcheance == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return dateEcheance.format(formatter);
    }
    
    /**
     * Retourne la date d'échéance avec heure formatée
     */
    public String getDateEcheanceAvecHeureFormatee() {
        if (dateEcheance == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return dateEcheance.format(formatter);
    }
    
    /**
     * Retourne la date de cotation formatée
     */
    public String getDateCotationFormatee() {
        if (dateCotation == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return dateCotation.format(formatter);
    }
    
    /**
     * Retourne la date de traitement formatée
     */
    public String getDateTraitementFormatee() {
        if (dateTraitement == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return dateTraitement.format(formatter);
    }
    
    /**
     * Vérifie si la cotation est en retard
     */
    public boolean isEnRetard() {
        if (dateEcheance == null || statut == null) return false;
        
        // Si déjà traité, pas en retard
        if (statut.equalsIgnoreCase("traite")) return false;
        
        // Comparer avec maintenant
        return LocalDateTime.now().isAfter(dateEcheance);
    }
    
    /**
     * Calcule le nombre de jours de retard
     */
    public long getJoursRetard() {
        if (!isEnRetard()) return 0;
        
        return ChronoUnit.DAYS.between(dateEcheance, LocalDateTime.now());
    }
    
    /**
     * Calcule le nombre de jours restants
     */
    public long getJoursRestants() {
        if (dateEcheance == null || statut == null) return 0;
        
        // Si déjà traité, pas de jours restants
        if (statut.equalsIgnoreCase("traite")) return 0;
        
        long jours = ChronoUnit.DAYS.between(LocalDateTime.now(), dateEcheance);
        return Math.max(0, jours);
    }
    
    /**
     * Retourne le libellé du statut
     */
    public String getStatutLibelle() {
        if (statut == null) return "En attente";
        
        switch (statut.toLowerCase()) {
            case "en_attente":
                return "En attente";
            case "en_cours":
                return "En cours";
            case "traite":
                return "Traité";
            case "refuse":
                return "Refusé";
            default:
                return statut;
        }
    }
    
    /**
     * Retourne l'icône du statut
     */
    public String getStatutIcone() {
        if (statut == null) return "⏸️";
        
        switch (statut.toLowerCase()) {
            case "en_attente":
                return "⏸️";
            case "en_cours":
                return "⏳";
            case "traite":
                return "✅";
            case "refuse":
                return "❌";
            default:
                return "⚪";
        }
    }
    
    /**
     * Retourne la couleur du statut
     */
    public String getStatutCouleur() {
        if (statut == null) return "#f39c12";
        
        switch (statut.toLowerCase()) {
            case "en_attente":
                return "#f39c12";
            case "en_cours":
                return "#3498db";
            case "traite":
                return "#27ae60";
            case "refuse":
                return "#e74c3c";
            default:
                return "#9e9e9e";
        }
    }
    
    /**
     * Retourne le libellé de la priorité
     */
    public String getPrioriteLibelle() {
        if (priorite == null) return "Normale";
        
        switch (priorite.toUpperCase()) {
            case "TRES_URGENTE":
                return "Très Urgente";
            case "URGENTE":
                return "Urgente";
            case "NORMALE":
                return "Normale";
            default:
                return priorite;
        }
    }
    
    /**
     * Retourne l'icône de priorité
     */
    public String getPrioriteIcone() {
        if (priorite == null) return "🟡";
        
        switch (priorite.toUpperCase()) {
            case "TRES_URGENTE":
                return "🚨";
            case "URGENTE":
                return "🔴";
            case "NORMALE":
                return "🟡";
            default:
                return "⚪";
        }
    }
    
    /**
     * Retourne une description du délai
     */
    public String getDelaiDescription() {
        long joursRestants = getJoursRestants();
        
        if (isEnRetard()) {
            long joursRetard = getJoursRetard();
            return "En retard de " + joursRetard + " jour(s)";
        } else if (joursRestants == 0) {
            return "Échéance aujourd'hui";
        } else if (joursRestants == 1) {
            return "1 jour restant";
        } else {
            return joursRestants + " jours restants";
        }
    }
    
    /**
     * Retourne une couleur pour le délai
     */
    public String getDelaiCouleur() {
        if (isEnRetard()) {
            return "#e74c3c"; // Rouge
        }
        
        long joursRestants = getJoursRestants();
        if (joursRestants <= 1) {
            return "#e67e22"; // Orange
        } else if (joursRestants <= 3) {
            return "#f39c12"; // Jaune
        } else {
            return "#27ae60"; // Vert
        }
    }
    
    /**
     * Prendre en charge la cotation (marquer comme en_cours)
     */
    public void prendreEnCharge() {
        if (this.statut != null && this.statut.equalsIgnoreCase("en_attente")) {
            this.statut = "en_cours";
            this.datePriseEnCharge = LocalDateTime.now();
        }
    }
    
    /**
     * Marquer comme traité
     */
    public void marquerTraite(String commentaire) {
        this.statut = "traite";
        this.dateTraitement = LocalDateTime.now();
        this.commentaireTraitement = commentaire;
    }
    
    /**
     * Refuser la cotation
     */
    public void refuser(String motif) {
        this.statut = "refuse";
        this.commentaireTraitement = "Refusé : " + motif;
        this.dateTraitement = LocalDateTime.now();
    }
    
    /**
     * Vérifie si la cotation peut être prise en charge
     */
    public boolean peutEtrePriseEnCharge() {
        return statut != null && statut.equalsIgnoreCase("en_attente");
    }
    
    /**
     * Vérifie si la cotation peut être traitée
     */
    public boolean peutEtreTraitee() {
        return statut != null && 
               (statut.equalsIgnoreCase("en_attente") || statut.equalsIgnoreCase("en_cours"));
    }
    
    /**
     * Retourne un résumé de la cotation
     */
    public String getResume() {
        StringBuilder resume = new StringBuilder();
        
        if (courrierCode != null) {
            resume.append(courrierCode).append(" - ");
        }
        
        if (assigneNom != null) {
            resume.append("Assigné à ").append(assigneNom);
        }
        
        if (isEnRetard()) {
            resume.append(" [EN RETARD]");
        }
        
        return resume.toString();
    }
    
    /**
     * Type du courrier
     */
    public String getTypeCourrier() {
        return typeCourrier;
    }

    public void setTypeCourrier(String typeCourrier) {
        this.typeCourrier = typeCourrier;
    }

    /**
     * Expéditeur du courrier
     */
    public String getExpediteurCourrier() {
        return expediteurCourrier;
    }

    public void setExpediteurCourrier(String expediteurCourrier) {
        this.expediteurCourrier = expediteurCourrier;
    }

    /**
     * Retourne le libellé du type de courrier
     */
    public String getTypeCourrierLibelle() {
        if (typeCourrier == null) return "Courrier";
        
        switch (typeCourrier.toUpperCase()) {
            case "ENTRANT":
                return "Entrant";
            case "SORTANT":
                return "Sortant";
            case "INTERNE":
                return "Interne";
            default:
                return typeCourrier;
        }
    }

    /**
     * Retourne l'icône du type de courrier
     */
    public String getTypeCourrierIcone() {
        if (typeCourrier == null) return "📧";
        
        switch (typeCourrier.toUpperCase()) {
            case "ENTRANT":
                return "📥";
            case "SORTANT":
                return "📤";
            case "INTERNE":
                return "🔄";
            default:
                return "📧";
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CotationCourrier that = (CotationCourrier) o;
        return id == that.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "CotationCourrier{" +
                "id=" + id +
                ", courrierId=" + courrierId +
                ", coteA=" + coteA +
                ", statut='" + statut + '\'' +
                ", dateEcheance=" + dateEcheance +
                ", enRetard=" + isEnRetard() +
                '}';
    }
}