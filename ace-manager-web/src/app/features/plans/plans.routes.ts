import { Routes } from '@angular/router';

export const PLANS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./plans-list/plans-list.component').then(m => m.PlansListComponent)
  },
  {
    path: 'new',
    loadComponent: () =>
      import('./plan-form/plan-form.component').then(m => m.PlanFormComponent)
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./plan-detail/plan-detail.component').then(m => m.PlanDetailComponent)
  },
  {
    path: ':id/edit',
    loadComponent: () =>
      import('./plan-form/plan-form.component').then(m => m.PlanFormComponent)
  }
];
