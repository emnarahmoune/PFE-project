import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../../core/services/api.service';

@Component({
  selector: 'app-competences',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './competences.component.html',
  styleUrls: ['./competences.component.css']
})
export class CompetencesComponent implements OnInit {

  competences: any[] = [];
  competencesDisponibles: any[] = [];
showConfirm = false;
selectedId: number | null = null;
selectedName: string = '';
  newCompetenceId: number | null = null;
  newLevel: number = 1;

  loading = false;
  errorMsg = '';

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.loadUserCompetences();
    this.loadAllCompetences();
  }

  confirmDelete() {

  if (!this.selectedId) return;

  this.api.delete('employes/me/competences', this.selectedId).subscribe({
    next: () => {
      this.loadUserCompetences();
      this.showConfirm = false;
    },
    error: () => alert("Erreur suppression")
  });
}


cancelDelete() {
  this.showConfirm = false;
}
  // 🔥 LOAD USER
loadUserCompetences() {
  this.loading = true;

  this.api.get('employes/me/competences').subscribe({
    next: (data: any) => {

      const list = Array.isArray(data) ? data : data.data || [];

      // ✅ FIX ICI
      this.competences = list.map((c: any) => ({
         ...c,
          niveau: Number(c.niveau)   // 🔥 conversion propre
      }));

      this.loading = false;
    },
    error: (err) => {
      console.error(err);
      this.errorMsg = "Erreur chargement compétences";
      this.loading = false;
    }
  });
}

  // 🔥 LOAD ALL
  loadAllCompetences() {
    this.api.get('competences').subscribe({
      next: (data: any) => {

        console.log("DATA API:", data);

        if (Array.isArray(data)) {
          this.competencesDisponibles = data;
        } else if (data.data) {
          this.competencesDisponibles = data.data;
        } else {
          this.competencesDisponibles = [];
        }

      },
      error: (err) => {
        console.error("Erreur API compétences", err);
      }
    });
  }

  // 🔥 ADD
  addCompetence() {

    if (!this.newCompetenceId) return;

    const exists = this.competences.some(c => c.competenceId == this.newCompetenceId);

    if (exists) {
      alert("⚠️ Compétence déjà ajoutée !");
      return;
    }

    const payload = {
      competenceId: this.newCompetenceId,
      niveau: this.newLevel
    };

    this.api.post('employes/me/competences', payload).subscribe({
      next: () => {

        alert("✔ Compétence ajoutée");

        this.loadUserCompetences();

        this.newCompetenceId = null;
        this.newLevel = 1;
      },
      error: (err) => {
        console.error("ERROR:", err);

        if (err.status === 200) {
          this.loadUserCompetences();
          return;
        }

        alert("Erreur ajout compétence");
      }
    });
  }

  // 🔥 DELETE (CORRIGÉ)
 deleteCompetence(id: number, nom: string) {

  // 🔥 ouvre modal custom
  this.selectedId = id;
  this.selectedName = nom;
  this.showConfirm = true;
}

  // 🔥 SAVE
  save() {

    const payload = this.competences.map(c => ({
      competenceId: c.competenceId,
      niveau: c.niveau
    }));

    this.api.putCustom('employes/me/competences', payload).subscribe({
      next: () => alert("✔ Compétences mises à jour"),
      error: () => alert("Erreur mise à jour")
    });
  }

  // 🔥 LABEL
  getLevelLabel(level: number) {
    switch (level) {
      case 1: return "Débutant";
      case 2: return "Intermédiaire";
      case 3: return "Avancé";
      case 4: return "Expert";
      default: return "";
    }
  }
}