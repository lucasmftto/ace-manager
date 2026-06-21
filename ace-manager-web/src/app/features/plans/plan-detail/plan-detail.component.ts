import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';

import { PlanService } from '../services/plan.service';
import { Plan, PLAN_TYPE_LABELS, PLAN_STATUS_LABELS } from '../models/plan.model';

@Component({
  selector: 'app-plan-detail',
  standalone: true,
  imports: [
    CommonModule, MatButtonModule, MatIconModule,
    MatChipsModule, MatTableModule, MatDialogModule
  ],
  templateUrl: './plan-detail.component.html'
})
export class PlanDetailComponent implements OnInit {
  private readonly planService = inject(PlanService);
  private readonly route       = inject(ActivatedRoute);
  private readonly router      = inject(Router);

  readonly typeLabels:   Record<string, string> = PLAN_TYPE_LABELS;
  readonly statusLabels: Record<string, string> = PLAN_STATUS_LABELS;

  plan: Plan | null = null;
  loading = true;
  planId  = '';

  ngOnInit(): void {
    this.planId = this.route.snapshot.paramMap.get('id') ?? '';
    this.load();
  }

  load(): void {
    this.loading = true;
    this.planService.findById(this.planId).subscribe({
      next: p => { this.plan = p; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  goToEdit():  void { this.router.navigate(['/plans', this.planId, 'edit']); }
  goToList():  void { this.router.navigate(['/plans']); }

  deactivate(): void {
    if (!confirm('Inativar este plano?')) return;
    this.planService.deactivate(this.planId).subscribe({
      next: () => this.router.navigate(['/plans']),
      error: () => alert('Erro ao inativar plano.')
    });
  }

  typeDetailLabel(p: Plan): string {
    if (p.type === 'MONTHLY') return `${p.weeklyClassCount ?? '—'}x/semana · cobrança dia ${p.billingDayOfMonth ?? '—'}`;
    if (p.type === 'BUNDLE')  return `${p.totalClasses ?? '—'} aulas no pacote`;
    if (p.type === 'GROUP')   return `Máximo ${p.maxStudents ?? '—'} alunos`;
    return '—';
  }
}
