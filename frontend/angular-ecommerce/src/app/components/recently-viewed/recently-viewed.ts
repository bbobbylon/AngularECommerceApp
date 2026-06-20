import { CommonModule } from '@angular/common';
import { MoneyPipe } from '../../common/money.pipe';
import { Component, computed, inject, input } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';

import { Product, discountPercent, isOnSale } from '../../common/product';
import { ProductService } from '../../services/product.service';
import { RecentlyViewedService } from '../../services/recently-viewed.service';

/**
 * Horizontal "Recently viewed" strip. Reactively follows the RecentlyViewedService and resolves the
 * stored ids to products. Pass `excludeId` to drop the product currently being viewed.
 */
@Component({
  selector: 'app-recently-viewed',
  imports: [CommonModule, MoneyPipe, RouterLink],
  templateUrl: './recently-viewed.html',
})
export class RecentlyViewed {

  readonly excludeId = input<number>();
  readonly heading = input('Recently viewed');

  private recentlyViewed = inject(RecentlyViewedService);
  private productService = inject(ProductService);

  protected readonly isOnSale = isOnSale;
  protected readonly discountPercent = discountPercent;

  private readonly ids = computed(() =>
    this.recentlyViewed.ids().filter(id => id !== this.excludeId()).slice(0, 8));

  readonly products = toSignal(
    toObservable(this.ids).pipe(switchMap(ids => this.productService.getProductsByIds(ids))),
    { initialValue: [] as Product[] },
  );
}
