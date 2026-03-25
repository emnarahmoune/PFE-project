import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-auth-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  template: `
    <form [formGroup]="form" (ngSubmit)="onSubmit()" class="auth-form">
      <h2>{{ title }}</h2>
      <p class="subtitle">{{ subtitle }}</p>

      <ng-content select=".form-content"></ng-content>

      <div class="form-actions">
        <button mat-raised-button color="primary" type="submit" 
                [disabled]="form.invalid || loading" class="submit-button">
          <mat-spinner diameter="20" *ngIf="loading"></mat-spinner>
          <span *ngIf="!loading">{{ submitLabel }}</span>
        </button>
      </div>

      <div class="additional-links">
        <ng-content select=".links"></ng-content>
      </div>
    </form>
  `,
  styles: [`
    .auth-form {
      max-width: 400px;
      width: 100%;
      padding: 2rem;
      background: white;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }
    h2 {
      margin: 0 0 0.5rem;
      color: #333;
    }
    .subtitle {
      color: #666;
      margin-bottom: 2rem;
    }
    .form-actions {
      margin-top: 1.5rem;
    }
    .submit-button {
      width: 100%;
      height: 48px;
    }
    .additional-links {
      margin-top: 1.5rem;
      text-align: center;
    }
  `]
})
export class AuthFormComponent {
  @Input() title: string = '';
  @Input() subtitle: string = '';
  @Input() submitLabel: string = '';
  @Input() loading: boolean = false;
  @Input() form!: FormGroup;
  @Output() formSubmit = new EventEmitter<any>();

  onSubmit(): void {
    if (this.form.valid) {
      this.formSubmit.emit(this.form.value);
    }
  }
}