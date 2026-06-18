import { Routes } from '@angular/router';
import { ShellComponent } from './core/layout/shell/shell.component';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Entrar',
    loadComponent: () => import('./features/auth/login/login.page').then(m => m.LoginPage),
  },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      {
        path: '',
        title: 'Dashboard',
        loadComponent: () => import('./pages/dashboard/dashboard.page').then(m => m.DashboardPage),
      },
      {
        path: 'imports',
        title: 'Importação',
        loadComponent: () => import('./pages/imports/imports.page').then(m => m.ImportsPage),
      },
      {
        path: 'reconciliation',
        title: 'Conciliação',
        loadComponent: () => import('./pages/reconciliation/reconciliation.page').then(m => m.ReconciliationPage),
      },
      {
        path: 'discrepancies',
        title: 'Divergências',
        loadComponent: () => import('./pages/discrepancies/discrepancies.page').then(m => m.DiscrepanciesPage),
      },
      {
        path: 'ledger',
        title: 'Ledger',
        loadComponent: () => import('./pages/ledger/ledger.page').then(m => m.LedgerPage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];