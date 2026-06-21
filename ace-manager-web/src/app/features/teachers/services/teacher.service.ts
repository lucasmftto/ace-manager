import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page } from '../../../shared/models/page.model';
import {
  TeacherSummary, TeacherDetail,
  CreateTeacherRequest, UpdateTeacherRequest,
  StudentPayoutConfig, UpsertStudentConfigRequest
} from '../models/teacher.model';

@Injectable({ providedIn: 'root' })
export class TeacherService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/teachers';

  findAll(page = 0, size = 20): Observable<Page<TeacherSummary>> {
    return this.http.get<Page<TeacherSummary>>(this.base, { params: { page, size, sort: 'name' } });
  }

  findById(id: string): Observable<TeacherDetail> {
    return this.http.get<TeacherDetail>(`${this.base}/${id}`);
  }

  create(request: CreateTeacherRequest): Observable<TeacherDetail> {
    return this.http.post<TeacherDetail>(this.base, request);
  }

  update(id: string, request: UpdateTeacherRequest): Observable<TeacherDetail> {
    return this.http.put<TeacherDetail>(`${this.base}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  findStudentConfigs(teacherId: string): Observable<StudentPayoutConfig[]> {
    return this.http.get<StudentPayoutConfig[]>(`${this.base}/${teacherId}/student-configs`);
  }

  upsertStudentConfig(teacherId: string, studentId: string, request: UpsertStudentConfigRequest): Observable<StudentPayoutConfig> {
    return this.http.put<StudentPayoutConfig>(`${this.base}/${teacherId}/student-configs/${studentId}`, request);
  }

  removeStudentConfig(teacherId: string, studentId: string): Observable<void> {
    return this.http.delete<void>(`${this.base}/${teacherId}/student-configs/${studentId}`);
  }
}
