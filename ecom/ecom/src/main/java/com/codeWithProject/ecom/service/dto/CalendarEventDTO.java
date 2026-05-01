package com.codeWithProject.ecom.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CalendarEventDTO {
    private Long id;
    private String title;
    private LocalDateTime start;
    private LocalDateTime end;
    private String color;
    private ExtendedProps extendedProps;

    // Constructeur pour JPQL (avec LocalDate -> conversion en début de journée)
    public CalendarEventDTO(Long id, String title, LocalDate start, LocalDate end,
                            String color, String statut, String type, String employeNom, String employePrenom) {
        this.id = id;
        this.title = title;
        this.start = start != null ? start.atStartOfDay() : null;
        this.end = end != null ? end.atStartOfDay() : null;
        this.color = color;
        this.extendedProps = new ExtendedProps(statut, type, employeNom, employePrenom);
    }

    // Constructeur alternatif si on a directement LocalDateTime (pour d'autres usages)
    public CalendarEventDTO(Long id, String title, LocalDateTime start, LocalDateTime end,
                            String color, String statut, String type, String employeNom, String employePrenom) {
        this.id = id;
        this.title = title;
        this.start = start;
        this.end = end;
        this.color = color;
        this.extendedProps = new ExtendedProps(statut, type, employeNom, employePrenom);
    }

    // Getters et setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getStart() { return start; }
    public void setStart(LocalDateTime start) { this.start = start; }
    public LocalDateTime getEnd() { return end; }
    public void setEnd(LocalDateTime end) { this.end = end; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public ExtendedProps getExtendedProps() { return extendedProps; }
    public void setExtendedProps(ExtendedProps extendedProps) { this.extendedProps = extendedProps; }

    public static class ExtendedProps {
        public String statut;
        public String type;
        public String employeNom;
        public String employePrenom;

        public ExtendedProps(String statut, String type, String employeNom, String employePrenom) {
            this.statut = statut;
            this.type = type;
            this.employeNom = employeNom;
            this.employePrenom = employePrenom;
        }
    }
}