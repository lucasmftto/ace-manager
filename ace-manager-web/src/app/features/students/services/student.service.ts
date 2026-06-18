import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Page } from '../../../shared/models/page.model';
import {
  CreateStudentRequest,
  StudentDetail,
  StudentSummary,
  UpdateStudentRequest
} from '../models/student.model';
import { StudentFilterParams } from '../models/student-filter.model';

@Injectable({ providedIn: 'root' })
export class StudentService {
  private readonly http    = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/students`;

  findAll(filters: StudentFilterParams = {}): Observable<Page<StudentSummary>> {
    let params = new HttpParams();

    if (filters.status)  params = params.set('status', filters.status);
    if (filters.search)  params = params.set('search', filters.search);
    if (filters.page != null) params = params.set('page', filters.page);
    if (filters.size != null) params = params.set('size', filters.size);
    if (filters.sort)    params = params.set('sort', filters.sort);

    return this.http.get<Page<StudentSummary>>(this.baseUrl, { params });
  }

  findById(id: string): Observable<StudentDetail> {
    return this.http.get<StudentDetail>(`${this.baseUrl}/${id}`);
  }

  create(request: CreateStudentRequest): Observable<StudentDetail> {
    return this.http.post<StudentDetail>(this.baseUrl, request);
  }

  update(id: string, request: UpdateStudentRequest): Observable<StudentDetail> {
    return this.http.put<StudentDetail>(`${this.baseUrl}/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
