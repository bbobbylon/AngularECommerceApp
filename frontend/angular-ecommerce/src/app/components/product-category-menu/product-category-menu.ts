import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { ProductCategory } from '../../common/product-category';
import { ProductService } from '../../services/product.service';

@Component({
  selector: 'app-product-category-menu',
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './product-category-menu.html',
})
export class ProductCategoryMenu implements OnInit {

  productCategories: ProductCategory[] = [];

  constructor(private productService: ProductService) {}

  ngOnInit(): void {
    this.productService
      .getProductCategories()
      .subscribe(data => (this.productCategories = data));
  }

  iconFor(categoryName: string): string {
    const icons: Record<string, string> = {
      'Books': 'fa-book',
      'Coffee Mugs': 'fa-mug-hot',
      'Mouse Pads': 'fa-computer-mouse',
      'Luggage': 'fa-suitcase-rolling',
    };
    return icons[categoryName] ?? 'fa-tag';
  }
}
