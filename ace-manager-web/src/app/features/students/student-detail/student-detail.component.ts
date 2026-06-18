import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';

import { StudentService } from '../services/student.service';
import { StudentDetail, PAYMENT_METHOD_LABELS, STUDENT_STATUS_LABELS } from '../models/student.model';
import { FormatPhonePipe } from '../../../shared/pipes/format-phone.pipe';

@Component({
  selector: 'app-student-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule, MatButtonModule, MatIconModule, MatChipsModule, MatDividerModule,
    FormatPhonePipe
  ],
  templateUrl: './student-detail.component.html'
})
export class StudentDetailComponent implements OnInit {
  private readonly studentService = inject(StudentService);
  private readonly route          = inject(ActivatedRoute);
  private readonly router         = inject(Router);

  readonly paymentLabels: Record<string, string> = PAYMENT_METHOD_LABELS;
  readonly statusLabels: Record<string, string>  = STUDENT_STATUS_LABELS;

  student: StudentDetail | null = null;
  loading = true;
  error   = false;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.studentService.findById(id).subscribe({
      next: s => { this.student = s; this.loading = false; },
      error: () => { this.error = true; this.loading = false; }
    });
  }

  editStudent(): void {
    this.router.navigate(['/students', this.student!.id, 'edit']);
  }

  goBack(): void {
    this.router.navigate(['/students']);
  }

  isMinor(): boolean {
    if (!this.student?.birthDate) return false;
    const birth = new Date(this.student.birthDate);
    const age   = new Date().getFullYear() - birth.getFullYear();
    return age < 18;
  }

  hasDiscount(): boolean {
    return !!this.student && this.student.discountAmount > 0;
  }
}
