import { Component, inject, ChangeDetectionStrategy } from '@angular/core';

import { ToastService, ToastType } from '../../services/toast.service';

/** Renders `ToastService`'s active notification queue. Mounted once in `App`'s global chrome. */
@Component({
  selector: 'app-toast',
  imports: [],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './toast.html',
})
export class Toast {
  protected readonly toastService = inject(ToastService);

  iconFor(type: ToastType): string {
    switch (type) {
      case 'success':
        return 'fa-circle-check';
      case 'error':
        return 'fa-circle-exclamation';
      default:
        return 'fa-circle-info';
    }
  }
}
