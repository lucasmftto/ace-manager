import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';

import { ClassService } from '../services/class.service';
import {
  ClassOccurrenceDetail, AttendanceStatus, AttendanceRecord,
  OCCURRENCE_STATUS_LABELS, ATTENDANCE_STATUS_LABELS
} from '../models/class.model';

interface AttendanceRow {
  studentId: string;
  studentName: string;
  status: AttendanceStatus;
  studentBilledValue: number | null;
  teacherPayoutValue: number | null;
}

@Component({
  selector: 'app-class-detail',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatButtonModule, MatIconModule,
    MatChipsModule, MatSelectModule, MatFormFieldModule
  ],
  templateUrl: './class-detail.component.html'
})
export class ClassDetailComponent implements OnInit {
  private readonly classService = inject(ClassService);
  private readonly route        = inject(ActivatedRoute);
  private readonly router       = inject(Router);

  readonly statusLabels: Record<string, string>     = OCCURRENCE_STATUS_LABELS;
  readonly attendanceLabels: Record<string, string> = ATTENDANCE_STATUS_LABELS;
  readonly attendanceOptions: AttendanceStatus[]    = ['PRESENT', 'ABSENT', 'JUSTIFIED_ABSENCE'];

  occurrence: ClassOccurrenceDetail | null = null;
  attendanceRows: AttendanceRow[] = [];
  loading  = true;
  saving   = false;
  errorMsg = '';
  occurrenceId = '';

  ngOnInit(): void {
    this.occurrenceId = this.route.snapshot.paramMap.get('id') ?? '';
    this.load();
  }

  load(): void {
    this.loading = true;
    this.classService.findOccurrenceById(this.occurrenceId).subscribe({
      next: occ => {
        this.occurrence = occ;
        this.attendanceRows = occ.attendances.map(a => ({
          studentId: a.studentId, studentName: a.studentName,
          status: a.status, studentBilledValue: a.studentBilledValue,
          teacherPayoutValue: a.teacherPayoutValue
        }));
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  saveAttendance(): void {
    if (!this.attendanceRows.length) return;
    this.saving = true; this.errorMsg = '';
    this.classService.updateAttendance(this.occurrenceId, {
      attendances: this.attendanceRows.map(r => ({ studentId: r.studentId, status: r.status }))
    }).subscribe({
      next: occ => {
        this.occurrence = occ;
        this.attendanceRows = occ.attendances.map(a => ({
          studentId: a.studentId, studentName: a.studentName,
          status: a.status, studentBilledValue: a.studentBilledValue,
          teacherPayoutValue: a.teacherPayoutValue
        }));
        this.saving = false;
      },
      error: err => { this.errorMsg = err?.error?.message || 'Erro ao salvar presença.'; this.saving = false; }
    });
  }

  cancelOccurrence(): void {
    if (!confirm('Cancelar esta aula? Todas as presenças serão removidas.')) return;
    this.classService.cancelOccurrence(this.occurrenceId).subscribe({
      next: () => this.load(),
      error: () => alert('Erro ao cancelar aula.')
    });
  }

  goBack(): void { this.router.navigate(['/classes']); }
  goToSchedule(): void {
    if (this.occurrence) this.router.navigate(['/classes/schedules', this.occurrence.scheduleId]);
  }

  formatDate(iso: string): string {
    const [y, m, d] = iso.split('-');
    return `${d}/${m}/${y}`;
  }

  formatTime(t: string): string { return t ? t.slice(0, 5) : ''; }

  isCancelled(): boolean { return this.occurrence?.status === 'CANCELLED'; }
}
