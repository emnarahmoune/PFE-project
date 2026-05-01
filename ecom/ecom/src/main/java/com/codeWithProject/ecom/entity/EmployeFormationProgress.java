package com.codeWithProject.ecom.entity;
import jakarta.persistence.*;

@Entity
public class EmployeFormationProgress {
    
    
    @Id
     @GeneratedValue(strategy = GenerationType.IDENTITY)
     private Long id;

    private Long employeId;

    @ManyToOne
    private FormationVideo video;

    private boolean completed;

@ManyToOne
@JoinColumn(name = "formation_id")
private Formation formation;

    // GETTERS
    public Long getId() { return id; }
    public Long getEmployeId() { return employeId; }
    public FormationVideo getVideo() { return video; }
    public boolean isCompleted() { return completed; }

    // SETTERS 🔥🔥🔥
    public void setEmployeId(Long employeId) { this.employeId = employeId; }
    public void setVideo(FormationVideo video) { this.video = video; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}