import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'google/callback',
    loadComponent: () => import('./pages/google-callback/google-callback.component').then(m => m.GoogleCallbackComponent)
  },
  {
    path: 'github/callback',
    loadComponent: () => import('./pages/github-callback/github-callback.component').then(m => m.GitHubCallbackComponent)
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./components/layout/layout.component').then(m => m.LayoutComponent),
    children: [
      { path: 'dashboard', loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'jobs', loadComponent: () => import('./pages/jobs/job-list/job-list.component').then(m => m.JobListComponent) },
      { path: 'jobs/create', loadComponent: () => import('./pages/jobs/job-create/job-create.component').then(m => m.JobCreateComponent) },
      { path: 'jobs/:id', loadComponent: () => import('./pages/jobs/job-detail/job-detail.component').then(m => m.JobDetailComponent) },
      { path: 'dead-letter', loadComponent: () => import('./pages/dead-letter/dead-letter.component').then(m => m.DeadLetterComponent) },
      { path: 'profile', loadComponent: () => import('./pages/profile/profile.component').then(m => m.ProfileComponent) },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: 'login' }
];
