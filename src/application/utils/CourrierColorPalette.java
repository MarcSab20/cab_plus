package application.utils;

import javafx.scene.paint.Color;
import java.util.*;

/**
 * Gestionnaire de palette de couleurs pour la visualisation moderne des courriers
 * Fournit des couleurs distinctes, harmonieuses et accessibles
 */
public class CourrierColorPalette {
    
    // Palette moderne de 20 couleurs distinctes et harmonieuses
    private static final String[] MODERN_COLORS = {
        "#FF6B6B", // Rouge coral
        "#4ECDC4", // Turquoise
        "#45B7D1", // Bleu ciel
        "#FFA07A", // Saumon
        "#98D8C8", // Menthe
        "#F7DC6F", // Jaune doré
        "#BB8FCE", // Violet doux
        "#85C1E2", // Bleu clair
        "#F8B739", // Orange doux
        "#52B788", // Vert forêt
        "#E78C8C", // Rose poudré
        "#6C63FF", // Indigo
        "#FF8FA3", // Rose vif
        "#74C0FC", // Azur
        "#FFB84D", // Ambre
        "#A78BFA", // Lavande
        "#34D399", // Émeraude
        "#F472B6", // Magenta
        "#60A5FA", // Bleu royal
        "#FCD34D"  // Or
    };
    
    // Couleurs pour les priorités
    private static final Map<String, String> PRIORITY_COLORS = Map.of(
        "NORMALE", "#27ae60",
        "URGENTE", "#e74c3c",
        "TRES_URGENTE", "#c0392b"
    );
    
    // Couleurs pour les statuts
    private static final Map<String, String> STATUS_COLORS = Map.of(
        "nouveau", "#3498db",
        "en_cours", "#f39c12",
        "traite", "#27ae60",
        "archive", "#95a5a6"
    );
    
    // Cache des couleurs assignées aux courriers
    private static final Map<Integer, String> courrierColorCache = new HashMap<>();
    private static int colorIndex = 0;
    
    /**
     * Obtient une couleur unique pour un courrier
     * Utilise un système de cache pour cohérence
     */
    public static String getColorForCourrier(int courrierId) {
        return courrierColorCache.computeIfAbsent(courrierId, id -> {
            String color = MODERN_COLORS[colorIndex % MODERN_COLORS.length];
            colorIndex++;
            return color;
        });
    }
    
    /**
     * Obtient une couleur par index (pour boucles) - INSTANCE METHOD
     * @param index L'index de la couleur (0-based)
     * @return Couleur JavaFX Color
     */
    public Color getColor(int index) {
        String hexColor = MODERN_COLORS[index % MODERN_COLORS.length];
        return Color.web(hexColor);
    }
    
    /**
     * Obtient une couleur hex par index - STATIC METHOD
     * @param index L'index de la couleur (0-based)
     * @return Couleur en format hex
     */
    public static String getColorHex(int index) {
        return MODERN_COLORS[index % MODERN_COLORS.length];
    }
    
    /**
     * Obtient une couleur Color JavaFX par index - STATIC METHOD
     * @param index L'index de la couleur (0-based)
     * @return Couleur JavaFX Color
     */
    public static Color getColorObj(int index) {
        String hexColor = MODERN_COLORS[index % MODERN_COLORS.length];
        return Color.web(hexColor);
    }
    
    /**
     * Obtient une couleur pour une priorité
     */
    public static String getColorForPriority(String priorite) {
        return PRIORITY_COLORS.getOrDefault(
            priorite != null ? priorite.toUpperCase() : "NORMALE", 
            "#27ae60"
        );
    }
    
    /**
     * Obtient une couleur pour un statut
     */
    public static String getColorForStatus(String statut) {
        return STATUS_COLORS.getOrDefault(
            statut != null ? statut.toLowerCase() : "nouveau",
            "#3498db"
        );
    }
    
    /**
     * Convertit une couleur hex en JavaFX Color
     */
    public static Color hexToColor(String hex) {
        return Color.web(hex);
    }
    
    /**
     * Obtient une couleur avec opacité
     */
    public static String getColorWithOpacity(String hexColor, double opacity) {
        Color color = Color.web(hexColor);
        return String.format("rgba(%d, %d, %d, %.2f)",
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255),
            opacity
        );
    }
    
    /**
     * Réinitialise le cache (utile lors du rechargement)
     */
    public static void resetCache() {
        courrierColorCache.clear();
        colorIndex = 0;
    }
    
    /**
     * Génère un dégradé entre deux couleurs
     */
    public static String createGradient(String color1, String color2) {
        return String.format("linear-gradient(to right, %s, %s)", color1, color2);
    }
    
    /**
     * Obtient une couleur contrastée pour le texte
     */
    public static String getContrastColor(String hexColor) {
        Color color = Color.web(hexColor);
        double luminance = 0.299 * color.getRed() + 
                          0.587 * color.getGreen() + 
                          0.114 * color.getBlue();
        return luminance > 0.5 ? "#000000" : "#FFFFFF";
    }
    
    /**
     * Assombrit une couleur
     */
    public static String darkenColor(String hexColor, double factor) {
        Color color = Color.web(hexColor);
        return String.format("#%02X%02X%02X",
            (int)(color.getRed() * 255 * (1 - factor)),
            (int)(color.getGreen() * 255 * (1 - factor)),
            (int)(color.getBlue() * 255 * (1 - factor))
        );
    }
    
    /**
     * Éclaircit une couleur
     */
    public static String lightenColor(String hexColor, double factor) {
        Color color = Color.web(hexColor);
        return String.format("#%02X%02X%02X",
            (int)(color.getRed() * 255 + (255 - color.getRed() * 255) * factor),
            (int)(color.getGreen() * 255 + (255 - color.getGreen() * 255) * factor),
            (int)(color.getBlue() * 255 + (255 - color.getBlue() * 255) * factor)
        );
    }
}