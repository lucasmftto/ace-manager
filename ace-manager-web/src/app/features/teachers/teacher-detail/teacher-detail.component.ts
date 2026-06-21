import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';

import { TeacherService } from '../services/teacher.service';
import { TeacherDetail, PAYOUT_MODEL_LABELS, TEACHER_STATUS_LABELS } from '../models/teacher.model';
import { StudentConfigTableComponent } from '../student-config/student-config-table.component';
import { FormatPhonePipe } from '../../../shared/pipes/format-phone.pipe';

@Component({
  selector: 'app-teacher-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule, MatButtonModule, MatIconModule, MatChipsModule, MatDividerModule,
    StudentConfigTableComponent, FormatPhonePipe
  ],
  templateUrl: './teacher-detail.component.html'
})
export class TeacherDetailComponent implements OnInit {
  private readonly teacherService = inject(TeacherService);
  private readonly route          = inject(ActivatedRoute);
  private readonly router         = inject(Router);

  readonly payoutLabels:  Record<string, string> = PAYOUT_MODEL_LABELS;
  readonly statusLabels:  Record<string, string> = TEACHER_STATUS_LABELS;

  teacher: TeacherDetail | null = null;
  loading  = true;
  errorMsg = '';

  get defaultValueLabel(): string {
    if (!this.teacher) return '—';
    if (this.teacher.payoutModel === 'PERCENTAGE') {
      return this.teacher.defaultPercentage != null ? `${this.teacher.defaultPercentage}%` : '—';
    }
    return this.teacher.defaultHourlyRate != null
      ? `R$ ${this.teacher.defaultHourlyRate.toFixed(2).replace('.', ',')}/h`
      : '—';
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.teacherService.findById(id).subscribe({
      next:  t => { this.teacher = t; this.loading = false; },
      error: () => { this.errorMsg = 'Professor não encontrado.'; this.loading = false; }
    });
  }

  goToEdit():  void { this.router.navigate(['/teachers', this.teacher!.id, 'edit']); }
  goToList():  void { this.router.navigate(['/teachers']); }

  delete(): void {
    if (!confirm('Deseja excluir este professor?')) return;
    this.teacherService.delete(this.teacher!.id).subscribe({
      next: () => this.router.navigate(['/teachers'])
    });
  }
}
