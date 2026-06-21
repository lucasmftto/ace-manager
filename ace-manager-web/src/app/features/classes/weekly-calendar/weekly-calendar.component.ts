import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { FormsModule } from '@angular/forms';

import { ClassService } from '../services/class.service';
import { ClassOccurrenceSummary, DayOfWeek, DAY_OF_WEEK_ORDER, OCCURRENCE_STATUS_LABELS } from '../models/class.model';

interface DayColumn {
  label: string;
  date: Date;
  dateStr: string;
  isToday: boolean;
  occurrences: ClassOccurrenceSummary[];
}

@Component({
  selector: 'app-weekly-calendar',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule, MatChipsModule, MatTooltipModule, MatDialogModule],
  templateUrl: './weekly-calendar.component.html'
})
export class WeeklyCalendarComponent implements OnInit {
  private readonly classService = inject(ClassService);
  private readonly router       = inject(Router);

  readonly statusLabels: Record<string, string> = OCCURRENCE_STATUS_LABELS;
  readonly dayAbbr: Record<string, string> = {
    MONDAY: 'Seg', TUESDAY: 'Ter', WEDNESDAY: 'Qua',
    THURSDAY: 'Qui', FRIDAY: 'Sex', SATURDAY: 'Sáb', SUNDAY: 'Dom'
  };

  weekStart: Date = this.getWeekStart(new Date());
  days: DayColumn[] = [];
  loading = false;
  selectedTeacherId: string | null = null;

  teachers: { id: string; name: string; color: string }[] = [];
  teacherColors = ['#1D9E75', '#1565C0', '#6A1B9A', '#E65100', '#C62828', '#37474F'];

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    const from = this.formatDate(this.weekStart);
    const to   = this.formatDate(this.addDays(this.weekStart, 6));

    this.classService.findOccurrences({ dateFrom: from, dateTo: to }).subscribe({
      next: occurrences => {
        this.buildTeacherList(occurrences);
        this.buildDays(occurrences);
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  prevWeek(): void { this.weekStart = this.addDays(this.weekStart, -7); this.load(); }
  nextWeek(): void { this.weekStart = this.addDays(this.weekStart,  7); this.load(); }
  goToToday(): void { this.weekStart = this.getWeekStart(new Date()); this.load(); }

  filterByTeacher(teacherId: string | null): void {
    this.selectedTeacherId = teacherId;
    const from = this.formatDate(this.weekStart);
    const to   = this.formatDate(this.addDays(this.weekStart, 6));
    const params: any = { dateFrom: from, dateTo: to };
    if (teacherId) params.teacherId = teacherId;

    this.loading = true;
    this.classService.findOccurrences(params).subscribe({
      next: occ => { this.buildDays(occ); this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  openOccurrence(id: string): void {
    this.router.navigate(['/classes/occurrences', id]);
  }

  goToSchedules(): void { this.router.navigate(['/classes/schedules']); }

  getTeacherColor(teacherId: string): string {
    const t = this.teachers.find(t => t.id === teacherId);
    return t ? t.color : '#999';
  }

  formatTime(time: string): string {
    return time ? time.slice(0, 5) : '';
  }

  weekLabel(): string {
    const end = this.addDays(this.weekStart, 6);
    const f   = (d: Date) => `${d.getDate().toString().padStart(2,'0')}/${(d.getMonth()+1).toString().padStart(2,'0')}`;
    return `${f(this.weekStart)} – ${f(end)} de ${end.getFullYear()}`;
  }

  private buildDays(occurrences: ClassOccurrenceSummary[]): void {
    const today = this.formatDate(new Date());
    this.days = Array.from({ length: 7 }, (_, i) => {
      const date    = this.addDays(this.weekStart, i);
      const dateStr = this.formatDate(date);
      const dow     = DAY_OF_WEEK_ORDER[i];
      return {
        label:   this.dayAbbr[dow],
        date,
        dateStr,
        isToday: dateStr === today,
        occurrences: occurrences
          .filter(o => o.occurrenceDate === dateStr)
          .sort((a, b) => a.startTime.localeCompare(b.startTime))
      };
    });
  }

  private buildTeacherList(occurrences: ClassOccurrenceSummary[]): void {
    const seen = new Map<string, string>();
    occurrences.forEach(o => seen.set(o.teacherId, o.teacherName));
    this.teachers = Array.from(seen.entries()).map(([id, name], i) => ({
      id, name, color: this.teacherColors[i % this.teacherColors.length]
    }));
  }

  private getWeekStart(date: Date): Date {
    const d = new Date(date);
    const day = d.getDay();
    const diff = day === 0 ? -6 : 1 - day; // Monday start
    d.setDate(d.getDate() + diff);
    d.setHours(0, 0, 0, 0);
    return d;
  }

  private addDays(date: Date, n: number): Date {
    const d = new Date(date);
    d.setDate(d.getDate() + n);
    return d;
  }

  private formatDate(d: Date): string {
    return `${d.getFullYear()}-${(d.getMonth()+1).toString().padStart(2,'0')}-${d.getDate().toString().padStart(2,'0')}`;
  }
}
