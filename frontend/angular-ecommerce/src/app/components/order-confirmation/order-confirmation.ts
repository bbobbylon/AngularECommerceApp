import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

@Component({
  selector: 'app-order-confirmation',
  imports: [RouterLink],
  templateUrl: './order-confirmation.html',
})
export class OrderConfirmation {
  trackingNumber = inject(ActivatedRoute).snapshot.paramMap.get('trackingNumber') ?? '';
}
