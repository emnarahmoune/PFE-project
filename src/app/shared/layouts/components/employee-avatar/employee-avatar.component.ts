import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PictureService } from '../../../../core/services/picture.service';

@Component({
  selector: 'app-employee-avatar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './employee-avatar.component.html',
  styleUrls: ['./employee-avatar.component.scss']
})
export class EmployeeAvatarComponent implements OnChanges {

  @Input() employee: any;
  @Input() size: 'sm' | 'md' | 'lg' | 'xl' = 'md';
  @Input() shape: 'circle' | 'square' = 'circle';

  imageError = false;
  normalizedPhotoUrl: string | null = null;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['employee']) {
      this.imageError = false;

      const rawPhoto =
        this.employee?.photoUrl ||
        this.employee?.photo_url ||
        this.employee?.photo ||
        this.employee?.imageUrl ||
        this.employee?.avatarUrl ||
        this.employee?.employePhotoProfil ||
        this.employee?.employePhotoUrl ||
        this.employee?.employeePhotoProfil ||
        this.employee?.employeePhotoUrl ||
        this.employee?.managerPhotoProfil ||
        this.employee?.managerPhotoUrl ||
        null;

      this.normalizedPhotoUrl = PictureService.buildDisplayUrl(rawPhoto);

      console.log('AVATAR EMPLOYEE = ', this.employee);
      console.log('AVATAR RAW PHOTO = ', rawPhoto);
      console.log('AVATAR NORMALIZED = ', this.normalizedPhotoUrl);
    }
  }

  get initials(): string {
    const prenom =
      this.employee?.prenom ||
      this.employee?.employePrenom ||
      this.employee?.employeePrenom ||
      this.employee?.managerPrenom ||
      this.employee?.firstName ||
      '';

    const nom =
      this.employee?.nom ||
      this.employee?.employeNom ||
      this.employee?.employeeNom ||
      this.employee?.managerNom ||
      this.employee?.lastName ||
      '';

    const p = String(prenom).trim().charAt(0).toUpperCase();
    const n = String(nom).trim().charAt(0).toUpperCase();

    return `${p}${n}`.trim() || '??';
  }

  get avatarClasses(): string[] {
    return [
      'employee-avatar',
      `avatar-${this.size}`,
      `avatar-${this.shape}`
    ];
  }

  onImageError(): void {
    console.error('ERREUR IMAGE AVATAR = ', this.normalizedPhotoUrl, this.employee);
    this.imageError = true;
  }
}