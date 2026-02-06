package application.models;

/**
 * Énumération pour le type de courrier
 */
public enum TypeCourrier {
	ENTRANT("Entrant", "📥", "#e67e22"),
    SORTANT("Sortant", "📤", "#3498db"),
    INTERNE("Interne", "🔄", "#95a5a6"),
    URGENT("Urgent", "🚨", "#ff0000");
    
    private final String libelle;
    private final String icone;
    private final String couleur;
    
    TypeCourrier(String libelle, String icone, String couleur) {
        this.libelle = libelle;
        this.icone = icone;
        this.couleur = couleur;
    }
    
    public String getLibelle() {
        return libelle;
    }
    
    public String getIcone() {
        return icone;
    }
    
    public String getCouleur() {
        return couleur;
    }
    
    /**
     * Convertit une chaîne en TypeCourrier
     */
    public static TypeCourrier fromString(String value) {
        if (value == null) return null;
        
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    /**
     * Retourne le nom pour la base de données (lowercase)
     */
    public String toDbString() {
        return name().toLowerCase();
    }
    
    @Override
    public String toString() {
        return libelle;
    }
}