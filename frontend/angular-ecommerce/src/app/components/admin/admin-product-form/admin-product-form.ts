import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { ProductCategory } from '../../../common/product-category';
import { AdminProductPayload, AdminService } from '../../../services/admin.service';
import { ToastService } from '../../../services/toast.service';

@Component({
  selector: 'app-admin-product-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './admin-product-form.html',
})
export class AdminProductForm implements OnInit {

  form!: FormGroup;
  categories: ProductCategory[] = [];
  editId: number | null = null;

  readonly saving = signal(false);
  readonly loading = signal(false);

  private fb = inject(FormBuilder);
  private admin = inject(AdminService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private toast = inject(ToastService);

  get isEdit(): boolean {
    return this.editId !== null;
  }

  ngOnInit(): void {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      sku: ['', Validators.required],
      description: [''],
      unitPrice: [null, [Validators.required, Validators.min(0)]],
      originalPrice: [null],
      imageUrl: [''],
      additionalImages: [''],
      unitsInStock: [0, [Validators.required, Validators.min(0)]],
      active: [true],
      categoryId: [null, Validators.required],
    });

    this.admin.getCategories().subscribe(cats => (this.categories = cats));

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editId = Number(id);
      this.loading.set(true);
      this.admin.getProduct(this.editId).subscribe({
        next: product => {
          this.form.patchValue({
            name: product.name,
            sku: product.sku,
            description: product.description,
            unitPrice: product.unitPrice,
            originalPrice: product.originalPrice ?? null,
            imageUrl: product.imageUrl,
            additionalImages: (product.additionalImages ?? []).join('\n'),
            unitsInStock: product.unitsInStock,
            active: product.active,
            categoryId: product.category?.id ?? null,
          });
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
          this.toast.error('Could not load that product.');
          this.router.navigate(['/admin/products']);
        },
      });
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.value;
    const payload: AdminProductPayload = {
      name: value.name,
      sku: value.sku,
      description: value.description ?? '',
      unitPrice: Number(value.unitPrice),
      originalPrice: value.originalPrice ? Number(value.originalPrice) : null,
      imageUrl: value.imageUrl ?? '',
      additionalImages: this.parseImageLines(value.additionalImages),
      unitsInStock: Number(value.unitsInStock),
      active: !!value.active,
      categoryId: Number(value.categoryId),
    };

    this.saving.set(true);
    const request$ = this.isEdit
      ? this.admin.updateProduct(this.editId!, payload)
      : this.admin.createProduct(payload);

    request$.subscribe({
      next: () => {
        this.toast.success(this.isEdit ? 'Product updated' : 'Product created');
        this.router.navigate(['/admin/products']);
      },
      error: () => {
        this.saving.set(false);
        this.toast.error('Could not save the product.');
      },
    });
  }

  /** Splits the gallery textarea (one URL per line) into a trimmed, blank-free list. */
  private parseImageLines(raw: unknown): string[] {
    return String(raw ?? '')
      .split('\n')
      .map(line => line.trim())
      .filter(line => line.length > 0);
  }

  // template validation helpers
  get name() { return this.form.get('name'); }
  get sku() { return this.form.get('sku'); }
  get unitPrice() { return this.form.get('unitPrice'); }
  get unitsInStock() { return this.form.get('unitsInStock'); }
  get categoryId() { return this.form.get('categoryId'); }
}
