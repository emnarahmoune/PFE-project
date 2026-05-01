import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormationService } from '../../../../../core/services/formation.service';
import { Location, CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

// 🔥 ANGULAR MATERIAL
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatDividerModule } from '@angular/material/divider';

@Component({
  selector: 'app-formation-detail',
  templateUrl: './formation-detail.component.html',
  styleUrls: ['./formation-detail.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatIconModule,
    MatButtonModule,
    MatCardModule,
    MatChipsModule,
    MatProgressBarModule,
    MatTableModule,
    MatDividerModule
  ]
})
export class FormationDetailComponent implements OnInit {

  formation: any = null;
  participants: any[] = [];

  displayedColumns: string[] = ['nom','poste','departement','statut','actions'];

  constructor(
    private route: ActivatedRoute,
    private formationService: FormationService,
    private router: Router,
    private location: Location
  ) {}

  // =========================
  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadFormation(+id);
    }
  }

  // =========================
  // 🔥 VERSION ROBUSTE
  loadFormation(id: number) {
    this.formationService.getById(id).subscribe({
      next: (res: any) => {

        console.log("===== API DEBUG =====");
        console.log("FULL RESPONSE:", res);
        console.log("EMPLOYES:", res?.employes);

        this.formation = res || {};

        // 🔥 FIX ULTRA IMPORTANT
        if (res && Array.isArray(res.employes)) {
          this.participants = res.employes;
        } else {
          console.warn("⚠️ employes absent ou invalide");
          this.participants = [];
        }

        console.log("PARTICIPANTS FINAL:", this.participants);
      },

      error: (err) => {
        console.error("❌ API ERROR:", err);
        this.participants = [];
      }
    });
  }

  // =========================
  goBack() {
    this.location.back();
  }

  goToEdit() {
    if (!this.formation?.id) return;
    this.router.navigate(['/admin/formations', this.formation.id, 'edit']);
  }

  deleteFormation() {
    if (!this.formation?.id) return;

    if (!confirm('Supprimer cette formation ?')) return;

    this.formationService.delete(this.formation.id)
      .subscribe(() => this.router.navigate(['/admin/formations']));
  }

  toggleStatut() {
    if (!this.formation?.id) return;

    const call = this.formation.actif
      ? this.formationService.desactiver(this.formation.id)
      : this.formationService.activer(this.formation.id);

    call.subscribe(() => this.loadFormation(this.formation.id));
  }

  retirerParticipant(p: any) {
    if (!this.formation?.id || !p?.id) return;

    this.formationService.retirerParticipant(this.formation.id, p.id)
      .subscribe(() => this.loadFormation(this.formation.id));
  }

  // =========================
  // UI HELPERS
  // =========================

  getDomaineColor(domaine: string): string {
    const map: any = {
      TECHNIQUE: 'primary',
      SOFT_SKILLS: 'accent',
      MANAGEMENT: 'warn',
      LANGUES: 'primary',
      SECURITE: 'warn'
    };
    return map[domaine] || 'primary';
  }

  getStatutColor(statut: string): string {
    return statut === 'TERMINE' ? 'primary' : 'accent';
  }

  getCompletionRate(): number {
    if (!this.participants.length) return 0;
    return Math.min((this.participants.length / 10) * 100, 100);
  }

  getAvgSatisfaction(): number {
    return 4;
  }

  goToParticipants() {
    if (!this.formation?.id) return;
    this.router.navigate(['/admin/formations', this.formation.id, 'participants']);
  }
}