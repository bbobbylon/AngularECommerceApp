import { CurrencyPipe } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap';
import { Product } from '../../common/product';
import { ProductService } from '../../services/product.service';

@Component({
  selector: 'app-product-list',
  imports: [CurrencyPipe, RouterLink, NgbPaginationModule],
  templateUrl: './product-list.html'
})
export class ProductListComponent implements OnInit {

  products: Product[] = [];
  currentCategoryId = 1;
  previousCategoryId = 1;
  searchMode = false;
  previousKeyword = '';

  // pagination (NgbPagination is 1-based; Spring Data REST is 0-based)
  pageNumber = 1;
  pageSize = 10;
  totalElements = 0;

  constructor(
    private productService: ProductService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(() => this.listProducts());
  }

  listProducts(): void {
    this.searchMode = this.route.snapshot.paramMap.has('keyword');
    if (this.searchMode) {
      this.handleSearchProducts();
    } else {
      this.handleListProducts();
    }
  }

  private handleListProducts(): void {
    const hasCategoryId = this.route.snapshot.paramMap.has('id');
    this.currentCategoryId = hasCategoryId
      ? Number(this.route.snapshot.paramMap.get('id'))
      : 1;

    // reset to first page whenever the category changes
    if (this.previousCategoryId !== this.currentCategoryId) {
      this.pageNumber = 1;
    }
    this.previousCategoryId = this.currentCategoryId;

    this.productService
      .getProductListPaginate(this.pageNumber - 1, this.pageSize, this.currentCategoryId)
      .subscribe(this.processResult());
  }

  private handleSearchProducts(): void {
    const keyword = this.route.snapshot.paramMap.get('keyword')!;

    // reset to first page whenever the keyword changes
    if (this.previousKeyword !== keyword) {
      this.pageNumber = 1;
    }
    this.previousKeyword = keyword;

    this.productService
      .searchProductsPaginate(this.pageNumber - 1, this.pageSize, keyword)
      .subscribe(this.processResult());
  }

  private processResult() {
    return (data: any) => {
      this.products = data._embedded.products;
      this.pageNumber = data.page.number + 1;
      this.pageSize = data.page.size;
      this.totalElements = data.page.totalElements;
    };
  }

  updatePageSize(value: string): void {
    this.pageSize = Number(value);
    this.pageNumber = 1;
    this.listProducts();
  }
}
