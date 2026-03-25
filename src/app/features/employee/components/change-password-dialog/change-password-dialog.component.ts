import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-change-password-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule
  ],
  template: `
    <h2 mat-dialog-title>Changer mon mot de passe</h2>
    <mat-dialog-content>
      <form [formGroup]="passwordForm">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Ancien mot de passe</mat-label>
          <input matInput [type]="hideOld ? 'password' : 'text'" formControlName="oldPassword">
          <button mat-icon-button matSuffix (click)="hideOld = !hideOld" type="button">
            <mat-icon>{{ hideOld ? 'visibility_off' : 'visibility' }}</mat-icon>
          </button>
          <mat-error *ngIf="passwordForm.get('oldPassword')?.hasError('required')">
            L'ancien mot de passe est requis
          </mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Nouveau mot de passe</mat-label>
          <input matInput [type]="hideNew ? 'password' : 'text'" formControlName="newPassword">
          <button mat-icon-button matSuffix (click)="hideNew = !hideNew" type="button">
            <mat-icon>{{ hideNew ? 'visibility_off' : 'visibility' }}</mat-icon>
          </button>
          <mat-error *ngIf="passwordForm.get('newPassword')?.hasError('required')">
            Le nouveau mot de passe est requis
          </mat-error>
          <mat-error *ngIf="passwordForm.get('newPassword')?.hasError('minlength')">
            Minimum 6 caractères
          </mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Confirmer le mot de passe</mat-label>
          <input matInput [type]="hideConfirm ? 'password' : 'text'" formControlName="confirmPassword">
          <button mat-icon-button matSuffix (click)="hideConfirm = !hideConfirm" type="button">
            <mat-icon>{{ hideConfirm ? 'visibility_off' : 'visibility' }}</mat-icon>
          </button>
          <mat-error *ngIf="passwordForm.hasError('passwordMismatch')">
            Les mots de passe ne correspondent pas
          </mat-error>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">Annuler</button>
      <button mat-raised-button color="primary" [disabled]="passwordForm.invalid" (click)="onConfirm()">
        Changer
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .full-width {
      width: 100%;
      margin-bottom: 16px;
    }
  `]
})
export class ChangePasswordDialogComponent {
  passwordForm: FormGroup;
  hideOld = true;
  hideNew = true;
  hideConfirm = true;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<ChangePasswordDialogComponent>
  ) {
    this.passwordForm = this.fb.group({
      oldPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(form: FormGroup) {
    const newPassword = form.get('newPassword')?.value;
    const confirmPassword = form.get('confirmPassword')?.value;
    return newPassword === confirmPassword ? null : { passwordMismatch: true };
  }

  onConfirm(): void {
    if (this.passwordForm.valid) {
      this.dialogRef.close({
        oldPassword: this.passwordForm.value.oldPassword,
        newPassword: this.passwordForm.value.newPassword
      });
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}