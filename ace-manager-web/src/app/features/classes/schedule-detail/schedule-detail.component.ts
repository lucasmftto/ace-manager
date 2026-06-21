import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { ClassService } from '../services/class.service';
import { StudentService } from '../../students/services/student.service';
import { PlanService } from '../../plans/services/plan.service';
import { ClassScheduleDetail, DAY_OF_WEEK_LABELS, CLASS_TYPE_LABELS, ScheduleStudentResponse } from '../models/class.model';
import { StudentSummary } from '../../students/models/student.model';
import { StudentPlan } from '../../plans/models/plan.model';

@Component({
  selector: 'app-schedule-detail',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatButtonModule, MatIconModule,
    MatChipsModule, MatTableModule, MatFormFieldModule, MatSelectModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './schedule-detail.component.html'
})
export class ScheduleDetailComponent implements OnInit {
  private readonly classService   = inject(ClassService);
  private readonly studentService = inject(StudentService);
  private readonly planService    = inject(PlanService);
  private readonly route          = inject(ActivatedRoute);
  private readonly router         = inject(Router);
  private readonly fb             = inject(FormBuilder);

  readonly dayLabels:    Record<string, string> = DAY_OF_WEEK_LABELS;
  readonly typeLabels:   Record<string, string> = CLASS_TYPE_LABELS;
  readonly statusLabels: Record<string, string> = { ACTIVE: 'Ativo', INACTIVE: 'Inativo' };

  schedule: ClassScheduleDetail | null = null;
  dataSource = new MatTableDataSource<ScheduleStudentResponse>([]);
  displayedColumns = ['studentName', 'planName', 'actions'];
  loading = true;
  scheduleId = '';
  errorMsg = '';

  students: StudentSummary[] = [];
  studentPlans: StudentPlan[] = [];

  addForm = this.fb.group({
    studentId:    ['', Validators.required],
    studentPlanId: [null as string | null]
  });

  showAddForm = false;
  adding = false;
  generatingOcc = false;
  generateMsg = '';
  loadingPlans = false;
  plansError = '';

  ngOnInit(): void {
    this.scheduleId = this.route.snapshot.paramMap.get('id') ?? '';
    this.load();
    this.studentService.findAll().subscribe({
      next: page => { this.students = page.content; }
    });

    this.addForm.get('studentId')?.valueChanges.subscribe(studentId => {
      if (studentId) this.onStudentChange(studentId);
    });
  }

  load(): void {
    this.loading = true;
    this.classService.findScheduleById(this.scheduleId).subscribe({
      next: s => {
        this.schedule = s;
        this.dataSource.data = s.enrolledStudents;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  onStudentChange(studentId: string): void {
    this.studentPlans = [];
    this.plansError = '';
    this.addForm.patchValue({ studentPlanId: null });
    if (studentId) {
      this.loadingPlans = true;
      this.planService.findEnrollments(studentId).subscribe({
        next: plans => {
          this.studentPlans = plans.filter(p => p.status === 'ACTIVE');
          this.loadingPlans = false;
        },
        error: err => {
          this.plansError = err?.error?.message || 'Erro ao carregar planos do aluno.';
          this.loadingPlans = false;
        }
      });
    }
  }

  addStudent(): void {
    if (this.addForm.invalid) { this.addForm.markAllAsTouched(); return; }
    this.adding = true;
    const raw = this.addForm.getRawValue();
    this.classService.addStudent(this.scheduleId, {
      studentId: raw.studentId!, studentPlanId: raw.studentPlanId
    }).subscribe({
      next: s => {
        this.schedule = s; this.dataSource.data = s.enrolledStudents;
        this.showAddForm = false; this.addForm.reset(); this.adding = false;
      },
      error: err => { this.errorMsg = err?.error?.message || 'Erro ao adicionar aluno.'; this.adding = false; }
    });
  }

  removeStudent(studentId: string): void {
    if (!confirm('Remover aluno desta grade?')) return;
    this.classService.removeStudent(this.scheduleId, studentId).subscribe({
      next: s => { this.schedule = s; this.dataSource.data = s.enrolledStudents; },
      error: () => alert('Erro ao remover aluno.')
    });
  }

  generateOccurrences(): void {
    const today = new Date();
    const in4w  = new Date(); in4w.setDate(today.getDate() + 28);
    const fmt   = (d: Date) => d.toISOString().slice(0, 10);

    this.generatingOcc = true; this.generateMsg = '';
    this.classService.generateOccurrences({
      scheduleId: this.scheduleId, fromDate: fmt(today), toDate: fmt(in4w)
    }).subscribe({
      next: r => {
        this.generateMsg = `${r.generated} aula(s) gerada(s), ${r.skipped} já existia(m).`;
        this.generatingOcc = false;
      },
      error: () => { this.generateMsg = 'Erro ao gerar aulas.'; this.generatingOcc = false; }
    });
  }

  formatTime(t: string): string { return t ? t.slice(0, 5) : ''; }
  goToEdit():  void { this.router.navigate(['/classes/schedules', this.scheduleId, 'edit']); }
  goToList():  void { this.router.navigate(['/classes/schedules']); }
}
