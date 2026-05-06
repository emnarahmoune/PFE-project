import { Component, OnInit } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';

import { EmployeeCongeService } from '../../../services/employee-conge.service';
import { DemandeConge, CongeResponse } from '../../../models/conge.model';
import { AuthService } from '../../../../../core/services/auth.service';

@Component({
  selector: 'app-detail-conge',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './detail-conge.component.html',
  styleUrls: ['./detail-conge.component.css']
})
export class DetailCongeComponent implements OnInit {

  demande: DemandeConge | null = null;

  loading = true;
  error: string | null = null;

  isAdmin = false;
  isEmploye = false;
  canAnnuler = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private congeService: EmployeeCongeService,
    private authService: AuthService,
    private location: Location
  ) {}

  ngOnInit(): void {
    this.checkUserRole();
    this.loadDemande();
  }

  private checkUserRole(): void {
    const role = this.authService.getUserRole();

    this.isAdmin = role === 'ADMIN_RH' || role === 'ADMIN';
    this.isEmploye = role === 'EMPLOYE' || role === 'EMPLOYEE';
  }

  private loadDemande(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = Number(idParam);

    if (!idParam || Number.isNaN(id)) {
      this.loading = false;
      this.error = 'ID de demande invalide.';
      return;
    }

    this.loading = true;
    this.error = null;

    if (this.isAdmin) {
      this.loadDemandeForAdmin(id);
    } else {
      this.loadDemandeForEmploye(id);
    }
  }

  private loadDemandeForAdmin(id: number): void {
    this.congeService.getAllDemandesAdmin().subscribe({
      next: (response: CongeResponse) => {
        const demandes = Array.isArray(response?.data)
          ? response.data as DemandeConge[]
          : [];

        const demande = demandes.find((d: DemandeConge) => Number(d.id) === id);

        if (demande) {
          this.demande = demande;
          this.checkCanAnnuler();
        } else {
          this.error = 'Demande non trouvée.';
        }

        this.loading = false;
      },
      error: (err: any) => {
        console.error('Erreur chargement demande admin:', err);
        this.error = 'Erreur lors du chargement de la demande.';
        this.loading = false;
      }
    });
  }

  private loadDemandeForEmploye(id: number): void {
    this.congeService.getMesConges().subscribe({
      next: (response: CongeResponse) => {
        const demandes = Array.isArray(response?.data)
          ? response.data as DemandeConge[]
          : [];

        const demande = demandes.find((d: DemandeConge) => Number(d.id) === id);

        if (demande) {
          this.demande = demande;
          this.checkCanAnnuler();
        } else {
          this.error = 'Demande non trouvée.';
        }

        this.loading = false;
      },
      error: (err: any) => {
        console.error('Erreur chargement demande employé:', err);
        this.error = 'Erreur lors du chargement de la demande.';
        this.loading = false;
      }
    });
  }

  private checkCanAnnuler(): void {
    this.canAnnuler =
      !!this.demande &&
      this.isEmploye &&
      this.demande.statut === 'EN_ATTENTE';
  }

  annulerDemande(): void {
    if (!this.demande?.id) return;

    const confirmed = confirm('Êtes-vous sûr de vouloir annuler cette demande de congé ?');

    if (!confirmed) return;

    this.loading = true;
    this.error = null;

    this.congeService.annulerConge(this.demande.id).subscribe({
      next: () => {
        if (this.demande) {
          this.demande.statut = 'ANNULE';
        }

        this.canAnnuler = false;
        this.loading = false;

        alert('Demande annulée avec succès.');
      },
      error: (err: any) => {
        console.error('Erreur annulation:', err);

        this.error =
          err?.error?.error ||
          err?.error?.message ||
          'Erreur lors de l’annulation de la demande.';

        this.loading = false;
      }
    });
  }

  goBack(): void {
    if (window.history.length > 1) {
      this.location.back();
      return;
    }

    if (this.isAdmin) {
      this.router.navigate(['/admin/conges']);
    } else {
      this.router.navigate(['/employee/mes-conges']);
    }
  }

  getStatutClass(): string {
    if (!this.demande?.statut) return 'status-annule';

    switch (this.demande.statut) {
      case 'EN_ATTENTE':
        return 'status-en_attente';

      case 'APPROUVE':
        return 'status-approuve';

      case 'REFUSE':
        return 'status-refuse';

      case 'ANNULE':
        return 'status-annule';

      default:
        return 'status-annule';
    }
  }

  getStatutLabel(): string {
    if (!this.demande?.statut) return 'Non défini';

    switch (this.demande.statut) {
      case 'EN_ATTENTE':
        return 'En attente';

      case 'APPROUVE':
        return 'Approuvé';

      case 'REFUSE':
        return 'Refusé';

      case 'ANNULE':
        return 'Annulé';

      default:
        return this.demande.statut;
    }
  }

  getTypeLabel(): string {
    if (!this.demande?.type) return 'Non défini';

    switch (this.demande.type) {
      case 'ANNUEL':
        return 'Congés annuels';

      case 'MALADIE':
        return 'Maladie';

      case 'SANS_SOLDE':
        return 'Sans solde';

      case 'MATERNITE':
        return 'Congé maternité';

      case 'PATERNITE':
        return 'Congé paternité';

      default:
        return this.demande.type;
    }
  }

  getFormattedDate(dateValue: string | Date | undefined | null): string {
    if (!dateValue) return '-';

    const date = dateValue instanceof Date
      ? dateValue
      : new Date(dateValue);

    if (Number.isNaN(date.getTime())) return '-';

    return date.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  }

  getDuree(): string {
    if (!this.demande?.dateDebut || !this.demande?.dateFin) {
      return '-';
    }

    const debut = new Date(this.demande.dateDebut);
    const fin = new Date(this.demande.dateFin);

    if (Number.isNaN(debut.getTime()) || Number.isNaN(fin.getTime())) {
      return '-';
    }

    const diffTime = Math.abs(fin.getTime() - debut.getTime());
    const totalDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
    const joursOuvres = this.demande.joursOuvres || totalDays;

    return `${totalDays} jour(s) (${joursOuvres} ouvré(s))`;
  }

  getMotif(): string {
    if (!this.demande) return 'Aucun motif fourni.';

    const commentaire = this.demande.commentaire?.trim();

    return commentaire || 'Aucun motif fourni.';
  }
}