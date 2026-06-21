import { Routes } from '@angular/router';

export const TEACHERS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./teachers-list/teachers-list.component').then(m => m.TeachersListComponent)
  },
  {
    path: 'new',
    loadComponent: () =>
      import('./teacher-form/teacher-form.component').then(m => m.TeacherFormComponent)
  },
  {
    path: ':id/edit',
    loadComponent: () =>
      import('./teacher-form/teacher-form.component').then(m => m.TeacherFormComponent)
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./teacher-detail/teacher-detail.component').then(m => m.TeacherDetailComponent)
  }
];
