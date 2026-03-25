import { Component, OnInit, Pipe, PipeTransform } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { CompetenceService } from '../../services/competence.service';
import { Competence } from '../../models/competence.model';
import { ConfirmationDialogComponent } from '../../../../../shared/layouts/components/confirmation-dialog/confirmation-dialog.component';

// Pipe inline pour remplacer _ par espace dans les catégories
@Pipe({ name: 'replace', standalone: true })
export class ReplacePipe implements PipeTransform {
  transform(value: string, from: string, to: string): string {
    return value ? value.split(from).join(to) : value;
  }
}

@Component({
  selector: 'app-competence-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, MatSnackBarModule, MatDialogModule, ReplacePipe],
  templateUrl: './competence-list.component.html',
  styleUrls: ['./competence-list.component.css']
})
export class CompetenceListComponent implements OnInit {

  allData:          Competence[] = [];
  loading           = false;
  searchText        = '';
  selectedCategorie = 'TOUTES';
  currentPage       = 0;
  readonly pageSize = 12;

  // Tri
  sortField:     keyof Competence | '' = '';
  sortDirection: 'asc' | 'desc'        = 'asc';

  stats = { total: 0, technique: 0, softSkill: 0, linguistique: 0, management: 0 };

  constructor(
    private svc:    CompetenceService,
    private snack:  MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadCompetences();
    this.loadStats();
  }

  // ── Chargement depuis l'API ────────────────────────────────

  loadCompetences(): void {
    this.loading = true;
    this.svc.getAll().subscribe({
      next: (res) => {
        this.allData = Array.isArray(res.data) ? res.data as Competence[] : [];
        this.currentPage = 0;
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur chargement compétences:', err);
        this.snack.open('Erreur lors du chargement', '×', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  loadStats(): void {
    this.svc.getStats().subscribe({
      next: (res) => {
        const s = res.data as any;
        this.stats = {
          total:        s.total        || 0,
          technique:    s.TECHNIQUE    || 0,
          softSkill:    s.SOFT_SKILL   || 0,
          linguistique: s.LINGUISTIQUE || 0,
          management:   s.MANAGEMENT   || 0
        };
      },
      error: (err) => console.error('Erreur stats:', err)
    });
  }

  // ── Filtrage + tri côté client ─────────────────────────────

  getFilteredData(): Competence[] {
    const search = this.searchText.trim().toLowerCase();
    let data = this.allData.filter(c => {
      const matchSearch = !search ||
        c.nom?.toLowerCase().includes(search) ||
        c.description?.toLowerCase().includes(search);
      const matchCat = this.selectedCategorie === 'TOUTES' || c.categorie === this.selectedCategorie;
      return matchSearch && matchCat;
    });

    if (this.sortField) {
      const field = this.sortField;
      const dir   = this.sortDirection === 'asc' ? 1 : -1;
      data = [...data].sort((a, b) => {
        const av = (a as any)[field] ?? '';
        const bv = (b as any)[field] ?? '';
        if (typeof av === 'number') return (av - bv) * dir;
        return String(av).localeCompare(String(bv)) * dir;
      });
    }
    return data;
  }

  getPagedData(): Competence[] {
    const f = this.getFilteredData();
    return f.slice(this.currentPage * this.pageSize, (this.currentPage + 1) * this.pageSize);
  }

  getTotalPages(): number {
    return Math.ceil(this.getFilteredData().length / this.pageSize);
  }

  applyFilter():             void { this.currentPage = 0; }
  setCategorie(c: string):   void { this.selectedCategorie = c; this.currentPage = 0; }

  resetFilters(): void {
    this.searchText       = '';
    this.selectedCategorie = 'TOUTES';
    this.currentPage      = 0;
  }

  sortBy(field: keyof Competence): void {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField     = field;
      this.sortDirection = 'asc';
    }
    this.currentPage = 0;
  }

  getSortIcon(field: string): string {
    if (this.sortField !== field) return '';
    return this.sortDirection === 'asc' ? '↑' : '↓';
  }

  // ── Calculs affichage ──────────────────────────────────────

  getNiveauMoyenPourcentage(niveau?: number): number {
    return niveau ? Math.round((niveau / 4) * 100) : 0;
  }

  // ── Suppression ───────────────────────────────────────────

  deleteCompetence(id: number, nom: string): void {
    (document.activeElement as HTMLElement)?.blur();

    const ref = this.dialog.open(ConfirmationDialogComponent, {
      width: '440px',
      autoFocus: true,
      restoreFocus: false,
      data: {
        title:       'Supprimer la compétence',
        message:     `Supprimer "${nom}" ? Cette action est irréversible.`,
        confirmText: 'Supprimer',
        cancelText:  'Annuler'
      }
    });

    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.svc.delete(id).subscribe({
        next: () => {
          // Suppression locale immédiate
          this.allData = this.allData.filter(c => c.id !== id);
          this.stats.total = Math.max(0, this.stats.total - 1);
          this.snack.open('Compétence supprimée', '×', { duration: 3000 });
          this.loadStats(); // Rafraîchir les stats depuis l'API
        },
        error: (err) => {
          const msg = err?.error?.message || 'Erreur lors de la suppression';
          this.snack.open(msg, '×', { duration: 4000 });
        }
      });
    });
  }
}