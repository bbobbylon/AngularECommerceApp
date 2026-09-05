import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AdminService } from '../../../services/admin.service';

/**
 * Shell for the whole admin back-office. Fetches the caller's RBAC (roadmap #19) role(s) once, purely
 * as a courtesy label in the sidebar — the backend's request-matcher authorization is the actual
 * enforcement boundary, this is not a client-side permission gate.
 */
@Component({
  selector: 'app-admin-layout',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './admin-layout.html',
})
export class AdminLayout implements OnInit {

  readonly roles = signal<string[]>([]);

  private admin = inject(AdminService);

  ngOnInit(): void {
    this.admin.getCurrentAdmin().subscribe({
      next: res => this.roles.set(res.roles),
      error: () => this.roles.set([]),
    });
  }
}
