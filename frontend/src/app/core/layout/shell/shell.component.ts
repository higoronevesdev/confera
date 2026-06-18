import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map, startWith } from 'rxjs';
import { ThemeService } from '../../services/theme.service';
import { AuthService } from '../../services/auth.service';
import { ToastComponent } from '../../../shared/components/toast/toast.component';

interface NavItem {
  path: string;
  label: string;
  exact: boolean;
  icon: string;
}

@Component({
  selector: 'app-shell',
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ToastComponent]
})
export class ShellComponent {
  private readonly router = inject(Router);
  readonly themeService   = inject(ThemeService);
  readonly authService    = inject(AuthService);

  readonly collapsed = signal(false);
  readonly isDark    = computed(() => this.themeService.theme() === 'dark');

  private readonly titleMap: Record<string, string> = {
    '/':               'Dashboard',
    '/imports':        'Importação',
    '/reconciliation': 'Conciliação',
    '/discrepancies':  'Divergências',
    '/ledger':         'Ledger',
  };

  readonly pageTitle = toSignal(
    this.router.events.pipe(
      filter(e => e instanceof NavigationEnd),
      map(e => this.titleMap[(e as NavigationEnd).urlAfterRedirects] ?? 'Confera'),
      startWith(this.titleMap[this.router.url] ?? 'Dashboard')
    ),
    { initialValue: 'Dashboard' }
  );

  readonly navItems: NavItem[] = [
    { path: '/',               label: 'Dashboard',   exact: true,  icon: 'home'           },
    { path: '/imports',        label: 'Importação',  exact: false, icon: 'upload'         },
    { path: '/reconciliation', label: 'Conciliação', exact: false, icon: 'reconciliation' },
    { path: '/discrepancies',  label: 'Divergências',exact: false, icon: 'warning'        },
    { path: '/ledger',         label: 'Ledger',      exact: false, icon: 'ledger'         },
  ];

  toggleCollapse(): void { this.collapsed.update(v => !v); }
  toggleTheme(): void    { this.themeService.toggle(); }
  logout(): void         { this.authService.logout(); }
}