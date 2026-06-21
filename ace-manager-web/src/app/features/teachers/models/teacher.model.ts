export type PayoutModel = 'PERCENTAGE' | 'HOURLY_RATE';
export type TeacherStatus = 'ACTIVE' | 'INACTIVE';

export interface TeacherSummary {
  id: string;
  name: string;
  phone: string | null;
  payoutModel: PayoutModel;
  defaultPercentage: number | null;
  defaultHourlyRate: number | null;
  studentConfigCount: number;
  status: TeacherStatus;
}

export interface TeacherDetail {
  id: string;
  name: string;
  phone: string | null;
  email: string | null;
  payoutModel: PayoutModel;
  defaultPercentage: number | null;
  defaultHourlyRate: number | null;
  status: TeacherStatus;
  studentConfigs: StudentPayoutConfig[];
}

export interface StudentPayoutConfig {
  studentId: string;
  studentName: string;
  effectivePercentage: number | null;
  effectiveHourlyRate: number | null;
  isOverride: boolean;
}

export interface CreateTeacherRequest {
  name: string;
  phone: string | null;
  email: string | null;
  payoutModel: PayoutModel;
  defaultPercentage: number | null;
  defaultHourlyRate: number | null;
}

export interface UpdateTeacherRequest extends CreateTeacherRequest {
  status: TeacherStatus;
}

export interface UpsertStudentConfigRequest {
  overridePercentage: number | null;
  overrideHourlyRate: number | null;
}

export const PAYOUT_MODEL_LABELS: Record<string, string> = {
  PERCENTAGE: 'Porcentagem',
  HOURLY_RATE: 'Valor por hora'
};

export const TEACHER_STATUS_LABELS: Record<string, string> = {
  ACTIVE: 'Ativo',
  INACTIVE: 'Inativo'
};
