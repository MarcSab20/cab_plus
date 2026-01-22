package application.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Modèle représentant un courrier - VERSION ALIGNÉE AVEC LA BASE DE DONNÉES
 * Correspond à la table 'courriers' de la base de données document
 */
public class Courrier {
    // Champs correspondant à la table DB
    private int id;
    private String codeCourrier;           // Format: COU-ANNÉE-SÉQUENCE
    private int documentId;                // Document obligatoire
    private String typeCourrier;           // ENTRANT, SORTANT, INTERNE
    private String objet;
    private String expediteur;
    private String destinataire;
    private String reference;
    private LocalDate dateCourrier;
    private String priorite;               // NORMALE, URGENTE, TRES_URGENTE
    private String observations;
    private boolean confidentiel;
    private String statut;                 // nouveau, en_cours, traite, archive
    private LocalDateTime dateArchivage;
    private Integer creePar;               // user_id qui a créé le courrier
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    
    // Champs additionnels pour l'affichage et la gestion
    private String expediteurNom;          // Nom complet de l'expéditeur (si user)
    private String destinataireNom;        // Nom complet du destinataire (si user)
    private String createurNom;            // Nom de l'utilisateur créateur
    private boolean aDesCotations;         // Indicateur si le courrier a des cotations
    private int nombreCotations;           // Nombre de cotations actives
    
    // Constructeurs
    public Courrier() {
        this.confidentiel = false;
        this.statut = "nouveau";
        this.priorite = "NORMALE";
        this.dateCreation = LocalDateTime.now();
        this.aDesCotations = false;
        this.nombreCotations = 0;
    }
    
