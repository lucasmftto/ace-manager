import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page } from '../../../shared/models/page.model';
import {
  Plan, StudentPlan,
  CreatePlanRequest, UpdatePlanRequest,
  EnrollStudentRequest, UpdateEnrollmentRequest
} from '../models/plan.model';

@Injectable({ providedIn: 'root' })
export class PlanService {
  private readonly http = inject(HttpClient);

  findAll(page = 0, size = 50): Observable<Page<Plan>> {
    return this.http.get<Page<Plan>>('/api/v1/plans', { params: { page, size, sort: 'name' } });
  }

  findById(id: string): Observable<Plan> {
    return this.http.get<Plan>(`/api/v1/plans/${id}`);
  }

  create(request: CreatePlanRequest): Observable<Plan> {
    return this.http.post<Plan>('/api/v1/plans', request);
  }

  update(id: string, request: UpdatePlanRequest): Observable<Plan> {
    return this.http.put<Plan>(`/api/v1/plans/${id}`, request);
  }

  deactivate(id: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/plans/${id}`);
  }

  findEnrollments(studentId: string): Observable<StudentPlan[]> {
    return this.http.get<StudentPlan[]>(`/api/v1/students/${studentId}/plans`);
  }

  enroll(studentId: string, request: EnrollStudentRequest): Observable<StudentPlan> {
    return this.http.post<StudentPlan>(`/api/v1/students/${studentId}/plans`, request);
  }

  updateEnrollment(studentId: string, enrollmentId: string, request: UpdateEnrollmentRequest): Observable<StudentPlan> {
    return this.http.patch<StudentPlan>(`/api/v1/students/${studentId}/plans/${enrollmentId}`, request);
  }

  cancelEnrollment(studentId: string, enrollmentId: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/students/${studentId}/plans/${enrollmentId}`);
  }
}
