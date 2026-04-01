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
    ReplacePipe
  ],
  templateUrl: './competence-list.component.html',
  styleUrls: ['./competence-list.component.css']
})
export class CompetenceListComponent implements OnInit {

  allData: Competence[] = [];
  loading = false;
  searchText = '';
  selectedCategorie = 'TOUTES';
  currentPage = 0;
  readonly pageSize = 12;

  // Tri
  sortField: keyof Competence | '' = '';
  sortDirection: 'asc' | 'desc' = 'asc';

  stats = { 
    total: 0, 
    technique: 0, 
    softSkill: 0, 
    linguistique: 0, 
    management: 0 
  };

  // Catégories disponibles
  categories = [
    { value: 'TOUTES', label: 'Toutes' },
    { value: 'TECHNIQUE', label: 'Technique' },
    { value: 'SOFT_SKILL', label: 'Soft Skill' },
    { value: 'LINGUISTIQUE', label: 'Linguistique' },
    { value: 'MANAGEMENT', label: 'Management' }
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
      error: (err) => {
        console.error('Erreur chargement statistiques:', err);
      }
    });
  }

  // ── Filtrage + tri côté client ─────────────────────────────

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
    const end = start + this.pageSize;
    return filtered.slice(start, end);
  }

  getTotalPages(): number {
    return Math.ceil(this.getFilteredData().length / this.pageSize);
  }

  applyFilter(): void { 
    this.currentPage = 0; 
  }
  
  setCategorie(categorie: string): void { 
    this.selectedCategorie = categorie; 
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

  // ── Calculs affichage ──────────────────────────────────────

  getNiveauMoyenPourcentage(niveau?: number): number {
    if (!niveau) return 0;
    return Math.round((niveau / 4) * 100);
  }

  getNiveauLabel(niveau?: number): string {
    if (!niveau) return 'Non défini';
    const labels: { [key: number]: string } = {
      1: 'Débutant',
      2: 'Intermédiaire',
      3: 'Avancé',
      4: 'Expert'
    };
    return labels[niveau] || 'Non défini';
  }

  // ── Pagination ─────────────────────────────────────────────

  nextPage(): void {
    if (this.currentPage + 1 < this.getTotalPages()) {
      this.currentPage++;
    }
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
    }
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
      for (let i = 0; i < total; i++) {
        pages.push(i);
      }
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

  // ── Suppression ───────────────────────────────────────────

  deleteCompetence(id: number, nom: string): void {
    // Perdre le focus
    (document.activeElement as HTMLElement)?.blur();

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      width: '440px',
      autoFocus: true,
      restoreFocus: false,
      data: {
        title: 'Supprimer la compétence',
        message: `Supprimer "${nom}" ? Cette action est irréversible.`,
        confirmText: 'Supprimer',
        cancelText: 'Annuler',
        confirmButtonColor: 'warn'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      
      this.svc.delete(id).subscribe({
        next: () => {
          // Suppression locale immédiate
          this.allData = this.allData.filter(c => c.id !== id);
          this.stats.total = Math.max(0, this.stats.total - 1);
          
          // Mise à jour des stats par catégorie
          const deletedComp = this.allData.find(c => c.id === id);
          if (deletedComp) {
            const category = deletedComp.categorie;
            switch (category) {
              case 'TECHNIQUE':
                this.stats.technique = Math.max(0, this.stats.technique - 1);
                break;
              case 'SOFT_SKILL':
                this.stats.softSkill = Math.max(0, this.stats.softSkill - 1);
                break;
              case 'LINGUISTIQUE':
                this.stats.linguistique = Math.max(0, this.stats.linguistique - 1);
                break;
              case 'MANAGEMENT':
                this.stats.management = Math.max(0, this.stats.management - 1);
                break;
            }
          }
          
          this.snack.open('Compétence supprimée avec succès', '×', { duration: 3000 });
          this.loadStats(); // Rafraîchir les stats depuis l'API pour garantir la cohérence
        },
        error: (err) => {
          const msg = err?.error?.message || 'Erreur lors de la suppression';
          this.snack.open(msg, '×', { duration: 4000 });
          console.error('Erreur suppression:', err);
        }
      });
    });
  }

  // ── Refresh data ───────────────────────────────────────────

  refresh(): void {
    this.loadCompetences();
    this.loadStats();
    this.snack.open('Données actualisées', '×', { duration: 2000 });
  }
}