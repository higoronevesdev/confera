import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { ToastComponent } from '../../../shared/components/toast/toast.component';

@Component({
  selector: 'app-login',
  templateUrl: './login.page.html',
  styleUrl: './login.page.scss',
  imports: [ReactiveFormsModule, ToastComponent]
})
export class LoginPage {
  private readonly auth    = inject(AuthService);
  private readonly router  = inject(Router);
  private readonly toast   = inject(ToastService);
  private readonly fb      = inject(FormBuilder);

  readonly loading = signal(false);

  readonly form = this.fb.group({
    email:    ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  submit(): void {
    if (this.form.invalid || this.loading()) return;
    const { email, password } = this.form.getRawValue();
    this.loading.set(true);

    this.auth.login(email!, password!).subscribe({
      next: () => this.router.navigate(['/']),
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        const msg = err.status === 401
          ? 'Credenciais inválidas. Verifique e-mail e senha.'
          : 'Falha ao conectar. Tente novamente.';
        this.toast.show(msg, 'error');
      },
    });
  }
}