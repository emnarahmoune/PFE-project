package com.codeWithProject.ecom.entity;
import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDate;

@Entity
@Table(name = "administrateurs_rh")
@DiscriminatorValue("ADMIN_RH") // ✅ important pour SINGLE_TABLE
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class AdministrateurRH extends Employe {

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

        admin.setSalaire(BigDecimal.ZERO);
        admin.setStatut("ACTIF");
        admin.setSoldeConges(25);

        admin.setActif(true);
        admin.setCompteVerrouille(false);
        admin.setNombreConnexions(0);
        admin.setTentativesEchec(0);

        admin.setRole("ADMIN_RH");

        return admin;
    }



    @Transient
public boolean peutSeConnecter() {
    return Boolean.TRUE.equals(this.getActif())
            && !Boolean.TRUE.equals(this.getCompteVerrouille());
}
    @Transient
    public boolean isActif() {
        return Boolean.TRUE.equals(this.getActif());
    }

    @Transient
    @Override
    public String getNomComplet() {
        return super.getNomComplet() + " (Administrateur RH)";
    }

    @Override
    public String toString() {
        return String.format("AdministrateurRH{id=%d, nom='%s'}",
                this.getId(), this.getNomComplet());
    }

    @PrePersist
    @PreUpdate
    protected void onPrePersistOrUpdate() {
        super.onPrePersistOrUpdate();

        if (this.getRole() == null) {
            this.setRole("ADMIN_RH");
        }

        if (this.getPassword() == null || this.getPassword().isEmpty()) {
            throw new IllegalStateException("Mot de passe obligatoire");
        }
    }
}