import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidatorFn,
  Validators
} from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { NgxCurrencyDirective } from 'ngx-currency';
import { NgxMaskDirective } from 'ngx-mask';

import { StudentService } from '../services/student.service';
import { StudentDetail, PaymentMethod, StudentStatus } from '../models/student.model';

const currentNotExceedsAgreed: ValidatorFn = (control: AbstractControl) => {
  const agreed  = control.get('agreedMonthlyValue')?.value;
  const current = control.get('currentMonthlyValue')?.value;
  if (agreed != null && current != null && current > agreed) {
    return { currentExceedsAgreed: true };
  }
  return null;
};

@Component({
  selector: 'app-student-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule, MatButtonModule, MatIconModule,
    MatDatepickerModule, MatNativeDateModule,
    NgxCurrencyDirective, NgxMaskDirective
  ],
  templateUrl: './student-form.component.html'
})
export class StudentFormComponent implements OnInit {
  private readonly fb             = inject(FormBuilder);
  private readonly studentService = inject(StudentService);
  private readonly route          = inject(ActivatedRoute);
  private readonly router         = inject(Router);

  readonly paymentMethods: PaymentMethod[] = ['PIX', 'BOLETO', 'PIX_AUTOMATIC', 'CREDIT_CARD', 'OTHER'];
  readonly statusOptions: StudentStatus[]   = ['ACTIVE', 'INACTIVE', 'SUSPENDED'];

  readonly paymentLabels: Record<string, string> = {
    PIX: 'PIX', BOLETO: 'Boleto', PIX_AUTOMATIC: 'PIX Automático', CREDIT_CARD: 'Cartão de Crédito', OTHER: 'Outro'
  };
  readonly statusLabels: Record<string, string> = {
    ACTIVE: 'Ativo', INACTIVE: 'Inativo', SUSPENDED: 'Suspenso'
  };

  isEdit = false;
  studentId: string | null = null;
  loading   = false;
  saving    = false;
  errorMsg  = '';
  pickerDate: Date | null = null;

  form = this.fb.group({
    name:                   ['', Validators.required],
    phone:                  [''],
    email:                  ['', Validators.email],
    birthDate:              ['' as string | null],
    guardianName:           [''],
    guardianPhone:          [''],
    agreedMonthlyValue:     [0, [Validators.required, Validators.min(0)]],
    currentMonthlyValue:    [0, [Validators.required, Validators.min(0)]],
    preferredPaymentMethod: ['PIX' as PaymentMethod, Validators.required],
    status:                 ['ACTIVE' as StudentStatus],
    notes:                  ['', Validators.maxLength(500)]
  }, { validators: currentNotExceedsAgreed });

  get showGuardianFields(): boolean {
    const raw = this.form.get('birthDate')?.value as string | null;
    if (!raw || raw.length < 8) return false;
    const [day, month, year] = raw.split('/').map(Number);
    if (!day || !month || !year || year < 1900) return false;
    const age = new Date().getFullYear() - year;
    return age < 18;
  }

  get isMinorWithoutGuardian(): boolean {
    if (!this.showGuardianFields) return false;
    const name  = this.form.get('guardianName')?.value?.trim();
    const phone = this.form.get('guardianPhone')?.value?.trim();
    return !name || !phone;
  }

  ngOnInit(): void {
    this.studentId = this.route.snapshot.paramMap.get('id');
    this.isEdit    = !!this.studentId && !this.router.url.endsWith('/new');

    if (this.isEdit && this.studentId) {
      this.loading = true;
      this.studentService.findById(this.studentId).subscribe({
        next: s => { this.patchForm(s); this.loading = false; },
        error: () => { this.errorMsg = 'Erro ao carregar aluno.'; this.loading = false; }
      });
    }
  }

  submit(): void {
    if (this.form.invalid || this.isMinorWithoutGuardian) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving  = true;
    this.errorMsg = '';
    const raw    = this.form.getRawValue();

    const payload = {
      name:                   raw.name!,
      phone:                  raw.phone ? raw.phone.replace(/\D/g, '') : null,
      email:                  raw.email || null,
      birthDate:              raw.birthDate ? this.parseBirthDate(raw.birthDate) : null,
      guardianName:           raw.guardianName || null,
      guardianPhone:          raw.guardianPhone ? raw.guardianPhone.replace(/\D/g, '') : null,
      agreedMonthlyValue:     raw.agreedMonthlyValue!,
      currentMonthlyValue:    raw.currentMonthlyValue!,
      preferredPaymentMethod: raw.preferredPaymentMethod as PaymentMethod,
      notes:                  raw.notes || null
    };

    const obs = this.isEdit
      ? this.studentService.update(this.studentId!, { ...payload, status: raw.status as StudentStatus })
      : this.studentService.create(payload);

    obs.subscribe({
      next: s  => this.router.navigate(['/students', s.id]),
      error: () => { this.errorMsg = 'Erro ao salvar aluno.'; this.saving = false; }
    });
  }

  onDatePickerChange(event: { value: Date | null }): void {
    if (event.value) {
      this.pickerDate = event.value;
      const d = event.value;
      const day   = String(d.getDate()).padStart(2, '0');
      const month = String(d.getMonth() + 1).padStart(2, '0');
      const year  = d.getFullYear();
      this.form.get('birthDate')?.setValue(`${day}/${month}/${year}`);
    }
  }

  cancel(): void {
    if (this.isEdit && this.studentId) {
      this.router.navigate(['/students', this.studentId]);
    } else {
      this.router.navigate(['/students']);
    }
  }

  private patchForm(s: StudentDetail): void {
    if (s.birthDate) {
      this.pickerDate = new Date(s.birthDate + 'T12:00:00');
    }
    this.form.patchValue({
      name:                   s.name,
      phone:                  s.phone ?? '',
      email:                  s.email ?? '',
      birthDate:              s.birthDate ? this.isoToBrDate(s.birthDate) : '',
      guardianName:           s.guardianName ?? '',
      guardianPhone:          s.guardianPhone ?? '',
      agreedMonthlyValue:     s.agreedMonthlyValue,
      currentMonthlyValue:    s.currentMonthlyValue,
      preferredPaymentMethod: s.preferredPaymentMethod,
      status:                 s.status,
      notes:                  s.notes ?? ''
    });
  }

  private parseBirthDate(ddmmyyyy: string): string | null {
    const [day, month, year] = ddmmyyyy.split('/');
    if (!day || !month || !year || year.length < 4) return null;
    return `${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`;
  }

  private isoToBrDate(iso: string): string {
    const [year, month, day] = iso.split('-');
    return `${day}/${month}/${year}`;
  }
}
