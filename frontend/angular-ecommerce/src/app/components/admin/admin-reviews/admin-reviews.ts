import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap';

import { AdminService } from '../../../services/admin.service';
import { Review } from '../../../services/review.service';
import { ToastService } from '../../../services/toast.service';
import { StarRating } from '../../star-rating/star-rating';

@Component({
  selector: 'app-admin-reviews',
  imports: [CommonModule, NgbPaginationModule, StarRating],
  templateUrl: './admin-reviews.html',
})
export class AdminReviews implements OnInit {

  readonly reviews = signal<Review[]>([]);
  readonly loading = signal(true);

  pageNumber = 1;
  pageSize = 20;
  totalElements = 0;

  private admin = inject(AdminService);
  private toast = inject(ToastService);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.admin.getReviews(this.pageNumber - 1, this.pageSize).subscribe({
      next: res => {
        this.reviews.set(res.content);
        this.totalElements = res.totalElements;
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.toast.error('Could not load reviews.');
      },
    });
  }

  remove(review: Review): void {
    if (!confirm(`Delete this ${review.rating}★ review by ${review.authorName}?`)) {
      return;
    }
    this.admin.deleteReview(review.id).subscribe({
      next: () => {
        this.toast.success('Review removed');
        if (this.reviews().length === 1 && this.pageNumber > 1) {
          this.pageNumber--;
        }
        this.load();
      },
      error: () => this.toast.error('Could not delete review.'),
    });
  }
}
