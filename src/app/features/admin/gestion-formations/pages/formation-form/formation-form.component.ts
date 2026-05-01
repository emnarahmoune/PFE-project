import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormArray,
  FormBuilder,
  FormGroup,
  Validators,
  ReactiveFormsModule
} from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';

import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';

import { FormationService } from '../../../../../core/services/formation.service';
import { Formation } from '../../models/formation.model';

type DomaineFormation =
  | 'TECHNIQUE'
  | 'SOFT_SKILLS'
  | 'MANAGEMENT'
  | 'LANGUES'
  | 'SECURITE'
  | 'INFORMATIQUE';

interface DomaineOption {
  value: DomaineFormation;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-formation-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatDividerModule
  ],
  templateUrl: './formation-form.component.html',
  styleUrls: ['./formation-form.component.css']
})
export class FormationFormComponent implements OnInit {

  formationForm!: FormGroup;

  isEditMode = false;
  formationId: number | null = null;

  loading = false;
  submitting = false;

  domaines: DomaineOption[] = [
    { value: 'TECHNIQUE', label: 'Technique', icon: 'build' },
    { value: 'SOFT_SKILLS', label: 'Soft Skills', icon: 'psychology' },
    { value: 'MANAGEMENT', label: 'Management', icon: 'groups' },
    { value: 'LANGUES', label: 'Langues', icon: 'language' },
    { value: 'SECURITE', label: 'Sécurité', icon: 'security' },
    { value: 'INFORMATIQUE', label: 'Informatique', icon: 'computer' }
  ];

  constructor(
    private fb: FormBuilder,
    private formationService: FormationService,
    private route: ActivatedRoute,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.formationForm = this.initForm();

    const idParam = this.route.snapshot.paramMap.get('id');

    if (idParam && idParam !== 'new') {
      const id = Number(idParam);

      if (!Number.isNaN(id)) {
        this.isEditMode = true;
        this.formationId = id;
        this.loadFormation(id);
        return;
      }
    }

    this.isEditMode = false;
    this.formationId = null;
  }

  initForm(): FormGroup {
    return this.fb.group({
      titre: ['', Validators.required],
      domaine: ['', Validators.required],
      dureeHeures: [1, [Validators.required, Validators.min(1)]],
      description: ['', Validators.required],
      actif: [true],
      videos: this.fb.array([]),
      supports: this.fb.array([])
    });
  }

  get videos(): FormArray {
    return this.formationForm.get('videos') as FormArray;
  }

  get supports(): FormArray {
    return this.formationForm.get('supports') as FormArray;
  }

  createVideoGroup(data?: any): FormGroup {
    return this.fb.group({
      id: [data?.id || null],
      titre: [data?.titre || '', Validators.required],
      urlYoutube: [
        data?.urlYoutube || data?.url_youtube || data?.url || '',
        Validators.required
      ],
      ordre: [data?.ordre || this.videos.length + 1]
    });
  }

  createSupportGroup(data?: any): FormGroup {
    return this.fb.group({
      id: [data?.id || null],
      titre: [data?.titre || '', Validators.required],
      fichierUrl: [
        data?.fichierUrl || data?.fichier_url || data?.url || data?.pdfPath || '',
        Validators.required
      ],
      ordre: [data?.ordre || this.supports.length + 1]
    });
  }

  addVideo(): void {
    this.videos.push(this.createVideoGroup());
  }

  removeVideo(index: number): void {
    this.videos.removeAt(index);
    this.reorderVideos();
  }

  addSupport(): void {
    this.supports.push(this.createSupportGroup());
  }

  removeSupport(index: number): void {
    this.supports.removeAt(index);
    this.reorderSupports();
  }

  reorderVideos(): void {
    this.videos.controls.forEach((control, index) => {
      control.patchValue({ ordre: index + 1 });
    });
  }

  reorderSupports(): void {
    this.supports.controls.forEach((control, index) => {
      control.patchValue({ ordre: index + 1 });
    });
  }

  loadFormation(id: number): void {
    this.loading = true;

    this.formationService.getById(id).subscribe({
      next: (res: any) => {
        const formation = this.extractFormation(res);

        console.log('FORMATION COMPLETE RECUE = ', formation);
        console.log('VIDEOS RECUES = ', formation?.videos);
        console.log('SUPPORTS RECUS = ', formation?.supports);

        this.formationForm.patchValue({
          titre: formation?.titre || '',
          domaine: this.normalizeDomaine(formation?.domaine || 'TECHNIQUE'),
          dureeHeures: formation?.dureeHeures || 1,
          description: formation?.description || '',
          actif: formation?.actif ?? true
        });

        this.videos.clear();
        this.supports.clear();

        const videos = this.extractVideos(formation);
        const supports = this.extractSupports(formation);

        videos
          .sort((a: any, b: any) => (a.ordre || 0) - (b.ordre || 0))
          .forEach((video: any) => {
            this.videos.push(this.createVideoGroup(video));
          });

        supports
          .sort((a: any, b: any) => (a.ordre || 0) - (b.ordre || 0))
          .forEach((support: any) => {
            this.supports.push(this.createSupportGroup(support));
          });

        this.loading = false;
      },
      error: (err: any) => {
        console.error('Erreur chargement formation:', err);
        this.loading = false;

        this.snackBar.open('Erreur lors du chargement de la formation', 'Fermer', {
          duration: 3000
        });

        this.router.navigate(['/admin/formations']);
      }
    });
  }

