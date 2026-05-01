import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Location, CommonModule } from '@angular/common';
import { trigger, style, animate, transition } from '@angular/animations';
import { CompetenceService } from '../../../../../core/services/competence.service';

@Component({
  standalone: true,
  selector: 'app-competence-detail',
  templateUrl: './competence-detail.component.html',
  styleUrls: ['./competence-detail.component.scss'],
  imports: [CommonModule],
  animations: [
    trigger('fadeSlide', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateY(20px)' }),
        animate('400ms ease-out',
          style({ opacity: 1, transform: 'translateY(0)' })
        )
      ])
    ])
  ]
})
export class CompetenceDetailComponent implements OnInit {

  competence: any;
  employes: any[] = [];
  loading = true;

  constructor(
    private route: ActivatedRoute,
    private competenceService: CompetenceService,
    private router: Router,
    private location: Location
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadCompetence(+id);
    }
  }


  getNiveauValue(niveau: string): number {
  if (!niveau) return 0;

  switch (niveau.toUpperCase()) {
    case 'DEBUTANT': return 1;
    case 'INTERMEDIAIRE': return 2;
    case 'AVANCE': return 3;
    case 'EXPERT': return 4;
    default: return 0;
  }
}

  loadCompetence(id: number): void {
    this.loading = true;

    this.competenceService.getDetails(id).subscribe({
      next: (res: any) => {
        this.competence = res.data;
        this.employes = res.data.employes || [];
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  goBack(): void {
    this.location.back();
  }

  goToEmploye(emp: any) {
    this.router.navigate(['/admin/employes', emp.id]);
  }

  getBadgeClass(categorie: string): string {
    if (!categorie) return 'autre';

    const cat = categorie.toLowerCase();

    if (cat.includes('tech')) return 'technique';
    if (cat.includes('soft')) return 'soft';
    if (cat.includes('manage')) return 'management';

    return 'autre';
  }
}