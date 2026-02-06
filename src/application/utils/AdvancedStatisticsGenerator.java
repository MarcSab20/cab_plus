package application.utils;

import javafx.scene.chart.*;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.geometry.Side;
import application.models.Courrier;
import application.models.WorkflowStep;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Générateur de statistiques avancées et de graphiques pour le workflow
 * Fournit des visualisations professionnelles et interactives
 */
public class AdvancedStatisticsGenerator {
    
    /**
     * Génère un graphique en barres pour la répartition par service
     */
    public static BarChart<String, Number> createServiceDistributionChart(
            Map<String, Integer> serviceData, String title) {
        
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Services");
        yAxis.setLabel("Nombre de courriers");
        
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle(title);
        barChart.setLegendVisible(false);
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        
        // Trier par valeur décroissante
        serviceData.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10) // Top 10
            .forEach(entry -> {
                XYChart.Data<String, Number> data = new XYChart.Data<>(entry.getKey(), entry.getValue());
                series.getData().add(data);
            });
        
        barChart.getData().add(series);
        barChart.setPrefHeight(300);
        
        // Style moderne
        barChart.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        
        return barChart;
    }
    
    /**
     * Génère un graphique en secteurs pour la répartition par type
     */
    public static PieChart createTypeDistributionChart(
            Map<String, Integer> typeData, String title) {
        
        PieChart pieChart = new PieChart();
        pieChart.setTitle(title);
        pieChart.setLegendSide(Side.RIGHT);
        
        typeData.forEach((type, count) -> {
            PieChart.Data slice = new PieChart.Data(
                String.format("%s (%d)", type, count), 
                count
            );
            pieChart.getData().add(slice);
        });
        
        pieChart.setPrefHeight(250);
        pieChart.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        
        return pieChart;
    }
    
    /**
     * Génère un graphique de ligne pour l'évolution temporelle
     */
    public static LineChart<String, Number> createTimelineChart(
            Map<String, Integer> timelineData, String title) {
        
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Période");
        yAxis.setLabel("Nombre de courriers");
        
        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle(title);
        lineChart.setCreateSymbols(true);
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Courriers traités");
        
        timelineData.forEach((period, count) -> 
            series.getData().add(new XYChart.Data<>(period, count))
        );
        
        lineChart.getData().add(series);
        lineChart.setPrefHeight(300);
        lineChart.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        
        return lineChart;
    }
    
    /**
     * Génère un graphique de zones empilées pour les statuts
     */
    public static StackedBarChart<String, Number> createStatusStackedChart(
            Map<String, Map<String, Integer>> statusByService, String title) {
        
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Services");
        yAxis.setLabel("Nombre de courriers");
        
        StackedBarChart<String, Number> stackedChart = new StackedBarChart<>(xAxis, yAxis);
        stackedChart.setTitle(title);
        
        // Créer une série par statut
        Map<String, XYChart.Series<String, Number>> seriesMap = new HashMap<>();
        String[] statuts = {"nouveau", "en_cours", "traite", "archive"};
        
        for (String statut : statuts) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(capitalizeFirst(statut));
            seriesMap.put(statut, series);
        }
        
        // Remplir les données
        statusByService.forEach((service, statusCounts) -> {
            for (String statut : statuts) {
                int count = statusCounts.getOrDefault(statut, 0);
                seriesMap.get(statut).getData().add(
                    new XYChart.Data<>(service, count)
                );
            }
        });
        
        stackedChart.getData().addAll(seriesMap.values());
        stackedChart.setPrefHeight(350);
        stackedChart.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        
        return stackedChart;
    }
    
    /**
     * Génère des cartes de statistiques avancées
     */
    public static VBox createAdvancedStatsCards(
            List<Courrier> courriers, 
            List<WorkflowStep> allSteps) {
        
        VBox container = new VBox(15);
        container.setStyle("-fx-padding: 10;");
        
        // Calculs statistiques
        int totalCourriers = courriers.size();
        long urgents = courriers.stream()
            .filter(c -> "URGENTE".equals(c.getPriorite()) || "TRES_URGENTE".equals(c.getPriorite()))
            .count();
        
        long traites = courriers.stream()
            .filter(c -> "traite".equalsIgnoreCase(c.getStatut()))
            .count();
        
        double tauxTraitement = totalCourriers > 0 ? 
            (traites * 100.0 / totalCourriers) : 0;
        
        // Durée moyenne de traitement
        double dureeMoyenne = calculateAverageDuration(allSteps);
        
        // Créer les cartes
        container.getChildren().addAll(
            createStatCard("📊 Taux de traitement", 
                String.format("%.1f%%", tauxTraitement), 
                tauxTraitement >= 80 ? "#27ae60" : "#e74c3c"),
            
            createStatCard("🚨 Courriers urgents", 
                String.format("%d / %d", urgents, totalCourriers),
                "#e67e22"),
            
            createStatCard("⏱️ Durée moyenne", 
                formatDuration(dureeMoyenne),
                "#3498db"),
            
            createStatCard("📈 Efficacité globale",
                calculateEfficiencyScore(courriers, allSteps),
                "#9b59b6")
        );
        
        return container;
    }
    
    /**
     * Crée une carte de statistique stylisée
     */
    private static VBox createStatCard(String title, String value, String color) {
        VBox card = new VBox(8);
        card.setStyle(String.format(
            "-fx-background-color: %s; " +
            "-fx-padding: 15; " +
            "-fx-background-radius: 10; " +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 2);",
            lightenColor(color, 0.9)
        ));
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: " + color + ";");
        
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        
        card.getChildren().addAll(titleLabel, valueLabel);
        
        return card;
    }
    
    /**
     * Calcule la durée moyenne de traitement
     */
    private static double calculateAverageDuration(List<WorkflowStep> steps) {
        if (steps.isEmpty()) return 0;
        
        Map<Integer, List<WorkflowStep>> byCourrierSteps = steps.stream()
            .collect(Collectors.groupingBy(WorkflowStep::getCourrierId));
        
        List<Long> durations = new ArrayList<>();
        
        byCourrierSteps.forEach((courrierId, courrierSteps) -> {
            if (courrierSteps.size() >= 2) {
                courrierSteps.sort(Comparator.comparing(WorkflowStep::getDateAction));
                WorkflowStep first = courrierSteps.get(0);
                WorkflowStep last = courrierSteps.get(courrierSteps.size() - 1);
                
                long hours = java.time.Duration.between(
                    first.getDateAction(), 
                    last.getDateAction()
                ).toHours();
                
                durations.add(hours);
            }
        });
        
        return durations.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0);
    }
    
    /**
     * Calcule un score d'efficacité global
     */
    private static String calculateEfficiencyScore(
            List<Courrier> courriers, 
            List<WorkflowStep> steps) {
        
        if (courriers.isEmpty()) return "N/A";
        
        double score = 100.0;
        
        // Pénalités
        long nouveaux = courriers.stream()
            .filter(c -> "nouveau".equalsIgnoreCase(c.getStatut()))
            .count();
        
        long enCours = courriers.stream()
            .filter(c -> "en_cours".equalsIgnoreCase(c.getStatut()))
            .count();
        
        double ratio = (double) (nouveaux + enCours) / courriers.size();
        score -= ratio * 30; // -30 points max pour les non-traités
        
        // Bonus pour rapidité
        double avgDuration = calculateAverageDuration(steps);
        if (avgDuration < 24) {
            score += 10; // Bonus si traitement < 24h
        } else if (avgDuration > 72) {
            score -= 15; // Pénalité si > 72h
        }
        
        score = Math.max(0, Math.min(100, score));
        
        return String.format("%.0f/100", score);
    }
    
    /**
     * Formate une durée en heures
     */
    private static String formatDuration(double heures) {
        if (heures < 1) {
            return String.format("%.0f min", heures * 60);
        } else if (heures < 24) {
            return String.format("%.1f h", heures);
        } else {
            return String.format("%.1f j", heures / 24);
        }
    }
    
    /**
     * Capitalise la première lettre
     */
    private static String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
    
    /**
     * Éclaircit une couleur (version simplifiée)
     */
    private static String lightenColor(String color, double factor) {
        // Implémentation simplifiée
        return color + "33"; // Ajoute transparence
    }
    
    /**
     * Génère un rapport de performance par service
     */
    public static String generatePerformanceReport(
            Map<String, Integer> serviceStats, 
            Map<String, Double> durations) {
        
        StringBuilder report = new StringBuilder();
        report.append("=== RAPPORT DE PERFORMANCE ===\n\n");
        
        serviceStats.forEach((service, count) -> {
            double avgDuration = durations.getOrDefault(service, 0.0);
            String performance = avgDuration < 24 ? "🟢 Excellent" :
                               avgDuration < 48 ? "🟡 Satisfaisant" :
                               "🔴 À améliorer";
            
            report.append(String.format("📊 %s:\n", service));
            report.append(String.format("   Courriers: %d\n", count));
            report.append(String.format("   Durée moy: %.1fh\n", avgDuration));
            report.append(String.format("   Performance: %s\n\n", performance));
        });
        
        return report.toString();
    }
}