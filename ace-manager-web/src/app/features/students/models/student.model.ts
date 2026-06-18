export type PaymentMethod = 'PIX' | 'BOLETO' | 'PIX_AUTOMATIC' | 'CREDIT_CARD' | 'OTHER';
export type StudentStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
export type FinancialStatus = 'PENDING' | 'PAID' | 'OVERDUE' | null;

export interface StudentSummary {
  id: string;
  name: string;
  phone: string | null;
  currentMonthlyValue: number | null;
  preferredPaymentMethod: PaymentMethod;
  status: StudentStatus;
  financialStatus: FinancialStatus;
}

export interface StudentDetail {
  id: string;
  name: string;
  phone: string | null;
  email: string | null;
  birthDate: string | null;
  guardianName: string | null;
  guardianPhone: string | null;
  agreedMonthlyValue: number;
  currentMonthlyValue: number;
  discountAmount: number;
  discountPercentage: number;
  preferredPaymentMethod: PaymentMethod;
  status: StudentStatus;
  notes: string | null;
  createdAt: string;
}

export interface CreateStudentRequest {
  name: string;
  phone: string | null;
  email: string | null;
  birthDate: string | null;
  guardianName: string | null;
  guardianPhone: string | null;
  agreedMonthlyValue: number;
  currentMonthlyValue: number;
  preferredPaymentMethod: PaymentMethod;
  notes: string | null;
}

export interface UpdateStudentRequest extends CreateStudentRequest {
  status: StudentStatus;
}

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  PIX: 'PIX',
  BOLETO: 'Boleto',
  PIX_AUTOMATIC: 'PIX Automático',
  CREDIT_CARD: 'Cartão de Crédito',
  OTHER: 'Outro'
};

export const STUDENT_STATUS_LABELS: Record<StudentStatus, string> = {
  ACTIVE: 'Ativo',
  INACTIVE: 'Inativo',
  SUSPENDED: 'Suspenso'
};
