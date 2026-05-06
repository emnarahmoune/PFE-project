import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Location, CommonModule } from '@angular/common';
import { trigger, style, animate, transition } from '@angular/animations';

import { CompetenceService } from '../../../../../core/services/competence.service';
import { EmployeeAvatarComponent } from '../../../../../shared/layouts/components/employee-avatar/employee-avatar.component';

@Component({
  standalone: true,
  selector: 'app-competence-detail',
  templateUrl: './competence-detail.component.html',
  styleUrls: ['./competence-detail.component.scss'],
  imports: [
    CommonModule,
    EmployeeAvatarComponent
  ],
  animations: [
    trigger('fadeSlide', [
      transition(':enter', [
        style({
          opacity: 0,
          transform: 'translateY(20px)'
        }),
        animate(
          '400ms ease-out',
          style({
            opacity: 1,
            transform: 'translateY(0)'
          })
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

  loadCompetence(id: number): void {
    this.loading = true;

    this.competenceService.getDetails(id).subscribe({
      next: (res: any) => {
        this.competence = res?.data || null;

        const rawEmployes = res?.data?.employes || [];

        console.log('DETAIL COMPETENCE RESPONSE = ', res);
        console.log('EMPLOYES COMPETENCE RAW = ', rawEmployes);

        this.employes = rawEmployes.map((emp: any) => this.normalizeEmployeForAvatar(emp));

        console.log('EMPLOYES COMPETENCE NORMALIZED = ', this.employes);

        this.loading = false;
      },
      error: (err: any) => {
        console.error('Erreur chargement compétence:', err);
        this.competence = null;
        this.employes = [];
        this.loading = false;
      }
    });
  }

  private normalizeEmployeForAvatar(emp: any): any {
    return {
      ...emp,

      id: emp.id || emp.employeId || emp.employeeId,

      prenom:
        emp.prenom ||
        emp.employePrenom ||
        emp.firstName ||
        emp.employeePrenom ||
        '',

      nom:
        emp.nom ||
        emp.employeNom ||
        emp.lastName ||
        emp.employeeNom ||
        '',

      email:
        emp.email ||
        emp.employeEmail ||
        emp.employeeEmail ||
        '',

      poste:
        emp.poste ||
        emp.employePoste ||
        emp.employeePoste ||
        'Employé',

      departement:
        emp.departement ||
        emp.employeDepartement ||
        emp.employeeDepartement ||
        '',

      photoUrl:
        emp.photoUrl ||
        emp.photo_url ||
        emp.photoProfil ||
        emp.photo ||
        emp.imageUrl ||
        emp.avatarUrl ||
        emp.employePhotoUrl ||
        emp.employePhotoProfil ||
        emp.employeePhotoUrl ||
        emp.employeePhotoProfil ||
        emp.profilPhoto ||
        emp.profilePhoto ||
        ''
    };
  }

  getNiveauValue(niveau: string): number {
    if (!niveau) {
      return 0;
    }

    switch (niveau.toUpperCase()) {
      case 'DEBUTANT':
        return 1;
      case 'INTERMEDIAIRE':
        return 2;
      case 'AVANCE':
        return 3;
      case 'EXPERT':
        return 4;
      default:
        return 0;
    }
  }

  getProgressValue(niveau: string): number {
    return this.getNiveauValue(niveau) * 25;
  }

  goBack(): void {
    this.location.back();
  }

  goToEmploye(emp: any): void {
    const id = emp?.id || emp?.employeId || emp?.employeeId;

    if (!id) {
      return;
    }

    this.router.navigate(['/admin/employes', id]);
  }

  getBadgeClass(categorie: string): string {
    if (!categorie) {
      return 'autre';
    }

    const cat = categorie.toLowerCase();

    if (cat.includes('tech')) {
      return 'technique';
    }

    if (cat.includes('soft')) {
      return 'soft';
    }

    if (cat.includes('manage')) {
      return 'management';
    }

    return 'autre';
  }
}