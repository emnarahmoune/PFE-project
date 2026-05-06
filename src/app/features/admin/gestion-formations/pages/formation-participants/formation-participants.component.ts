import { Component, OnInit } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormationService } from '../../../../../core/services/formation.service';

import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

import { EmployeeAvatarComponent } from '../../../../../shared/layouts/components/employee-avatar/employee-avatar.component';

@Component({
  selector: 'app-formation-participants',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    EmployeeAvatarComponent
  ],
  templateUrl: './formation-participants.component.html',
  styleUrls: ['./formation-participants.component.scss']
})
export class FormationParticipantsComponent implements OnInit {

  participants: any[] = [];
  loading = true;
  formationId!: number;

  constructor(
    private route: ActivatedRoute,
    private formationService: FormationService,
    private router: Router,
    private location: Location
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = Number(idParam);

    if (!idParam || Number.isNaN(id)) {
      this.router.navigate(['/admin/formations']);
      return;
    }

    this.formationId = id;
    this.loadParticipants();
  }

  loadParticipants(): void {
    this.loading = true;

    this.formationService.getParticipants(this.formationId).subscribe({
      next: (data: any) => {
        console.log('Participants:', data);

        this.participants = Array.isArray(data)
          ? data
          : data?.data || [];

        this.loading = false;
      },
      error: (err: any) => {
        console.error('Erreur participants:', err);
        this.participants = [];
        this.loading = false;
      }
    });
  }

  goBack(): void {
    this.location.back();
  }
}