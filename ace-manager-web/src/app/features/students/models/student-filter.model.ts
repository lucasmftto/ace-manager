import { StudentStatus } from './student.model';

export interface StudentFilterParams {
  status?: StudentStatus;
  search?: string;
  page?: number;
  size?: number;
  sort?: string;
}