    public Courrier(String codeCourrier, String typeCourrier, String objet) {
        this();
        this.codeCourrier = codeCourrier;
        this.typeCourrier = typeCourrier;
        this.objet = objet;
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
    
    public String getCodeCourrier() {
        return codeCourrier;
    }
    
    public void setCodeCourrier(String codeCourrier) {
        this.codeCourrier = codeCourrier;
    }
    
    public int getDocumentId() {
        return documentId;
    }
    
    public void setDocumentId(int documentId) {
        this.documentId = documentId;
    }
    
    public String getTypeCourrier() {
        return typeCourrier;
    }
    
    public void setTypeCourrier(String typeCourrier) {
        this.typeCourrier = typeCourrier;
    }
    
    public String getObjet() {
        return objet;
    }
    
    public void setObjet(String objet) {
        this.objet = objet;
    }
    
    public String getExpediteur() {
        return expediteur;
    }
    
    public void setExpediteur(String expediteur) {
        this.expediteur = expediteur;
    }
    
    public String getDestinataire() {
        return destinataire;
    }
    
    public void setDestinataire(String destinataire) {
        this.destinataire = destinataire;
    }
    
    public String getReference() {
        return reference;
    }
    
    public void setReference(String reference) {
        this.reference = reference;
    }
    
    public LocalDate getDateCourrier() {
        return dateCourrier;
    }
    
    public void setDateCourrier(LocalDate dateCourrier) {
        this.dateCourrier = dateCourrier;
    }
    
    public String getPriorite() {
        return priorite;
    }
    
    public void setPriorite(String priorite) {
        this.priorite = priorite;
    }
    
    public String getObservations() {
        return observations;
    }
    
    public void setObservations(String observations) {
        this.observations = observations;
    }
    
    public boolean isConfidentiel() {
        return confidentiel;
    }
    
    public void setConfidentiel(boolean confidentiel) {
        this.confidentiel = confidentiel;
    }
    
    public String getStatut() {
        return statut;
    }
    
    public void setStatut(String statut) {
        this.statut = statut;
    }
    
    public LocalDateTime getDateArchivage() {
        return dateArchivage;
    }
    
    public void setDateArchivage(LocalDateTime dateArchivage) {
        this.dateArchivage = dateArchivage;
    }
    
    public Integer getCreePar() {
        return creePar;
    }
    
    public void setCreePar(Integer creePar) {
        this.creePar = creePar;
    }
    
    public LocalDateTime getDateCreation() {
        return dateCreation;
    }
    
    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
    
    public LocalDateTime getDateModification() {
        return dateModification;
    }
    
    public void setDateModification(LocalDateTime dateModification) {
        this.dateModification = dateModification;
    }
    
    // Champs additionnels
    public String getExpediteurNom() {
        return expediteurNom;
    }
    
    public void setExpediteurNom(String expediteurNom) {
        this.expediteurNom = expediteurNom;
    }
    
    public String getDestinataireNom() {
        return destinataireNom;
    }
    
    public void setDestinataireNom(String destinataireNom) {
        this.destinataireNom = destinataireNom;
    }
    
    public String getCreateurNom() {
        return createurNom;
    }
    
    public void setCreateurNom(String createurNom) {
        this.createurNom = createurNom;
    }
    
    public boolean isADesCotations() {
        return aDesCotations;
    }
    
    public void setADesCotations(boolean aDesCotations) {
        this.aDesCotations = aDesCotations;
    }
    
    public int getNombreCotations() {
        return nombreCotations;
    }
    
    public void setNombreCotations(int nombreCotations) {
        this.nombreCotations = nombreCotations;
    }
    
    // ============================================================================
    // Méthodes utilitaires pour l'affichage
    // ============================================================================
    
    /**
     * Retourne la date du courrier formatée
     */
    public String getDateCourrierFormatee() {
        if (dateCourrier == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return dateCourrier.format(formatter);
    }
    
    /**
     * Retourne la date de création formatée
     */
    public String getDateCreationFormatee() {
        if (dateCreation == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return dateCreation.format(formatter);
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
     * Retourne la couleur associée à la priorité
     */
    public String getPrioriteCouleur() {
        if (priorite == null) return "#f39c12";
        
        switch (priorite.toUpperCase()) {
            case "TRES_URGENTE":
                return "#c0392b";
            case "URGENTE":
                return "#e74c3c";
            case "NORMALE":
                return "#f39c12";
            default:
                return "#9e9e9e";
        }
    }
    
    /**
     * Retourne le libellé du statut
     */
    public String getStatutLibelle() {
        if (statut == null) return "Nouveau";
        
        switch (statut.toLowerCase()) {
            case "nouveau":
                return "Nouveau";
            case "en_cours":
                return "En cours";
            case "traite":
                return "Traité";
            case "archive":
                return "Archivé";
            default:
                return statut;
        }
    }
    
    /**
     * Retourne l'icône du statut
     */
    public String getStatutIcone() {
        if (statut == null) return "🆕";
        
        switch (statut.toLowerCase()) {
            case "nouveau":
                return "🆕";
            case "en_cours":
                return "⏳";
            case "traite":
                return "✅";
            case "archive":
                return "📦";
            default:
                return "⚪";
        }
    }
    
    /**
     * Retourne la couleur associée au statut
     */
    public String getStatutCouleur() {
        if (statut == null) return "#3498db";
        
        switch (statut.toLowerCase()) {
            case "nouveau":
                return "#3498db";
            case "en_cours":
                return "#f39c12";
            case "traite":
                return "#27ae60";
            case "archive":
                return "#95a5a6";
            default:
                return "#9e9e9e";
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
    
    /**
     * Retourne le libellé du type
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
     * Vérifie si le courrier peut être coté
     */
    public boolean peutEtreCote() {
        return statut != null && !statut.equalsIgnoreCase("archive");
    }
    
    /**
     * Vérifie si le courrier peut être traité
     */
    public boolean peutEtreTraite() {
        return statut != null && 
               (statut.equalsIgnoreCase("nouveau") || statut.equalsIgnoreCase("en_cours"));
    }
    
    /**
     * Retourne un résumé du courrier
     */
    public String getResume() {
        StringBuilder resume = new StringBuilder();
        resume.append(codeCourrier).append(" - ");
        resume.append(objet);
        
        if (priorite != null && (priorite.equalsIgnoreCase("URGENTE") || priorite.equalsIgnoreCase("TRES_URGENTE"))) {
            resume.append(" [").append(getPrioriteLibelle()).append("]");
        }
        
        if (confidentiel) {
            resume.append(" [CONFIDENTIEL]");
        }
        
        return resume.toString();
    }
    
    /**
     * Retourne le nom d'affichage de l'expéditeur
     */
    public String getExpediteurAffichage() {
        return expediteurNom != null ? expediteurNom : expediteur;
    }
    
    /**
     * Retourne le nom d'affichage du destinataire
     */
    public String getDestinataireAffichage() {
        return destinataireNom != null ? destinataireNom : destinataire;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Courrier courrier = (Courrier) o;
        return id == courrier.id;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Courrier{" +
                "id=" + id +
                ", code='" + codeCourrier + '\'' +
                ", objet='" + objet + '\'' +
                ", statut='" + statut + '\'' +
                ", priorite='" + priorite + '\'' +
                ", cotations=" + nombreCotations +
                '}';
    }
}