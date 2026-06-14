import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProductCategory } from '../../common/product-category';
import { ProductService } from '../../services/product.service';

@Component({
  selector: 'app-product-category-menu',
  imports: [RouterLink],
  templateUrl: './product-category-menu.html'
})
export class ProductCategoryMenuComponent implements OnInit {

  productCategories: ProductCategory[] = [];

  constructor(private productService: ProductService) {}

  ngOnInit(): void {
    this.productService.getProductCategories().subscribe(data => {
      this.productCategories = data;
    });
  }
}
