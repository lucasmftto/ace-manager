import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { PlanService } from '../services/plan.service';
import { Plan, PLAN_TYPE_LABELS, PLAN_STATUS_LABELS } from '../models/plan.model';

@Component({
  selector: 'app-plans-list',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule, MatButtonModule, MatIconModule,
    MatChipsModule, MatTooltipModule, MatProgressSpinnerModule
  ],
  templateUrl: './plans-list.component.html'
})
export class PlansListComponent implements OnInit {
  private readonly planService = inject(PlanService);
  private readonly router = inject(Router);

  readonly typeLabels:   Record<string, string> = PLAN_TYPE_LABELS;
  readonly statusLabels: Record<string, string> = PLAN_STATUS_LABELS;

  displayedColumns = ['name', 'type', 'referencePrice', 'details', 'status', 'actions'];
  dataSource = new MatTableDataSource<Plan>([]);
  loading = true;
  total   = 0;

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.planService.findAll().subscribe({
      next: page => { this.dataSource.data = page.content; this.total = page.page.totalElements; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  planDetails(p: Plan): string {
    if (p.type === 'MONTHLY')  return `${p.weeklyClassCount ?? '?'}x/sem · dia ${p.billingDayOfMonth ?? '?'}`;
    if (p.type === 'BUNDLE')   return `${p.totalClasses ?? '?'} aulas`;
    if (p.type === 'GROUP')    return `Máx. ${p.maxStudents ?? '?'} alunos`;
    return '—';
  }

  goToNew():             void { this.router.navigate(['/plans/new']); }
  goToDetail(id: string): void { this.router.navigate(['/plans', id]); }
  goToEdit(id: string):   void { this.router.navigate(['/plans', id, 'edit']); }
}
