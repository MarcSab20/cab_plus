package application.utils;

import javafx.scene.chart.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.control.Label;
import javafx.geometry.Side;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import application.models.*;
import application.services.*;

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
     * Génère les statistiques globales
     */
    public VBox generateGlobalStats(List<Courrier> courriers, LocalDateTime dateDebut, LocalDateTime dateFin) {
        VBox container = new VBox(15);
        container.setPadding(new Insets(15));
        container.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        
        // Titre
        Label titre = new Label("📊 STATISTIQUES GLOBALES");
        titre.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2c3e50;");
        
        // Période
        Label periode = new Label(String.format("Période: %s au %s",
            dateDebut.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
            dateFin.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        periode.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
        
        // Grille de statistiques
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(15);
        grid.setPadding(new Insets(15, 0, 0, 0));
        
        int row = 0;
        
        // Total courriers
        grid.add(createStatLabel("📧 Total courriers:"), 0, row);
        grid.add(createValueLabel(String.valueOf(courriers.size())), 1, row++);
        
        // Par type
        Map<String, Long> parType = courriers.stream()
            .collect(Collectors.groupingBy(
                c -> c.getTypeCourrier() != null ? c.getTypeCourrier() : "INCONNU",
                Collectors.counting()
            ));
        
        grid.add(createStatLabel("📥 Entrants:"), 0, row);
        grid.add(createValueLabel(String.valueOf(parType.getOrDefault("ENTRANT", 0L))), 1, row++);
        
        grid.add(createStatLabel("📤 Sortants:"), 0, row);
        grid.add(createValueLabel(String.valueOf(parType.getOrDefault("SORTANT", 0L))), 1, row++);
        
        grid.add(createStatLabel("🔄 Internes:"), 0, row);
        grid.add(createValueLabel(String.valueOf(parType.getOrDefault("INTERNE", 0L))), 1, row++);
        
        // Par statut
        Map<String, Long> parStatut = courriers.stream()
            .collect(Collectors.groupingBy(
                c -> c.getStatut() != null ? c.getStatut() : "inconnu",
                Collectors.counting()
            ));
        
        grid.add(createStatLabel("🆕 Nouveaux:"), 0, row);
        grid.add(createValueLabel(String.valueOf(parStatut.getOrDefault("nouveau", 0L))), 1, row++);
        
        grid.add(createStatLabel("⏳ En cours:"), 0, row);
        grid.add(createValueLabel(String.valueOf(parStatut.getOrDefault("en_cours", 0L))), 1, row++);
        
        grid.add(createStatLabel("✅ Traités:"), 0, row);
        grid.add(createValueLabel(String.valueOf(parStatut.getOrDefault("traite", 0L))), 1, row++);
        
        grid.add(createStatLabel("📦 Archivés:"), 0, row);
        grid.add(createValueLabel(String.valueOf(parStatut.getOrDefault("archive", 0L))), 1, row++);
        
        // Graphique de répartition par type
        PieChart typeChart = createTypeDistributionChart(
            parType.entrySet().stream()
                .collect(Collectors.toMap(
                    e -> getTypeLibelle(e.getKey()),
                    e -> e.getValue().intValue()
                )),
            "Répartition par type"
        );
        typeChart.setPrefHeight(200);
        
        container.getChildren().addAll(titre, periode, grid, typeChart);
        
        return container;
    }
    
    /**
     * Génère les statistiques par service
     */
    public VBox generateServiceStats(List<Courrier> courriers, 
                                     List<ServiceHierarchy> servicesAutorises,
                                     WorkflowAnalysisService workflowService,
                                     CotationService cotationService) {
        VBox container = new VBox(15);
        container.setPadding(new Insets(15));
        container.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        
        Label titre = new Label("🏢 STATISTIQUES PAR SERVICE");
        titre.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2c3e50;");
        
        // Calculer stats par service
        Map<String, Integer> courriersByService = new HashMap<>();
        
        for (Courrier courrier : courriers) {
            List<CotationCourrier> cotations = cotationService.getCotationsByCourrier(courrier.getId());
            for (CotationCourrier cot : cotations) {
                String serviceCode = cot.getServiceDestination();
                if (serviceCode != null) {
                    courriersByService.merge(serviceCode, 1, Integer::sum);
                }
            }
        }
        
        // Créer les cartes pour chaque service
        VBox servicesBox = new VBox(10);
        
        for (ServiceHierarchy service : servicesAutorises) {
            int count = courriersByService.getOrDefault(service.getServiceCode(), 0);
            if (count > 0) {
                HBox serviceCard = createServiceStatCard(service, count);
                servicesBox.getChildren().add(serviceCard);
            }
        }
        
        // Graphique en barres
        if (!courriersByService.isEmpty()) {
            Map<String, Integer> topServices = courriersByService.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                    e -> {
                        ServiceHierarchy s = workflowService.getServiceByCode(e.getKey());
                        return s != null ? s.getServiceName() : e.getKey();
                    },
                    Map.Entry::getValue,
                    (a, b) -> a,
                    LinkedHashMap::new
                ));
            
            BarChart<String, Number> chart = createServiceDistributionChart(
                topServices,
                "Top 10 des services"
            );
            chart.setPrefHeight(250);
            container.getChildren().add(chart);
        }
        
        container.getChildren().addAll(titre, servicesBox);
        
        return container;
    }
    
    /**
     * Génère les statistiques temporelles
     */
    public VBox generateTemporalStats(List<Courrier> courriers, 
                                      LocalDateTime dateDebut, 
                                      LocalDateTime dateFin) {
        VBox container = new VBox(15);
        container.setPadding(new Insets(15));
        container.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        
        Label titre = new Label("📈 ÉVOLUTION TEMPORELLE");
        titre.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2c3e50;");
        
        // Grouper par jour
        Map<String, Integer> parJour = courriers.stream()
            .filter(c -> c.getDateCreation() != null)
            .collect(Collectors.groupingBy(
                c -> c.getDateCreation().format(DateTimeFormatter.ofPattern("dd/MM")),
                LinkedHashMap::new,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
        
        if (!parJour.isEmpty()) {
            LineChart<String, Number> dailyChart = createTimelineChart(
                parJour,
                "Courriers par jour"
            );
            dailyChart.setPrefHeight(200);
            container.getChildren().add(dailyChart);
        }
        
        // Grouper par mois si période > 60 jours
        long daysBetween = java.time.Duration.between(dateDebut, dateFin).toDays();
        if (daysBetween > 60) {
            Map<String, Integer> parMois = courriers.stream()
                .filter(c -> c.getDateCreation() != null)
                .collect(Collectors.groupingBy(
                    c -> c.getDateCreation().format(DateTimeFormatter.ofPattern("MM/yyyy")),
                    LinkedHashMap::new,
                    Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
            
            if (!parMois.isEmpty()) {
                LineChart<String, Number> monthlyChart = createTimelineChart(
                    parMois,
                    "Courriers par mois"
                );
                monthlyChart.setPrefHeight(200);
                container.getChildren().add(monthlyChart);
            }
        }
        
        container.getChildren().add(0, titre);
        
        return container;
    }
    
    // ═══════════════════════════════════════════════════════════════
    // MÉTHODES PRIVÉES D'AIDE
    // ═══════════════════════════════════════════════════════════════
    
    private Label createStatLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #34495e; -fx-font-size: 12px;");
        return label;
    }
    
    private Label createValueLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #3498db;");
        return label;
    }
    
    private String getTypeLibelle(String type) {
        return switch (type.toUpperCase()) {
            case "ENTRANT" -> "📥 Entrant";
            case "SORTANT" -> "📤 Sortant";
            case "INTERNE" -> "🔄 Interne";
            default -> type;
        };
    }
    
    private HBox createServiceStatCard(ServiceHierarchy service, int count) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10));
        card.setStyle(
            "-fx-background-color: #f8f9fa;" +
            "-fx-border-color: " + service.getCouleur() + ";" +
            "-fx-border-width: 0 0 0 4;" +
            "-fx-background-radius: 5;"
        );
        
        Label icon = new Label(service.getIcone());
        icon.setStyle("-fx-font-size: 20px;");
        
        VBox info = new VBox(3);
        Label name = new Label(service.getServiceName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        
        Label code = new Label(service.getServiceCode());
        code.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 10px;");
        
        info.getChildren().addAll(name, code);
        HBox.setHgrow(info, Priority.ALWAYS);
        
        Label countLabel = new Label(String.valueOf(count));
        countLabel.setStyle(
            "-fx-font-size: 20px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: " + service.getCouleur() + ";"
        );
        
        card.getChildren().addAll(icon, info, countLabel);
        
        return card;
    }
    
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
        
        serviceData.forEach((service, count) -> {
            XYChart.Data<String, Number> data = new XYChart.Data<>(service, count);
            series.getData().add(data);
        });
        
        barChart.getData().add(series);
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
        series.setName("Courriers");
        
        timelineData.forEach((period, count) -> 
            series.getData().add(new XYChart.Data<>(period, count))
        );
        
        lineChart.getData().add(series);
        lineChart.setStyle("-fx-background-color: white; -fx-background-radius: 10;");
        
        return lineChart;
    }
}