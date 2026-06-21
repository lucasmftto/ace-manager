export type PlanType = 'MONTHLY' | 'BUNDLE' | 'DROP_IN' | 'GROUP';
export type PlanStatus = 'ACTIVE' | 'INACTIVE';
export type StudentPlanStatus = 'ACTIVE' | 'SUSPENDED' | 'COMPLETED' | 'CANCELLED';

export interface Plan {
  id: string;
  name: string;
  description: string | null;
  type: PlanType;
  referencePrice: number;
  weeklyClassCount: number | null;
  billingDayOfMonth: number | null;
  totalClasses: number | null;
  maxStudents: number | null;
  activeEnrollments: number;
  status: PlanStatus;
}

export interface StudentPlan {
  id: string;
  planId: string;
  planName: string;
  planType: PlanType;
  teacherId: string | null;
  teacherName: string | null;
  referencePrice: number;
  billedValue: number;
  discountAmount: number;
  startDate: string;
  endDate: string | null;
  remainingClasses: number | null;
  lowClassesAlert: boolean;
  status: StudentPlanStatus;
}

export interface CreatePlanRequest {
  name: string;
  description: string | null;
  type: PlanType;
  referencePrice: number;
  weeklyClassCount: number | null;
  billingDayOfMonth: number | null;
  totalClasses: number | null;
  maxStudents: number | null;
}

export interface UpdatePlanRequest extends CreatePlanRequest {
  status: PlanStatus;
}

export interface EnrollStudentRequest {
  planId: string;
  teacherId: string | null;
  billedValue: number;
  startDate: string;
}

export interface UpdateEnrollmentRequest {
  billedValue: number | null;
  status: StudentPlanStatus | null;
}

export const PLAN_TYPE_LABELS: Record<string, string> = {
  MONTHLY: 'Mensalidade',
  BUNDLE:  'Pacote de aulas',
  DROP_IN: 'Aula avulsa',
  GROUP:   'Turma'
};

export const PLAN_STATUS_LABELS: Record<string, string> = {
  ACTIVE:   'Ativo',
  INACTIVE: 'Inativo'
};

export const STUDENT_PLAN_STATUS_LABELS: Record<string, string> = {
  ACTIVE:    'Ativo',
  SUSPENDED: 'Suspenso',
  COMPLETED: 'Concluído',
  CANCELLED: 'Cancelado'
};
