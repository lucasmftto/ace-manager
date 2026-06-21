export type ClassType = 'INDIVIDUAL' | 'GROUP';
export type ClassScheduleStatus = 'ACTIVE' | 'INACTIVE';
export type OccurrenceStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED';
export type AttendanceStatus = 'PRESENT' | 'ABSENT' | 'JUSTIFIED_ABSENCE';
export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export interface ClassScheduleSummary {
  id: string;
  name: string;
  dayOfWeek: DayOfWeek;
  startTime: string;
  durationMinutes: number;
  teacherId: string;
  teacherName: string;
  type: ClassType;
  maxStudents: number;
  enrolledCount: number;
  status: ClassScheduleStatus;
}

export interface ScheduleStudentResponse {
  id: string;
  studentId: string;
  studentName: string;
  studentPlanId: string | null;
  studentPlanName: string | null;
}

export interface ClassScheduleDetail extends ClassScheduleSummary {
  enrolledStudents: ScheduleStudentResponse[];
}

export interface AttendanceRecord {
  id: string;
  studentId: string;
  studentName: string;
  status: AttendanceStatus;
  studentBilledValue: number | null;
  teacherPayoutValue: number | null;
}

export interface ClassOccurrenceSummary {
  id: string;
  scheduleId: string;
  scheduleName: string;
  occurrenceDate: string;
  startTime: string;
  durationMinutes: number;
  teacherId: string;
  teacherName: string;
  status: OccurrenceStatus;
  attendanceCount: number;
}

export interface ClassOccurrenceDetail extends ClassOccurrenceSummary {
  attendances: AttendanceRecord[];
}

export interface CreateClassScheduleRequest {
  name: string;
  dayOfWeek: DayOfWeek;
  startTime: string;
  durationMinutes: number;
  teacherId: string;
  type: ClassType;
  maxStudents: number | null;
}

export interface UpdateClassScheduleRequest extends CreateClassScheduleRequest {
  status: ClassScheduleStatus;
}

export interface AddStudentRequest {
  studentId: string;
  studentPlanId: string | null;
}

export interface GenerateOccurrencesRequest {
  scheduleId: string | null;
  fromDate: string;
  toDate: string;
}

export interface GenerateOccurrencesResponse {
  generated: number;
  skipped: number;
}

export interface UpdateAttendanceRequest {
  attendances: { studentId: string; status: AttendanceStatus }[];
}

export const DAY_OF_WEEK_LABELS: Record<string, string> = {
  MONDAY: 'Segunda', TUESDAY: 'Terça', WEDNESDAY: 'Quarta',
  THURSDAY: 'Quinta', FRIDAY: 'Sexta', SATURDAY: 'Sábado', SUNDAY: 'Domingo'
};

export const DAY_OF_WEEK_ORDER: DayOfWeek[] =
  ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

export const OCCURRENCE_STATUS_LABELS: Record<string, string> = {
  SCHEDULED: 'Agendada', COMPLETED: 'Concluída', CANCELLED: 'Cancelada'
};

export const ATTENDANCE_STATUS_LABELS: Record<string, string> = {
  PRESENT: 'Presente', ABSENT: 'Ausente', JUSTIFIED_ABSENCE: 'Falta justificada'
};

export const CLASS_TYPE_LABELS: Record<string, string> = {
  INDIVIDUAL: 'Individual', GROUP: 'Turma'
};
