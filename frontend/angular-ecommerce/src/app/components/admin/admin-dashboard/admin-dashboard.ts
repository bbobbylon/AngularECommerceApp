import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AdminService, AdminStats, SystemHealth } from '../../../services/admin.service';

@Component({
  selector: 'app-admin-dashboard',
  imports: [CurrencyPipe, DecimalPipe, RouterLink],
  templateUrl: './admin-dashboard.html',
})
export class AdminDashboard implements OnInit {

  readonly stats = signal<AdminStats | null>(null);
  readonly loading = signal(true);
  readonly error = signal(false);

  readonly health = signal<SystemHealth | null>(null);

  private admin = inject(AdminService);

  ngOnInit(): void {
    this.admin.getStats().subscribe({
      next: stats => {
        this.stats.set(stats);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });

    this.admin.getSystemHealth().subscribe({
      next: health => this.health.set(health),
      error: () => this.health.set(null),
    });
  }

  /** Human-friendly uptime, e.g. "2h 14m" or "47s". */
  formatUptime(seconds: number): string {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = Math.floor(seconds % 60);
    if (h > 0) {
      return `${h}h ${m}m`;
    }
    return m > 0 ? `${m}m ${s}s` : `${s}s`;
  }
}
