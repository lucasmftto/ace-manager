import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { NgxCurrencyDirective } from 'ngx-currency';

import { PlanService } from '../services/plan.service';
import { PlanType, PlanStatus } from '../models/plan.model';

@Component({
  selector: 'app-plan-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, NgxCurrencyDirective
  ],
  templateUrl: './plan-form.component.html'
})
export class PlanFormComponent implements OnInit {
  private readonly fb          = inject(FormBuilder);
  private readonly planService = inject(PlanService);
  private readonly route       = inject(ActivatedRoute);
  private readonly router      = inject(Router);

  readonly planTypes: PlanType[] = ['MONTHLY', 'BUNDLE', 'DROP_IN', 'GROUP'];
  readonly statusOptions: PlanStatus[] = ['ACTIVE', 'INACTIVE'];

  readonly typeLabels:   Record<string, string> = { MONTHLY: 'Mensalidade', BUNDLE: 'Pacote de aulas', DROP_IN: 'Aula avulsa', GROUP: 'Turma' };
  readonly statusLabels: Record<string, string> = { ACTIVE: 'Ativo', INACTIVE: 'Inativo' };

  isEdit   = false;
  planId: string | null = null;
  loading  = false;
  saving   = false;
  errorMsg = '';

  form = this.fb.group({
    name:               ['', Validators.required],
    description:        [''],
    type:               ['MONTHLY' as PlanType, Validators.required],
    referencePrice:     [null as number | null, [Validators.required, Validators.min(0)]],
    weeklyClassCount:   [null as number | null],
    billingDayOfMonth:  [null as number | null, [Validators.min(1), Validators.max(28)]],
    totalClasses:       [null as number | null, [Validators.min(1)]],
    maxStudents:        [null as number | null, [Validators.min(1)]],
    status:             ['ACTIVE' as PlanStatus]
  });

  get selectedType(): PlanType { return this.form.get('type')?.value as PlanType; }
  get isMonthly():  boolean { return this.selectedType === 'MONTHLY'; }
  get isBundle():   boolean { return this.selectedType === 'BUNDLE'; }
  get isGroup():    boolean { return this.selectedType === 'GROUP'; }

  ngOnInit(): void {
    this.planId = this.route.snapshot.paramMap.get('id');
    this.isEdit = !!this.planId && !this.router.url.endsWith('/new');

    this.form.get('type')?.valueChanges.subscribe(() => {
      this.form.patchValue({ weeklyClassCount: null, billingDayOfMonth: null, totalClasses: null, maxStudents: null });
    });

    if (this.isEdit && this.planId) {
      this.loading = true;
      this.planService.findById(this.planId).subscribe({
        next: p => {
          this.form.patchValue({
            name: p.name, description: p.description ?? '', type: p.type,
            referencePrice: p.referencePrice, weeklyClassCount: p.weeklyClassCount,
            billingDayOfMonth: p.billingDayOfMonth, totalClasses: p.totalClasses,
            maxStudents: p.maxStudents, status: p.status
          });
          this.loading = false;
        },
        error: () => { this.errorMsg = 'Erro ao carregar plano.'; this.loading = false; }
      });
    }
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving = true; this.errorMsg = '';
    const raw = this.form.getRawValue();

    const payload = {
      name:              raw.name!,
      description:       raw.description || null,
      type:              raw.type as PlanType,
      referencePrice:    raw.referencePrice!,
      weeklyClassCount:  this.isMonthly ? raw.weeklyClassCount : null,
      billingDayOfMonth: this.isMonthly ? raw.billingDayOfMonth : null,
      totalClasses:      this.isBundle  ? raw.totalClasses : null,
      maxStudents:       this.isGroup   ? raw.maxStudents  : null
    };

    const obs = this.isEdit
      ? this.planService.update(this.planId!, { ...payload, status: raw.status as PlanStatus })
      : this.planService.create(payload);

    obs.subscribe({
      next: p => this.router.navigate(['/plans', p.id]),
      error: (err) => { this.errorMsg = err?.error?.message || 'Erro ao salvar plano.'; this.saving = false; }
    });
  }

  cancel(): void {
    this.router.navigate(this.isEdit && this.planId ? ['/plans', this.planId] : ['/plans']);
  }
}
