import { Component, OnInit, Pipe, PipeTransform } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatMenuModule } from '@angular/material/menu';
import { MatIconModule } from '@angular/material/icon';
import { CompetenceService } from '../../services/competence.service';
import { Competence } from '../../models/competence.model';
import { ConfirmationDialogComponent } from '../../../../../shared/layouts/components/confirmation-dialog/confirmation-dialog.component';

// Pipe pour remplacer les underscores par des espaces
@Pipe({ name: 'replace', standalone: true })
export class ReplacePipe implements PipeTransform {
  transform(value: string | null | undefined, from: string, to: string): string {
    if (!value) return '';
    return value.split(from).join(to);
  }
}

@Component({
  selector: 'app-competence-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    MatSnackBarModule,
    MatDialogModule,
    MatMenuModule,   // ✅ AJOUTÉ
    MatIconModule,   // ✅ AJOUTÉ
    ReplacePipe
  ],
  templateUrl: './competence-list.component.html',
  styleUrls: ['./competence-list.component.scss']
})
export class CompetenceListComponent implements OnInit {
  allData: Competence[] = [];
  loading = false;
  searchText = '';
  selectedCategorie = 'TOUTES';
  currentPage = 0;
  readonly pageSize = 9; // Pour une grille 3x3

  // Tri
  sortField: keyof Competence | '' = '';
  sortDirection: 'asc' | 'desc' = 'asc';

  // Statistiques
  stats = {
    total: 0,
    technique: 0,
    softSkill: 0,
    linguistique: 0,
    management: 0
  };

  // Catégories avec icônes et couleurs
  categories = [
    { value: 'TOUTES', label: 'Toutes', icon: '📌', color: '#6B7280' },
    { value: 'TECHNIQUE', label: 'Technique', icon: '⚙️', color: '#3B82F6' },
    { value: 'SOFT_SKILL', label: 'Soft skill', icon: '🤝', color: '#EC4899' },
    { value: 'LINGUISTIQUE', label: 'Linguistique', icon: '🌐', color: '#10B981' },
    { value: 'MANAGEMENT', label: 'Management', icon: '📊', color: '#F59E0B' }
  ];

  constructor(
    private svc: CompetenceService,
    private snack: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadCompetences();
    this.loadStats();
  }

  // ── Chargement des données ──────────────────────────────────
  loadCompetences(): void {
    this.loading = true;
    this.svc.getAll().subscribe({
      next: (res) => {
        this.allData = Array.isArray(res.data) ? (res.data as Competence[]) : [];
        this.currentPage = 0;
        this.loading = false;
      },
      error: () => {
        this.snack.open('Erreur lors du chargement des compétences', '×', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  loadStats(): void {
    this.svc.getStats().subscribe({
      next: (res) => {
        const s = res.data as any;
        this.stats = {
          total: s.total || 0,
          technique: s.TECHNIQUE || s.technique || 0,
          softSkill: s.SOFT_SKILL || s.softSkill || 0,
          linguistique: s.LINGUISTIQUE || s.linguistique || 0,
          management: s.MANAGEMENT || s.management || 0
        };
      },
      error: () => {}
    });
  }

  // ── Filtrage et tri ─────────────────────────────────────────
  getFilteredData(): Competence[] {
    const search = this.searchText.trim().toLowerCase();
    let data = this.allData.filter(c => {
      const matchSearch = !search ||
        (c.nom?.toLowerCase().includes(search) || false) ||
        (c.description?.toLowerCase().includes(search) || false);
      const matchCat = this.selectedCategorie === 'TOUTES' || c.categorie === this.selectedCategorie;
      return matchSearch && matchCat;
    });

    if (this.sortField) {
      const field = this.sortField;
      const dir = this.sortDirection === 'asc' ? 1 : -1;
      data = [...data].sort((a, b) => {
        const av = a[field] ?? '';
        const bv = b[field] ?? '';
        if (typeof av === 'number' && typeof bv === 'number') {
          return (av - bv) * dir;
        }
        return String(av).localeCompare(String(bv)) * dir;
      });
    }
    return data;
  }

  getPagedData(): Competence[] {
    const filtered = this.getFilteredData();
    const start = this.currentPage * this.pageSize;
    return filtered.slice(start, start + this.pageSize);
  }

  getTotalPages(): number {
    return Math.ceil(this.getFilteredData().length / this.pageSize);
  }

  applyFilter(): void {
    this.currentPage = 0;
  }

  setCategorie(cat: string): void {
    this.selectedCategorie = cat;
    this.currentPage = 0;
  }

  resetFilters(): void {
    this.searchText = '';
    this.selectedCategorie = 'TOUTES';
    this.currentPage = 0;
    this.sortField = '';
    this.sortDirection = 'asc';
  }

  sortBy(field: keyof Competence): void {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'asc';
    }
    this.currentPage = 0;
  }

  getSortIcon(field: string): string {
    if (this.sortField !== field) return '';
    return this.sortDirection === 'asc' ? '↑' : '↓';
  }

  getNiveauMoyenPourcentage(niveau?: number): number {
    if (!niveau) return 0;
    return Math.round((niveau / 4) * 100);
  }

  // ── Pagination ──────────────────────────────────────────────
  previousPage(): void {
    if (this.currentPage > 0) this.currentPage--;
  }

  nextPage(): void {
    if (this.currentPage + 1 < this.getTotalPages()) this.currentPage++;
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.getTotalPages()) {
      this.currentPage = page;
    }
  }

  getPages(): number[] {
    const total = this.getTotalPages();
    const current = this.currentPage;
    const pages: number[] = [];

    if (total <= 7) {
      for (let i = 0; i < total; i++) pages.push(i);
    } else {
      if (current <= 3) {
        for (let i = 0; i <= 4; i++) pages.push(i);
        pages.push(-1);
        pages.push(total - 1);
      } else if (current >= total - 4) {
        pages.push(0);
        pages.push(-1);
        for (let i = total - 5; i < total; i++) pages.push(i);
      } else {
        pages.push(0);
        pages.push(-1);
        for (let i = current - 1; i <= current + 1; i++) pages.push(i);
        pages.push(-1);
        pages.push(total - 1);
      }
    }
    return pages;
  }

  // ── Suppression ─────────────────────────────────────────────
  deleteCompetence(id: number, nom: string): void {
    (document.activeElement as HTMLElement)?.blur();
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '440px',
      data: {
        title: 'Supprimer la compétence',
        message: `Supprimer "${nom}" ? Cette action est irréversible.`,
        confirmText: 'Supprimer',
        cancelText: 'Annuler'
      }
    });
    dialogRef.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.svc.delete(id).subscribe({
        next: () => {
          this.allData = this.allData.filter(c => c.id !== id);
          this.loadStats();
          this.snack.open('Compétence supprimée', '×', { duration: 3000 });
        },
        error: (err) => {
          this.snack.open(err?.error?.message || 'Erreur de suppression', '×', { duration: 4000 });
        }
      });
    });
  }

  // ── Rafraîchissement ────────────────────────────────────────
  refresh(): void {
    this.loadCompetences();
    this.loadStats();
    this.snack.open('Données actualisées', '×', { duration: 2000 });
  }

  // ── Utilitaires pour le template ────────────────────────────
  getCategoryColor(categorie: string): string {
    const found = this.categories.find(c => c.value === categorie);
    return found ? found.color : '#6B7280';
  }

  getCategoryIcon(categorie: string): string {
    const found = this.categories.find(c => c.value === categorie);
    return found ? found.icon : '📌';
  }
}