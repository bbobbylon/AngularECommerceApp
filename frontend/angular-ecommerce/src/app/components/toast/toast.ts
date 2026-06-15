import { Component, inject } from '@angular/core';

import { ToastService, ToastType } from '../../services/toast.service';

@Component({
  selector: 'app-toast',
  imports: [],
  templateUrl: './toast.html',
})
export class Toast {

  protected readonly toastService = inject(ToastService);

  iconFor(type: ToastType): string {
    switch (type) {
      case 'success': return 'fa-circle-check';
      case 'error': return 'fa-circle-exclamation';
      default: return 'fa-circle-info';
    }
  }
}
