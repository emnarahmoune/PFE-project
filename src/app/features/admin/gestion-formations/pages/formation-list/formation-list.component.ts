import { Component, OnInit, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';

import { FormationService } from '../../../../../core/services/formation.service';
import { Formation } from '../../models/formation.model';
import { ConfirmationDialogComponent } from '../../../../../shared/layouts/components/confirmation-dialog/confirmation-dialog.component';

@Component({
  selector: 'app-formation-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatDialogModule,
    MatMenuModule,
    MatProgressBarModule,
    MatSlideToggleModule
  ],
  templateUrl: './formation-list.component.html',
  styleUrls: ['./formation-list.component.scss']
})
export class FormationListComponent implements OnInit, AfterViewInit {

  stats: any = {
    total: 0,
    actives: 0,
    dureeMoyenne: 0,
    totalParticipants: 0,
    participantsMoyens: 0,
    informatique: 0,
    technique: 0,
    softSkills: 0,
    management: 0,
    langues: 0,
    securite: 0
  };

  displayedColumns = ['titre', 'domaine', 'duree', 'participants', 'statut', 'actions'];

  dataSource = new MatTableDataSource<Formation>([]);
  formations: Formation[] = [];

  searchText = '';
  selectedDomaine = 'TOUS';

  domaines = [
    'TOUS',
    'INFORMATIQUE',
    'TECHNIQUE',
    'SOFT_SKILLS',
    'MANAGEMENT',
    'LANGUES',
    'SECURITE'
  ];

  loading = false;
  viewMode: 'grid' | 'table' = 'grid';

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private formationService: FormationService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadFormations();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  loadFormations(): void {
    this.loading = true;

    this.formationService.getAll().subscribe({
      next: (res: any) => {
        const data = this.extractFormations(res);

        this.formations = data;
        this.calculateStats(data);

        this.applyFilter();

        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.snackBar.open('Erreur chargement', 'Fermer', {
          duration: 3000
        });
      }
    });
  }

  extractFormations(res: any): Formation[] {
    if (Array.isArray(res)) {
      return res;
    }

    if (Array.isArray(res?.data)) {
      return res.data;
    }

    if (Array.isArray(res?.content)) {
      return res.content;
    }

    if (Array.isArray(res?.items)) {
      return res.items;
    }

    return [];
  }

  calculateStats(data: Formation[]): void {
    const total = data.length;

    this.stats.total = total;
    this.stats.actives = data.filter((f: any) => !!f.actif).length;

    this.stats.totalParticipants = data.reduce(
      (sum: number, f: any) => sum + Number(f.nombreParticipants || 0),
      0
    );

    const totalDuree = data.reduce(
      (sum: number, f: any) => sum + Number(f.dureeHeures || 0),
      0
    );

    this.stats.dureeMoyenne = total > 0 ? Math.round(totalDuree / total) : 0;

    this.stats.participantsMoyens =
      total > 0 ? Math.round(this.stats.totalParticipants / total) : 0;

    this.stats.informatique = data.filter((f: any) =>
      this.normalizeDomaine(f.domaine) === 'INFORMATIQUE'
    ).length;

    this.stats.technique = data.filter((f: any) =>
      this.normalizeDomaine(f.domaine) === 'TECHNIQUE'
    ).length;

    this.stats.softSkills = data.filter((f: any) =>
      this.normalizeDomaine(f.domaine) === 'SOFT_SKILLS'
    ).length;

    this.stats.management = data.filter((f: any) =>
      this.normalizeDomaine(f.domaine) === 'MANAGEMENT'
    ).length;

    this.stats.langues = data.filter((f: any) =>
      this.normalizeDomaine(f.domaine) === 'LANGUES'
    ).length;

    this.stats.securite = data.filter((f: any) =>
      this.normalizeDomaine(f.domaine) === 'SECURITE'
    ).length;
  }

  applyFilter(): void {
    const search = this.normalizeText(this.searchText);
    const selected = this.normalizeDomaine(this.selectedDomaine);

    let filtered = [...this.formations];

    if (search) {
      filtered = filtered.filter((f: any) => {
        const titre = this.normalizeText(f.titre);
        const description = this.normalizeText(f.description);
        const domaine = this.normalizeText(f.domaine);

        return (
          titre.includes(search) ||
          description.includes(search) ||
          domaine.includes(search)
        );
      });
    }

    if (selected !== 'TOUS') {
      filtered = filtered.filter((f: any) => {
        const formationDomaine = this.normalizeDomaine(f.domaine);
        return formationDomaine === selected;
      });
    }

    this.dataSource.data = filtered;

    if (this.paginator) {
      this.paginator.firstPage();
    }
  }

