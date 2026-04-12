package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long employeId;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private String type; // SUCCESS, ERROR, INFO, WARNING

    private boolean lu = false;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "related_demande_id")
    private Long relatedDemandeId;
}