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
import { NgxCurrencyDirective } from 'ngx-currency';

import { PlanService } from '../services/plan.service';
import { StudentPlan, StudentPlanStatus, STUDENT_PLAN_STATUS_LABELS } from '../models/plan.model';

@Component({
  selector: 'app-enrollment-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, NgxMaskDirective, NgxCurrencyDirective
  ],
  templateUrl: './enrollment-form.component.html'
})
export class EnrollmentFormComponent implements OnInit {
  private readonly fb          = inject(FormBuilder);
  private readonly planService = inject(PlanService);
  private readonly route       = inject(ActivatedRoute);
  private readonly router      = inject(Router);

  readonly statusOptions: StudentPlanStatus[] = ['ACTIVE', 'SUSPENDED', 'COMPLETED', 'CANCELLED'];
  readonly statusLabels: Record<string, string> = STUDENT_PLAN_STATUS_LABELS;

  studentId:    string = '';
  enrollmentId: string | null = null;
  isEdit = false;
  saving = false;
  loading = false;
  errorMsg = '';

  form = this.fb.group({
    planId:      ['', Validators.required],
    billedValue: [null as number | null, [Validators.required, Validators.min(0)]],
    startDate:   ['', Validators.required],
    status:      ['ACTIVE' as StudentPlanStatus]
  });

  ngOnInit(): void {
    this.studentId    = this.route.snapshot.paramMap.get('studentId') ?? '';
    this.enrollmentId = this.route.snapshot.paramMap.get('enrollmentId');
    this.isEdit = !!this.enrollmentId;

    const planIdFromQuery = this.route.snapshot.queryParamMap.get('planId');
    if (planIdFromQuery) this.form.patchValue({ planId: planIdFromQuery });

    if (this.isEdit && this.enrollmentId) {
      this.loading = true;
      this.planService.findEnrollments(this.studentId).subscribe({
        next: enrollments => {
          const e = enrollments.find(en => en.id === this.enrollmentId);
          if (e) {
            this.form.patchValue({
              planId: e.planId,
              billedValue: e.billedValue,
              startDate: e.startDate,
              status: e.status
            });
            this.form.get('planId')?.disable();
          }
          this.loading = false;
        },
        error: () => { this.loading = false; }
      });
    }
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving = true; this.errorMsg = '';
    const raw = this.form.getRawValue();

    if (this.isEdit && this.enrollmentId) {
      this.planService.updateEnrollment(this.studentId, this.enrollmentId, {
        billedValue: raw.billedValue,
        status: raw.status as StudentPlanStatus
      }).subscribe({
        next: () => this.goBack(),
        error: (err) => { this.errorMsg = err?.error?.message || 'Erro ao atualizar matrícula.'; this.saving = false; }
      });
    } else {
      this.planService.enroll(this.studentId, {
        planId: raw.planId!,
        teacherId: null,
        billedValue: raw.billedValue!,
        startDate: this.parseBrDate(raw.startDate ?? '')
      }).subscribe({
        next: () => this.goBack(),
        error: (err) => { this.errorMsg = err?.error?.message || 'Erro ao matricular aluno.'; this.saving = false; }
      });
    }
  }

  cancel(): void { this.goBack(); }
  private goBack(): void { this.router.navigate(['/students', this.studentId]); }

  private parseBrDate(ddmmyyyy: string): string {
    const d = ddmmyyyy.replace(/\D/g, '');
    if (d.length !== 8) return ddmmyyyy;
    return `${d.slice(4,8)}-${d.slice(2,4)}-${d.slice(0,2)}`;
  }
}
