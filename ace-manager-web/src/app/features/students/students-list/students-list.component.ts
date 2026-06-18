import { Component, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSortModule, MatSort, Sort } from '@angular/material/sort';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { StudentService } from '../services/student.service';
import {
  StudentSummary,
  StudentStatus,
  PAYMENT_METHOD_LABELS,
  STUDENT_STATUS_LABELS
} from '../models/student.model';
import { StudentFilterParams } from '../models/student-filter.model';
import { FormatPhonePipe } from '../../../shared/pipes/format-phone.pipe';

@Component({
  selector: 'app-students-list',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatTableModule, MatPaginatorModule, MatSortModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatChipsModule, MatTooltipModule,
    FormatPhonePipe
  ],
  templateUrl: './students-list.component.html'
})
export class StudentsListComponent implements OnInit {
  private readonly studentService = inject(StudentService);
  private readonly router         = inject(Router);

  readonly paymentLabels: Record<string, string>  = PAYMENT_METHOD_LABELS;
  readonly statusLabels: Record<string, string>   = STUDENT_STATUS_LABELS;
  readonly statusOptions: StudentStatus[] = ['ACTIVE', 'INACTIVE', 'SUSPENDED'];

  displayedColumns = ['name', 'phone', 'currentMonthlyValue', 'preferredPaymentMethod', 'status', 'financialStatus', 'actions'];

  dataSource = new MatTableDataSource<StudentSummary>();
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;
  sortField = 'name';
  sortDir   = 'asc';
  loading   = false;

  searchControl  = new FormControl('');
  statusControl  = new FormControl<StudentStatus | ''>('');

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  ngOnInit(): void {
    this.loadStudents();

    this.searchControl.valueChanges.pipe(
      debounceTime(400),
      distinctUntilChanged()
    ).subscribe(() => {
      this.pageIndex = 0;
      this.loadStudents();
    });

    this.statusControl.valueChanges.subscribe(() => {
      this.pageIndex = 0;
      this.loadStudents();
    });
  }

  loadStudents(): void {
    this.loading = true;
    const filters: StudentFilterParams = {
      page: this.pageIndex,
      size: this.pageSize,
      sort: `${this.sortField},${this.sortDir}`
    };

    const search = this.searchControl.value?.trim();
    if (search) filters.search = search;

    const status = this.statusControl.value;
    if (status) filters.status = status;

    this.studentService.findAll(filters).subscribe({
      next: page => {
        this.dataSource.data = page.content;
        this.totalElements   = page.page.totalElements;
        this.loading         = false;
      },
      error: () => { this.loading = false; }
    });
  }

  onPage(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize  = event.pageSize;
    this.loadStudents();
  }

  onSort(event: Sort): void {
    this.sortField = event.active || 'name';
    this.sortDir   = event.direction || 'asc';
    this.pageIndex = 0;
    this.loadStudents();
  }

  openDetail(student: StudentSummary): void {
    this.router.navigate(['/students', student.id]);
  }

  openCreate(): void {
    this.router.navigate(['/students/new']);
  }

  openEdit(student: StudentSummary, event: Event): void {
    event.stopPropagation();
    this.router.navigate(['/students', student.id, 'edit']);
  }

  delete(student: StudentSummary, event: Event): void {
    event.stopPropagation();
    if (!confirm(`Excluir ${student.name}?`)) return;

    this.studentService.delete(student.id).subscribe({
      next: () => this.loadStudents()
    });
  }

  clearFilters(): void {
    this.searchControl.setValue('');
    this.statusControl.setValue('');
  }

  financialStatusColor(status: string | null): string {
    if (status === 'PAID') return 'accent';
    if (status === 'OVERDUE') return 'warn';
    return 'primary';
  }

  hasDiscount(student: StudentSummary): boolean {
    return false;
  }
}
