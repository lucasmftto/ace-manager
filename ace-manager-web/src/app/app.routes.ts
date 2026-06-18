import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'students',
    pathMatch: 'full'
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./core/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'students',
    canActivate: [authGuard],
    loadChildren: () =>
      import('./features/students/students.routes').then(m => m.STUDENTS_ROUTES)
  },
  {
    path: '**',
    redirectTo: 'students'
  }
];