  filterByDomaine(domaine: string): void {
    this.selectedDomaine = this.normalizeDomaine(domaine);
    this.applyFilter();
  }

  resetFilters(): void {
    this.searchText = '';
    this.selectedDomaine = 'TOUS';
    this.applyFilter();
  }

normalizeDomaine(value: string | null | undefined): string {
  const raw = String(value || '')
    .toUpperCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .trim();

  const normalized = raw
    .replace(/[-\s]+/g, '_')
    .replace(/_+/g, '_');

  const aliases: Record<string, string> = {
    SOFT_SKILL: 'SOFT_SKILLS',
    SOFT_SKILLS: 'SOFT_SKILLS',
    SOFTSKILL: 'SOFT_SKILLS',
    SOFTSKILLS: 'SOFT_SKILLS',
    'SOFT SKILL': 'SOFT_SKILLS',
    'SOFT SKILLS': 'SOFT_SKILLS',

    INFORMATIQUE: 'INFORMATIQUE',
    INFO: 'INFORMATIQUE',

    TECHNIQUE: 'TECHNIQUE',
    TECHNICAL: 'TECHNIQUE',

    MANAGEMENT: 'MANAGEMENT',

    LANGUE: 'LANGUES',
    LANGUES: 'LANGUES',

    SECURITE: 'SECURITE',
    SECURITE_INFORMATIQUE: 'SECURITE',
    SECURITY: 'SECURITE'
  };

  return aliases[raw] || aliases[normalized] || normalized;
}
  normalizeText(value: string | null | undefined): string {
    return String(value || '')
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim();
  }

  getDomaineLabel(domaine: string | null | undefined): string {
    const normalized = this.normalizeDomaine(domaine);

    const labels: Record<string, string> = {
      TOUS: '📌 Tous',
      INFORMATIQUE: 'Informatique',
      TECHNIQUE: 'Technique',
      SOFT_SKILLS: 'Soft skills',
      MANAGEMENT: 'Management',
      LANGUES: 'Langues',
      SECURITE: 'Sécurité'
    };

    return labels[normalized] || normalized.replace(/_/g, ' ');
  }

  goToDetails(id: number): void {
    this.router.navigate(['/admin/formations', id]);
  }

  goToEdit(id: number): void {
    this.router.navigate(['/admin/formations', id, 'edit']);
  }

  goToParticipants(id: number): void {
    this.router.navigate(['/admin/formations', id, 'participants']);
  }

  deleteFormation(id: number, titre: string): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '400px',
      data: {
        title: 'Suppression',
        message: `Supprimer "${titre}" ?`
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (!result) {
        return;
      }

      this.formationService.delete(id).subscribe({
        next: () => {
          this.snackBar.open('Supprimé', 'OK', {
            duration: 2000
          });
          this.loadFormations();
        },
        error: () => {
          this.snackBar.open('Erreur suppression', 'Fermer', {
            duration: 3000
          });
        }
      });
    });
  }

  toggleStatut(f: Formation): void {
    if (!f.id) {
      return;
    }

    const call = f.actif
      ? this.formationService.desactiver(f.id)
      : this.formationService.activer(f.id);

    call.subscribe({
      next: () => {
        this.loadFormations();
      },
      error: () => {
        this.snackBar.open('Erreur changement statut', 'Fermer', {
          duration: 3000
        });
      }
    });
  }

  getDomaineColor(domaine: string | null | undefined): string {
    const normalized = this.normalizeDomaine(domaine);

    const map: Record<string, string> = {
      INFORMATIQUE: '#0EA5E9',
      TECHNIQUE: '#6366F1',
      SOFT_SKILLS: '#EC4899',
      MANAGEMENT: '#F59E0B',
      LANGUES: '#10B981',
      SECURITE: '#EF4444'
    };

    return map[normalized] || '#6B7280';
  }

  formatDuree(h: number): string {
    return `${h || 0}h`;
  }

  getParticipantPercentage(nombreParticipants: number, totalEmployes?: number): number {
    const total = totalEmployes && totalEmployes > 0 ? totalEmployes : 50;
    return Math.min((nombreParticipants / total) * 100, 100);
  }
}