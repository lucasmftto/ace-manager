import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { NgxMaskDirective } from 'ngx-mask';

import { TeacherService } from '../services/teacher.service';
import { PayoutModel, TeacherStatus } from '../models/teacher.model';

@Component({
  selector: 'app-teacher-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, NgxMaskDirective
  ],
  templateUrl: './teacher-form.component.html'
})
export class TeacherFormComponent implements OnInit {
  private readonly fb             = inject(FormBuilder);
  private readonly teacherService = inject(TeacherService);
  private readonly route          = inject(ActivatedRoute);
  private readonly router         = inject(Router);

  readonly payoutModels: PayoutModel[] = ['PERCENTAGE', 'HOURLY_RATE'];
  readonly statusOptions: TeacherStatus[] = ['ACTIVE', 'INACTIVE'];

  readonly payoutLabels: Record<string, string> = {
    PERCENTAGE: 'Porcentagem', HOURLY_RATE: 'Valor por hora'
  };
  readonly statusLabels: Record<string, string> = {
    ACTIVE: 'Ativo', INACTIVE: 'Inativo'
  };

  isEdit    = false;
  teacherId: string | null = null;
  loading   = false;
  saving    = false;
  errorMsg  = '';

  form = this.fb.group({
    name:               ['', Validators.required],
    phone:              [''],
    email:              ['', Validators.email],
    payoutModel:        ['PERCENTAGE' as PayoutModel, Validators.required],
    defaultPercentage:  [null as number | null],
    defaultHourlyRate:  [null as number | null],
    status:             ['ACTIVE' as TeacherStatus]
  });

  get isPercentage(): boolean { return this.form.get('payoutModel')?.value === 'PERCENTAGE'; }
  get isHourlyRate(): boolean { return this.form.get('payoutModel')?.value === 'HOURLY_RATE'; }

  ngOnInit(): void {
    this.teacherId = this.route.snapshot.paramMap.get('id');
    this.isEdit    = !!this.teacherId && !this.router.url.endsWith('/new');

    this.form.get('payoutModel')?.valueChanges.subscribe(() => {
      this.form.patchValue({ defaultPercentage: null, defaultHourlyRate: null });
    });

    if (this.isEdit && this.teacherId) {
      this.loading = true;
      this.teacherService.findById(this.teacherId).subscribe({
        next: t => {
          this.form.patchValue({
            name: t.name, phone: t.phone ?? '', email: t.email ?? '',
            payoutModel: t.payoutModel,
            defaultPercentage: t.defaultPercentage,
            defaultHourlyRate: t.defaultHourlyRate,
            status: t.status
          });
          this.loading = false;
        },
        error: () => { this.errorMsg = 'Erro ao carregar professor.'; this.loading = false; }
      });
    }
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }

    const raw = this.form.getRawValue();
    if (raw.payoutModel === 'PERCENTAGE' && raw.defaultPercentage == null) {
      this.errorMsg = 'Informe a porcentagem padrão.'; return;
    }
    if (raw.payoutModel === 'HOURLY_RATE' && raw.defaultHourlyRate == null) {
      this.errorMsg = 'Informe o valor por hora padrão.'; return;
    }

    this.saving   = true;
    this.errorMsg = '';

    const payload = {
      name:              raw.name!,
      phone:             raw.phone ? raw.phone.replace(/\D/g, '') : null,
      email:             raw.email || null,
      payoutModel:       raw.payoutModel as PayoutModel,
      defaultPercentage: raw.payoutModel === 'PERCENTAGE' ? raw.defaultPercentage : null,
      defaultHourlyRate: raw.payoutModel === 'HOURLY_RATE' ? raw.defaultHourlyRate : null
    };

    const obs = this.isEdit
      ? this.teacherService.update(this.teacherId!, { ...payload, status: raw.status as TeacherStatus })
      : this.teacherService.create(payload);

    obs.subscribe({
      next: t  => this.router.navigate(['/teachers', t.id]),
      error: (err) => {
        this.errorMsg = err?.error?.message || 'Erro ao salvar professor.';
        this.saving = false;
      }
    });
  }

  cancel(): void {
    if (this.isEdit && this.teacherId) {
      this.router.navigate(['/teachers', this.teacherId]);
    } else {
      this.router.navigate(['/teachers']);
    }
  }
}
