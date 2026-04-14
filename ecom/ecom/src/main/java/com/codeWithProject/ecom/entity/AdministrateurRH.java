package com.codeWithProject.ecom.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDate;

@Entity
@Table(name = "administrateurs_rh")
@PrimaryKeyJoinColumn(name = "employe_id")
@DiscriminatorValue("ADMIN_RH")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor  // ← constructeur public (nécessaire pour new AdministrateurRH())
public class AdministrateurRH extends Employe {

    // ===== MÉTHODES STATIQUES DE CRÉATION =====
    public static AdministrateurRH fromEmploye(Employe employe, String password) {
        if (employe == null) {
            throw new IllegalArgumentException("L'employé ne peut pas être null");
        }

        AdministrateurRH admin = new AdministrateurRH();
        admin.setMatricule(employe.getMatricule());
        admin.setNom(employe.getNom());
        admin.setPrenom(employe.getPrenom());
        admin.setEmail(employe.getEmail());
        admin.setTelephone(employe.getTelephone());
        admin.setPassword(password);
        admin.setDateEmbauche(employe.getDateEmbauche());
        admin.setPoste(employe.getPoste());
        admin.setSalaire(employe.getSalaire());
        admin.setStatut(employe.getStatut());
        admin.setDepartement(employe.getDepartement());
        admin.setSoldeConges(employe.getSoldeConges());
        admin.setActif(true);
        admin.setCompteVerrouille(false);
        admin.setNombreConnexions(0);
        admin.setTentativesEchec(0);
        admin.setRole("ADMIN_RH");
        admin.setTypeEmploye(Employe.TYPE_ADMIN_RH);

        return admin;
    }

    public static AdministrateurRH create(String matricule, String nom, String prenom,
                                          String email, String password, LocalDate dateEmbauche) {
        AdministrateurRH admin = new AdministrateurRH();
        admin.setMatricule(matricule);
        admin.setNom(nom);
        admin.setPrenom(prenom);
        admin.setEmail(email);
        admin.setPassword(password);
        admin.setDateEmbauche(dateEmbauche);
        admin.setSalaire(0.0);
        admin.setStatut("ACTIF");
        admin.setSoldeConges(25);
        admin.setActif(true);
        admin.setCompteVerrouille(false);
        admin.setNombreConnexions(0);
        admin.setTentativesEchec(0);
        admin.setRole("ADMIN_RH");
        admin.setTypeEmploye(Employe.TYPE_ADMIN_RH);

        return admin;
    }

    // ===== MÉTHODES MÉTIER =====
    // (conservez toutes vos méthodes métier inchangées)
    public void creerEmploye(Employe employe) {
        if (employe == null) throw new IllegalArgumentException("L'employé ne peut pas être null");
    }

    public void modifierEmploye(Employe employe) {
        if (employe == null) throw new IllegalArgumentException("L'employé ne peut pas être null");
    }

    public void supprimerEmploye(Employe employe) {
        if (employe == null) throw new IllegalArgumentException("L'employé ne peut pas être null");
    }

    public void consulterEmploye(Employe employe) {
        if (employe == null) throw new IllegalArgumentException("L'employé ne peut pas être null");
        if (!Boolean.TRUE.equals(this.getActif()))
            throw new IllegalStateException("Administrateur inactif - Impossible de consulter les employés");
    }

    public void creerCompetence(Competence competence) {
        if (!Boolean.TRUE.equals(this.getActif()))
            throw new IllegalStateException("Administrateur inactif - Impossible de créer une compétence");
    }

    public void modifierCompetence(Competence competence) {
        if (!Boolean.TRUE.equals(this.getActif()))
            throw new IllegalStateException("Administrateur inactif - Impossible de modifier une compétence");
    }

    public void supprimerCompetence(Competence competence) {
        if (!Boolean.TRUE.equals(this.getActif()))
            throw new IllegalStateException("Administrateur inactif - Impossible de supprimer une compétence");
    }

    public void associerCompetenceEmploye(Employe employe, Competence competence, String niveau) {
        if (!Boolean.TRUE.equals(this.getActif()))
            throw new IllegalStateException("Administrateur inactif - Impossible d'associer une compétence");
    }

    public void creerFormation(Formation formation) {
        if (!Boolean.TRUE.equals(this.getActif()))
            throw new IllegalStateException("Administrateur inactif - Impossible de créer une formation");
    }

    public void modifierFormation(Formation formation) {
        if (!Boolean.TRUE.equals(this.getActif()))
            throw new IllegalStateException("Administrateur inactif - Impossible de modifier une formation");
    }

    public void supprimerFormation(Formation formation) {
        if (!Boolean.TRUE.equals(this.getActif()))
            throw new IllegalStateException("Administrateur inactif - Impossible de supprimer une formation");
    }

    public void inscrireEmployeFormation(Employe employe, Formation formation) {
        if (!Boolean.TRUE.equals(this.getActif()))
            throw new IllegalStateException("Administrateur inactif - Impossible d'inscrire à une formation");
    }

    public void consulterDashboardsRH() {
        if (!Boolean.TRUE.equals(this.getActif()))
            throw new IllegalStateException("Administrateur inactif - Impossible de consulter les tableaux de bord");
    }

    public void analyserTurnover() {
        if (!Boolean.TRUE.equals(this.getActif()))
            throw new IllegalStateException("Administrateur inactif - Impossible d'analyser le turnover");
    }

    public void analyserAbsenteisme() {
        if (!Boolean.TRUE.equals(this.getActif()))
            throw new IllegalStateException("Administrateur inactif - Impossible d'analyser l'absentéisme");
    }

    public void consulterScoresRisque() {
        if (!Boolean.TRUE.equals(this.getActif()))
            throw new IllegalStateException("Administrateur inactif - Impossible de consulter les scores de risque");
    }

    public void genererRapportEmployes() {
        if (!Boolean.TRUE.equals(this.getActif()))
            throw new IllegalStateException("Administrateur inactif - Impossible de générer un rapport");
    }

    public void genererRapportFormations() {
        if (!Boolean.TRUE.equals(this.getActif()))
            throw new IllegalStateException("Administrateur inactif - Impossible de générer un rapport");
    }

    public void exporterDonneesRH() {
        if (!Boolean.TRUE.equals(this.getActif()))
            throw new IllegalStateException("Administrateur inactif - Impossible d'exporter les données");
    }

    // ===== MÉTHODES UTILITAIRES =====
    @Transient
    public boolean isActif() {
        return Boolean.TRUE.equals(this.getActif());
    }

    @Transient
    public boolean peutEffectuerOperations() {
        return isActif() && !Boolean.TRUE.equals(this.getCompteVerrouille());
    }

    @Transient
    @Override
    public String getNomComplet() {
        return super.getNomComplet() + " (Administrateur RH)";
    }

    @Override
    public String toString() {
        return String.format("AdministrateurRH{id=%d, matricule='%s', nom='%s', prenom='%s', email='%s', actif=%s}",
                this.getId(), this.getMatricule(), this.getNom(), this.getPrenom(),
                this.getEmail(), this.getActif());
    }

    @PrePersist
    @PreUpdate
    protected void onPrePersistOrUpdate() {
        super.onPrePersistOrUpdate();

        if (this.getRole() == null) {
            this.setRole("ADMIN_RH");
        }
        if (this.getTypeEmploye() == null) {
            this.setTypeEmploye(Employe.TYPE_ADMIN_RH);
        }
        if (this.getPassword() == null || this.getPassword().isEmpty()) {
            throw new IllegalStateException("Le mot de passe de l'administrateur est obligatoire");
        }
    }
}