package com.codeWithProject.ecom.service.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CalendarEventDTO {

    private Long id;
    private String title;
    private LocalDateTime start;
    private LocalDateTime end;
    private String color;
    private ExtendedProps extendedProps;

    // ✅ Constructeur vide obligatoire pour new CalendarEventDTO()
    public CalendarEventDTO() {
    }

    // Constructeur pour JPQL avec LocalDate
    public CalendarEventDTO(Long id, String title, LocalDate start, LocalDate end,
                            String color, String statut, String type,
                            String employeNom, String employePrenom) {
        this.id = id;
        this.title = title;
        this.start = start != null ? start.atStartOfDay() : null;
        this.end = end != null ? end.atStartOfDay() : null;
        this.color = color;
        this.extendedProps = new ExtendedProps(statut, type, employeNom, employePrenom);
    }

    // Constructeur pour JPQL avec LocalDateTime
    public CalendarEventDTO(Long id, String title, LocalDateTime start, LocalDateTime end,
                            String color, String statut, String type,
                            String employeNom, String employePrenom) {
        this.id = id;
        this.title = title;
        this.start = start;
        this.end = end;
        this.color = color;
        this.extendedProps = new ExtendedProps(statut, type, employeNom, employePrenom);
    }

    // Getters / Setters principaux
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public void setEnd(LocalDateTime end) {
        this.end = end;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public ExtendedProps getExtendedProps() {
        return extendedProps;
    }

    public void setExtendedProps(ExtendedProps extendedProps) {
        this.extendedProps = extendedProps;
    }

    public static class ExtendedProps {

        private Long demandeId;
        private String statut;
        private String type;
        private String employeNom;
        private String employePrenom;
        private String employeEmail;

        // ✅ Constructeur vide obligatoire pour new CalendarEventDTO.ExtendedProps()
        public ExtendedProps() {
        }

        // ✅ Ancien constructeur conservé pour ne pas casser ton JPQL existant
        public ExtendedProps(String statut, String type, String employeNom, String employePrenom) {
            this.statut = statut;
            this.type = type;
            this.employeNom = employeNom;
            this.employePrenom = employePrenom;
        }

        // ✅ Nouveau constructeur optionnel complet
        public ExtendedProps(Long demandeId, String statut, String type,
                             String employeNom, String employePrenom, String employeEmail) {
            this.demandeId = demandeId;
            this.statut = statut;
            this.type = type;
            this.employeNom = employeNom;
            this.employePrenom = employePrenom;
            this.employeEmail = employeEmail;
        }

        public Long getDemandeId() {
            return demandeId;
        }

        public void setDemandeId(Long demandeId) {
            this.demandeId = demandeId;
        }

        public String getStatut() {
            return statut;
        }

        public void setStatut(String statut) {
            this.statut = statut;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getEmployeNom() {
            return employeNom;
        }

        public void setEmployeNom(String employeNom) {
            this.employeNom = employeNom;
        }

        public String getEmployePrenom() {
            return employePrenom;
        }

        public void setEmployePrenom(String employePrenom) {
            this.employePrenom = employePrenom;
        }

        public String getEmployeEmail() {
            return employeEmail;
        }

        public void setEmployeEmail(String employeEmail) {
            this.employeEmail = employeEmail;
        }
    }
}