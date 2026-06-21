import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page } from '../../../shared/models/page.model';
import {
  ClassScheduleSummary, ClassScheduleDetail,
  ClassOccurrenceSummary, ClassOccurrenceDetail,
  CreateClassScheduleRequest, UpdateClassScheduleRequest,
  AddStudentRequest, GenerateOccurrencesRequest, GenerateOccurrencesResponse,
  UpdateAttendanceRequest, OccurrenceStatus
} from '../models/class.model';

@Injectable({ providedIn: 'root' })
export class ClassService {
  private readonly http = inject(HttpClient);

  findSchedules(page = 0, size = 100): Observable<Page<ClassScheduleSummary>> {
    return this.http.get<Page<ClassScheduleSummary>>('/api/v1/class-schedules', { params: { page, size } });
  }

  findScheduleById(id: string): Observable<ClassScheduleDetail> {
    return this.http.get<ClassScheduleDetail>(`/api/v1/class-schedules/${id}`);
  }

  createSchedule(req: CreateClassScheduleRequest): Observable<ClassScheduleSummary> {
    return this.http.post<ClassScheduleSummary>('/api/v1/class-schedules', req);
  }

  updateSchedule(id: string, req: UpdateClassScheduleRequest): Observable<ClassScheduleDetail> {
    return this.http.put<ClassScheduleDetail>(`/api/v1/class-schedules/${id}`, req);
  }

  deactivateSchedule(id: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/class-schedules/${id}`);
  }

  addStudent(scheduleId: string, req: AddStudentRequest): Observable<ClassScheduleDetail> {
    return this.http.post<ClassScheduleDetail>(`/api/v1/class-schedules/${scheduleId}/students`, req);
  }

  removeStudent(scheduleId: string, studentId: string): Observable<ClassScheduleDetail> {
    return this.http.delete<ClassScheduleDetail>(`/api/v1/class-schedules/${scheduleId}/students/${studentId}`);
  }

  findOccurrences(params: {
    dateFrom?: string; dateTo?: string;
    teacherId?: string; studentId?: string; status?: OccurrenceStatus
  }): Observable<ClassOccurrenceSummary[]> {
    const p: Record<string, string> = {};
    if (params.dateFrom)  p['dateFrom']  = params.dateFrom;
    if (params.dateTo)    p['dateTo']    = params.dateTo;
    if (params.teacherId) p['teacherId'] = params.teacherId;
    if (params.studentId) p['studentId'] = params.studentId;
    if (params.status)    p['status']    = params.status;
    return this.http.get<ClassOccurrenceSummary[]>('/api/v1/class-occurrences', { params: p });
  }

  findOccurrenceById(id: string): Observable<ClassOccurrenceDetail> {
    return this.http.get<ClassOccurrenceDetail>(`/api/v1/class-occurrences/${id}`);
  }

  generateOccurrences(req: GenerateOccurrencesRequest): Observable<GenerateOccurrencesResponse> {
    return this.http.post<GenerateOccurrencesResponse>('/api/v1/class-occurrences/generate', req);
  }

  substituteTeacher(occurrenceId: string, teacherId: string): Observable<ClassOccurrenceDetail> {
    return this.http.patch<ClassOccurrenceDetail>(`/api/v1/class-occurrences/${occurrenceId}/teacher`, { teacherId });
  }

  cancelOccurrence(occurrenceId: string): Observable<ClassOccurrenceDetail> {
    return this.http.patch<ClassOccurrenceDetail>(`/api/v1/class-occurrences/${occurrenceId}/cancel`, {});
  }

  updateAttendance(occurrenceId: string, req: UpdateAttendanceRequest): Observable<ClassOccurrenceDetail> {
    return this.http.put<ClassOccurrenceDetail>(`/api/v1/class-occurrences/${occurrenceId}/attendance`, req);
  }
}
