package com.codeWithProject.ecom.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "parametres")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Parametre {

    @Id
    private String cle;

    @Column(nullable = false)
    private String valeur;

    private String description;
}