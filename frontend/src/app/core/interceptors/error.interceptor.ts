import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../services/toast.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(ToastService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401) {
        const msg =
          error.status === 0
            ? 'Sem conexão com o servidor. Verifique a rede.'
            : (error.error?.message ?? `Erro ${error.status}: algo deu errado.`);
        toast.show(msg, 'error');
        console.error('[HTTP Error]', error);
      }
      return throwError(() => error);
    })
  );
};