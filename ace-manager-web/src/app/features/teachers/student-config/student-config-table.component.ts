import { Component, Input, OnChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSelectModule } from '@angular/material/select';

import { TeacherService } from '../services/teacher.service';
import { StudentPayoutConfig, PayoutModel } from '../models/teacher.model';
import { StudentService } from '../../students/services/student.service';
import { StudentSummary } from '../../students/models/student.model';

interface ConfigRow extends StudentPayoutConfig {
  editing: boolean;
  editValue: number | null;
}

@Component({
  selector: 'app-student-config-table',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatTableModule, MatButtonModule, MatIconModule,
    MatInputModule, MatFormFieldModule, MatSelectModule, MatChipsModule,
    MatTooltipModule, MatDialogModule, MatSnackBarModule
  ],
  templateUrl: './student-config-table.component.html'
})
export class StudentConfigTableComponent implements OnChanges {
  @Input() teacherId!: string;
  @Input() payoutModel!: PayoutModel;
  @Input() defaultPercentage: number | null = null;
  @Input() defaultHourlyRate: number | null = null;

  private readonly teacherService = inject(TeacherService);
  private readonly studentService = inject(StudentService);
  private readonly snackBar       = inject(MatSnackBar);

  displayedColumns = ['studentName', 'effectiveValue', 'override', 'actions'];
  dataSource = new MatTableDataSource<ConfigRow>([]);

  allStudents: StudentSummary[] = [];
  addingStudentId: string | null = null;
  addValue: number | null = null;
  saving = false;

  ngOnChanges(): void {
    if (this.teacherId) this.loadConfigs();
  }

  loadConfigs(): void {
    this.teacherService.findStudentConfigs(this.teacherId).subscribe(configs => {
      this.dataSource.data = configs.map(c => ({ ...c, editing: false, editValue: null }));
    });

    this.studentService.findAll({ status: 'ACTIVE' }).subscribe(page => {
      this.allStudents = page.content;
    });
  }

  get availableStudents(): StudentSummary[] {
    const configured = new Set(this.dataSource.data.map(r => r.studentId));
    return this.allStudents.filter(s => !configured.has(s.id));
  }

  effectiveLabel(row: ConfigRow): string {
    if (this.payoutModel === 'PERCENTAGE') {
      return row.effectivePercentage != null ? `${row.effectivePercentage}%` : '—';
    }
    return row.effectiveHourlyRate != null
      ? `R$ ${row.effectiveHourlyRate.toFixed(2).replace('.', ',')}/h`
      : '—';
  }

  startEdit(row: ConfigRow): void {
    row.editing  = true;
    row.editValue = this.payoutModel === 'PERCENTAGE' ? row.effectivePercentage : row.effectiveHourlyRate;
  }

  cancelEdit(row: ConfigRow): void {
    row.editing  = false;
    row.editValue = null;
  }

  saveEdit(row: ConfigRow): void {
    this.saving = true;
    const req = this.payoutModel === 'PERCENTAGE'
      ? { overridePercentage: row.editValue, overrideHourlyRate: null }
      : { overridePercentage: null, overrideHourlyRate: row.editValue };

    this.teacherService.upsertStudentConfig(this.teacherId, row.studentId, req).subscribe({
      next: updated => {
        const idx = this.dataSource.data.findIndex(r => r.studentId === row.studentId);
        this.dataSource.data[idx] = { ...updated, editing: false, editValue: null };
        this.dataSource.data = [...this.dataSource.data];
        this.saving = false;
        this.snackBar.open('Configuração salva.', 'Fechar', { duration: 3000 });
      },
      error: () => { this.saving = false; }
    });
  }

  removeOverride(row: ConfigRow): void {
    if (!confirm(`Remover override para ${row.studentName}? O repasse padrão do professor será usado.`)) return;

    this.teacherService.removeStudentConfig(this.teacherId, row.studentId).subscribe({
      next: () => {
        this.dataSource.data = this.dataSource.data.filter(r => r.studentId !== row.studentId);
        this.snackBar.open('Configuração removida.', 'Fechar', { duration: 3000 });
      }
    });
  }

  addConfig(): void {
    if (!this.addingStudentId || this.addValue == null) return;
    this.saving = true;

    const req = this.payoutModel === 'PERCENTAGE'
      ? { overridePercentage: this.addValue, overrideHourlyRate: null }
      : { overridePercentage: null, overrideHourlyRate: this.addValue };

    this.teacherService.upsertStudentConfig(this.teacherId, this.addingStudentId, req).subscribe({
      next: created => {
        this.dataSource.data = [...this.dataSource.data, { ...created, editing: false, editValue: null }];
        this.addingStudentId = null;
        this.addValue = null;
        this.saving = false;
        this.snackBar.open('Configuração adicionada.', 'Fechar', { duration: 3000 });
      },
      error: () => { this.saving = false; }
    });
  }
}
