import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

/**
 * Shell for the platform (superadmin) tier (roadmap #21, Milestone B) — sits above the regular
 * tenant-scoped admin back-office, mirroring {@code AdminLayout}'s structure so future platform pages
 * (e.g. roadmap #22 tenant billing) have somewhere to live without restructuring. Route-level access
 * (`SuperAdmin` authority) is enforced server-side; each page here checks the caller's roles itself
 * (same courtesy-not-enforcement pattern as the admin sidebar's role badge).
 */
@Component({
  selector: 'app-platform-layout',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './platform-layout.html',
})
export class PlatformLayout {
}
