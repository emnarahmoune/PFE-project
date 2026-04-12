import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { EmployeeCongeService } from '../../../services/employee-conge.service';
import { DemandeConge, CongeResponse } from '../../../models/conge.model';
import { AuthService } from '../../../../../core/services/auth.service';
import { Location } from '@angular/common';

@Component({
  selector: 'app-detail-conge',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './detail-conge.component.html',
  styleUrls: ['./detail-conge.component.css']
})
export class DetailCongeComponent implements OnInit {

  demande: DemandeConge | null = null;
  loading = false;
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

  checkUserRole(): void {
    const role = this.authService.getUserRole();
    this.isAdmin = role === 'ADMIN_RH';
    this.isEmploye = role === 'EMPLOYE';
  }

  loadDemande(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error = 'ID de demande non trouvé';
      return;
    }

    this.loading = true;
    
    if (this.isAdmin) {
      this.loadDemandeForAdmin(Number(id));
    } else {
      this.loadDemandeForEmploye(Number(id));
    }
  }

  loadDemandeForAdmin(id: number): void {
    this.congeService.getAllDemandesAdmin().subscribe({
      next: (response: CongeResponse) => {
        const demandes = response.data as DemandeConge[];
        const demande = demandes?.find((d: DemandeConge) => d.id === id);
        if (demande) {
          this.demande = demande;
          this.checkCanAnnuler();
        } else {
          this.error = 'Demande non trouvée';
        }
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Erreur chargement demande:', err);
        this.error = 'Erreur lors du chargement de la demande';
        this.loading = false;
      }
    });
  }

  loadDemandeForEmploye(id: number): void {
    this.congeService.getMesConges().subscribe({
      next: (response: CongeResponse) => {
        const demandes = response.data as DemandeConge[];
        const demande = demandes?.find((d: DemandeConge) => d.id === id);
        if (demande) {
          this.demande = demande;
          this.checkCanAnnuler();
        } else {
          this.error = 'Demande non trouvée';
        }
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Erreur chargement demande:', err);
        this.error = 'Erreur lors du chargement de la demande';
        this.loading = false;
      }
    });
  }

  checkCanAnnuler(): void {
    if (this.demande && this.isEmploye) {
      this.canAnnuler = this.demande.statut === 'EN_ATTENTE';
    }
  }

  annulerDemande(): void {
    if (!this.demande || !this.demande.id) return;

    if (confirm('Êtes-vous sûr de vouloir annuler cette demande de congé ?')) {
      this.loading = true;
      this.congeService.annulerConge(this.demande.id).subscribe({
        next: () => {
          if (this.demande) {
            this.demande.statut = 'ANNULE';
            this.canAnnuler = false;
          }
          this.loading = false;
          alert('Demande annulée avec succès');
        },
        error: (err: any) => {
          console.error('Erreur annulation:', err);
          this.error = err.error?.error || 'Erreur lors de l\'annulation';
          this.loading = false;
        }
      });
    }
  }

  goBack(): void {
    if (this.isAdmin) {
      this.router.navigate(['/admin/conges']);
    } else {
      this.router.navigate(['/employee/mes-conges']);
    }
  }

  getStatutClass(): string {
    if (!this.demande) return 'badge-secondary';
    switch(this.demande.statut) {
      case 'EN_ATTENTE': return 'badge-warning';
      case 'APPROUVE': return 'badge-success';
      case 'REFUSE': return 'badge-danger';
      case 'ANNULE': return 'badge-secondary';
      default: return 'badge-info';
    }
  }

  getStatutLabel(): string {
    if (!this.demande) return '';
    switch(this.demande.statut) {
      case 'EN_ATTENTE': return 'En attente';
      case 'APPROUVE': return 'Approuvé';
      case 'REFUSE': return 'Refusé';
      case 'ANNULE': return 'Annulé';
      default: return this.demande.statut || '';
    }
  }

  getTypeLabel(): string {
    if (!this.demande) return '';
    switch(this.demande.type) {
      case 'ANNUEL': return 'Congés annuels';
      case 'MALADIE': return 'Maladie';
      case 'SANS_SOLDE': return 'Sans solde';
      case 'MATERNITE': return 'Congé maternité';
      case 'PATERNITE': return 'Congé paternité';
      default: return this.demande.type || '';
    }
  }

  getFormattedDate(dateStr: string | Date | undefined | null): string {
    if (!dateStr) return '';
    try {
      const date = typeof dateStr === 'string' ? new Date(dateStr) : dateStr;
      if (isNaN(date.getTime())) return '';
      return date.toLocaleDateString('fr-FR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      });
    } catch (error) {
      return '';
    }
  }

  getDuree(): string {
    if (!this.demande || !this.demande.dateDebut || !this.demande.dateFin) return '';
    try {
      const debut = new Date(this.demande.dateDebut);
      const fin = new Date(this.demande.dateFin);
      if (isNaN(debut.getTime()) || isNaN(fin.getTime())) return '';
      const diffTime = Math.abs(fin.getTime() - debut.getTime());
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
      return `${diffDays} jour(s) (${this.demande.joursOuvres || diffDays} jour(s) ouvré(s))`;
    } catch (error) {
      return '';
    }
  }

  // ✅ CORRECTION: Ajouter la méthode getMotif()
  getMotif(): string {
    if (!this.demande) return 'Aucun motif fourni';
    return this.demande.commentaire || this.demande.motifRefus || 'Aucun motif fourni';
  }
}