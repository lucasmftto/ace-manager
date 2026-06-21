import { Routes } from '@angular/router';

export const CLASSES_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./weekly-calendar/weekly-calendar.component').then(m => m.WeeklyCalendarComponent)
  },
  {
    path: 'schedules',
    loadComponent: () =>
      import('./schedule-list/schedule-list.component').then(m => m.ScheduleListComponent)
  },
  {
    path: 'schedules/new',
    loadComponent: () =>
      import('./schedule-form/schedule-form.component').then(m => m.ScheduleFormComponent)
  },
  {
    path: 'schedules/:id',
    loadComponent: () =>
      import('./schedule-detail/schedule-detail.component').then(m => m.ScheduleDetailComponent)
  },
  {
    path: 'schedules/:id/edit',
    loadComponent: () =>
      import('./schedule-form/schedule-form.component').then(m => m.ScheduleFormComponent)
  },
  {
    path: 'occurrences/:id',
    loadComponent: () =>
      import('./class-detail/class-detail.component').then(m => m.ClassDetailComponent)
  }
];
