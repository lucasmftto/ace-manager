import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { ClassService } from '../services/class.service';
import { ClassScheduleSummary, DAY_OF_WEEK_LABELS, CLASS_TYPE_LABELS } from '../models/class.model';

@Component({
  selector: 'app-schedule-list',
  standalone: true,
  imports: [
    CommonModule, MatTableModule, MatButtonModule, MatIconModule,
    MatChipsModule, MatTooltipModule, MatProgressSpinnerModule
  ],
  templateUrl: './schedule-list.component.html'
})
export class ScheduleListComponent implements OnInit {
  private readonly classService = inject(ClassService);
  private readonly router       = inject(Router);

  readonly dayLabels:  Record<string, string> = DAY_OF_WEEK_LABELS;
  readonly typeLabels: Record<string, string> = CLASS_TYPE_LABELS;
  readonly statusLabels: Record<string, string> = { ACTIVE: 'Ativo', INACTIVE: 'Inativo' };

  displayedColumns = ['name', 'dayOfWeek', 'time', 'type', 'teacher', 'enrolled', 'status', 'actions'];
  dataSource = new MatTableDataSource<ClassScheduleSummary>([]);
  loading = true;
  total   = 0;

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.classService.findSchedules().subscribe({
      next: page => { this.dataSource.data = page.content; this.total = page.page.totalElements; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  formatTime(t: string): string { return t ? t.slice(0, 5) : ''; }

  goToNew():               void { this.router.navigate(['/classes/schedules/new']); }
  goToDetail(id: string):  void { this.router.navigate(['/classes/schedules', id]); }
  goToEdit(id: string):    void { this.router.navigate(['/classes/schedules', id, 'edit']); }
  goToCalendar():          void { this.router.navigate(['/classes']); }
}