  private extractFormation(res: any): any {
    if (!res) {
      return {};
    }

    if (res.data) {
      return res.data;
    }

    if (res.formation) {
      return res.formation;
    }

    return res;
  }

  private extractVideos(formation: any): any[] {
    if (!formation) {
      return [];
    }

    if (Array.isArray(formation.videos)) {
      return formation.videos;
    }

    if (Array.isArray(formation.formationVideos)) {
      return formation.formationVideos;
    }

    if (Array.isArray(formation.videoList)) {
      return formation.videoList;
    }

    return [];
  }

  private extractSupports(formation: any): any[] {
    if (!formation) {
      return [];
    }

    if (Array.isArray(formation.supports)) {
      return formation.supports;
    }

    if (Array.isArray(formation.formationSupports)) {
      return formation.formationSupports;
    }

    if (Array.isArray(formation.supportList)) {
      return formation.supportList;
    }

    return [];
  }

  normalizeDomaine(domaine: string): DomaineFormation {
    const value = domaine?.toString().trim().toUpperCase();

    const found = this.domaines.find(d => d.value === value);

    if (found) {
      return found.value;
    }

    return 'TECHNIQUE';
  }

  getSelectedDomaine(): DomaineOption | undefined {
    const value = this.formationForm?.get('domaine')?.value;
    return this.domaines.find(d => d.value === value);
  }

  getSelectedIcon(): string {
    return this.getSelectedDomaine()?.icon || 'help';
  }

  getSelectedLabel(): string {
    return this.getSelectedDomaine()?.label || 'Choisir';
  }

  uploadSupportPdf(event: Event, index: number): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];

    if (!file) {
      return;
    }

    if (file.type !== 'application/pdf') {
      this.snackBar.open('Veuillez sélectionner un fichier PDF', 'Fermer', {
        duration: 3000
      });
      input.value = '';
      return;
    }

    const formData = new FormData();
    formData.append('file', file);

    this.formationService.uploadPdf(formData).subscribe({
      next: (res: any) => {
        const filePath =
          res?.filePath ||
          res?.fichierUrl ||
          res?.url ||
          res?.data?.filePath ||
          res?.data?.fichierUrl ||
          '';

        this.supports.at(index).patchValue({
          fichierUrl: filePath,
          titre: this.supports.at(index).get('titre')?.value || file.name
        });

        this.snackBar.open('PDF chargé avec succès', 'OK', {
          duration: 2000
        });
      },
      error: (err: any) => {
        console.error('Erreur upload PDF:', err);

        this.snackBar.open('Erreur lors du chargement du PDF', 'Fermer', {
          duration: 3000
        });
      }
    });
  }

  onSubmit(): void {
    if (this.formationForm.invalid) {
      this.formationForm.markAllAsTouched();

      this.snackBar.open('Veuillez remplir tous les champs obligatoires', 'Fermer', {
        duration: 3000
      });

      return;
    }

    this.submitting = true;

    const payload: Formation = {
      titre: this.formationForm.value.titre,
      domaine: this.normalizeDomaine(this.formationForm.value.domaine),
      dureeHeures: Number(this.formationForm.value.dureeHeures),
      description: this.formationForm.value.description,
      actif: this.formationForm.value.actif,

      videos: this.formationForm.value.videos.map((video: any, index: number) => ({
        id: video.id,
        titre: video.titre,
        urlYoutube: video.urlYoutube,
        ordre: index + 1
      })),

      supports: this.formationForm.value.supports.map((support: any, index: number) => ({
        id: support.id,
        titre: support.titre,
        fichierUrl: support.fichierUrl,
        ordre: index + 1
      }))
    };

    console.log('PAYLOAD FORMATION ENVOYE = ', payload);

    const request$ = this.isEditMode && this.formationId
      ? this.formationService.update(this.formationId, payload)
      : this.formationService.create(payload);

    request$.subscribe({
      next: () => {
        this.snackBar.open(
          this.isEditMode ? 'Formation modifiée avec succès ✅' : 'Formation créée avec succès ✅',
          'OK',
          { duration: 2500 }
        );

        this.router.navigate(['/admin/formations']);
      },
      error: (err: any) => {
        console.error(
          this.isEditMode ? 'Erreur modification formation:' : 'Erreur création formation:',
          err
        );

        this.submitting = false;

        this.snackBar.open(
          this.isEditMode ? 'Erreur lors de la modification' : 'Erreur lors de la création',
          'Fermer',
          { duration: 3000 }
        );
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/admin/formations']);
  }
}