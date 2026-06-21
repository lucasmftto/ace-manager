import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

import { ClassService } from '../services/class.service';
import { TeacherService } from '../../teachers/services/teacher.service';
import { TeacherSummary } from '../../teachers/models/teacher.model';
import {
  ClassType, ClassScheduleStatus, DayOfWeek,
  DAY_OF_WEEK_LABELS, DAY_OF_WEEK_ORDER, CLASS_TYPE_LABELS
} from '../models/class.model';

@Component({
  selector: 'app-schedule-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule
  ],
  templateUrl: './schedule-form.component.html'
})
export class ScheduleFormComponent implements OnInit {
  private readonly fb           = inject(FormBuilder);
  private readonly classService = inject(ClassService);
  private readonly teacherService = inject(TeacherService);
  private readonly route        = inject(ActivatedRoute);
  private readonly router       = inject(Router);

  readonly daysOfWeek: DayOfWeek[]   = DAY_OF_WEEK_ORDER;
  readonly classTypes: ClassType[]   = ['INDIVIDUAL', 'GROUP'];
  readonly statusOptions: ClassScheduleStatus[] = ['ACTIVE', 'INACTIVE'];
  readonly dayLabels:  Record<string, string>   = DAY_OF_WEEK_LABELS;
  readonly typeLabels: Record<string, string>   = CLASS_TYPE_LABELS;
  readonly statusLabels: Record<string, string> = { ACTIVE: 'Ativo', INACTIVE: 'Inativo' };

  isEdit     = false;
  scheduleId: string | null = null;
  loading    = false;
  saving     = false;
  errorMsg   = '';
  teachers: TeacherSummary[] = [];

  form = this.fb.group({
    name:            ['', Validators.required],
    dayOfWeek:       ['MONDAY' as DayOfWeek, Validators.required],
    startTime:       ['', Validators.required],
    durationMinutes: [60, [Validators.required, Validators.min(30)]],
    teacherId:       ['', Validators.required],
    type:            ['INDIVIDUAL' as ClassType, Validators.required],
    maxStudents:     [1 as number | null],
    status:          ['ACTIVE' as ClassScheduleStatus]
  });

  get isGroup(): boolean { return this.form.get('type')?.value === 'GROUP'; }

  ngOnInit(): void {
    this.scheduleId = this.route.snapshot.paramMap.get('id');
    this.isEdit = !!this.scheduleId && !this.router.url.endsWith('/new');

    this.teacherService.findAll().subscribe({
      next: page => { this.teachers = page.content; }
    });

    if (this.isEdit && this.scheduleId) {
      this.loading = true;
      this.classService.findScheduleById(this.scheduleId).subscribe({
        next: s => {
          this.form.patchValue({
            name: s.name, dayOfWeek: s.dayOfWeek, startTime: s.startTime.slice(0,5),
            durationMinutes: s.durationMinutes, teacherId: s.teacherId,
            type: s.type, maxStudents: s.maxStudents, status: s.status
          });
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

    const payload = {
      name:            raw.name!,
      dayOfWeek:       raw.dayOfWeek as DayOfWeek,
      startTime:       raw.startTime! + ':00',
      durationMinutes: raw.durationMinutes!,
      teacherId:       raw.teacherId!,
      type:            raw.type as ClassType,
      maxStudents:     this.isGroup ? raw.maxStudents : null,
      status:          raw.status as ClassScheduleStatus
    };

    const obs = this.isEdit
      ? this.classService.updateSchedule(this.scheduleId!, payload)
      : this.classService.createSchedule(payload);

    obs.subscribe({
      next: s => this.router.navigate(['/classes/schedules', 'id' in s ? (s as any).id : this.scheduleId]),
      error: err => { this.errorMsg = err?.error?.message || 'Erro ao salvar grade.'; this.saving = false; }
    });
  }

  cancel(): void {
    this.router.navigate(this.isEdit && this.scheduleId ? ['/classes/schedules', this.scheduleId] : ['/classes/schedules']);
  }
}
