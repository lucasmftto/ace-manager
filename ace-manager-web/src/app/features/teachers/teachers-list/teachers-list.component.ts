import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { TeacherService } from '../services/teacher.service';
import { TeacherSummary, PAYOUT_MODEL_LABELS, TEACHER_STATUS_LABELS } from '../models/teacher.model';
import { FormatPhonePipe } from '../../../shared/pipes/format-phone.pipe';

@Component({
  selector: 'app-teachers-list',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule, MatButtonModule, MatIconModule,
    MatChipsModule, MatTooltipModule, MatProgressSpinnerModule,
    FormatPhonePipe
  ],
  templateUrl: './teachers-list.component.html'
})
export class TeachersListComponent implements OnInit {
  private readonly teacherService = inject(TeacherService);
  private readonly router = inject(Router);

  readonly payoutLabels: Record<string, string> = PAYOUT_MODEL_LABELS;
  readonly statusLabels: Record<string, string>  = TEACHER_STATUS_LABELS;

  displayedColumns = ['name', 'phone', 'payoutModel', 'payoutValue', 'studentConfigCount', 'status', 'actions'];
  dataSource = new MatTableDataSource<TeacherSummary>([]);
  loading = true;
  totalElements = 0;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.teacherService.findAll().subscribe({
      next: page => {
        this.dataSource.data = page.content;
        this.totalElements = page.page.totalElements;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  payoutValue(t: TeacherSummary): string {
    if (t.payoutModel === 'PERCENTAGE' && t.defaultPercentage != null) {
      return `${t.defaultPercentage}%`;
    }
    if (t.payoutModel === 'HOURLY_RATE' && t.defaultHourlyRate != null) {
      return `R$ ${t.defaultHourlyRate.toFixed(2).replace('.', ',')}/h`;
    }
    return '—';
  }

  goToNew():      void { this.router.navigate(['/teachers/new']); }
  goToDetail(id: string): void { this.router.navigate(['/teachers', id]); }
  goToEdit(id: string):   void { this.router.navigate(['/teachers', id, 'edit']); }
}
