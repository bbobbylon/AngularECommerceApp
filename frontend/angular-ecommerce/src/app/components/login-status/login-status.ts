import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { OKTA_AUTH, OktaAuthStateService } from '@okta/okta-angular';
import { Observable, map } from 'rxjs';

/**
 * Header sign-in/sign-out widget, reading Okta's auth state directly via `OktaAuthStateService`
 * (roadmap M3). Unlike `dev-auth.guard.ts`, this doesn't check `isOktaConfigured()` itself — it just
 * reflects whatever `okta-config.ts`'s issuer (real or placeholder) reports.
 */
@Component({
  selector: 'app-login-status',
  imports: [CommonModule, RouterLink],
  templateUrl: './login-status.html',
})
export class LoginStatus implements OnInit {

  isAuthenticated$!: Observable<boolean>;
  userFullName$!: Observable<string>;

  private oktaAuth = inject(OKTA_AUTH);
  private authStateService = inject(OktaAuthStateService);

  ngOnInit(): void {
    this.isAuthenticated$ = this.authStateService.authState$.pipe(
      map(state => !!state.isAuthenticated),
    );
    this.userFullName$ = this.authStateService.authState$.pipe(
      map(state => (state.idToken?.claims?.name as string) ?? ''),
    );
  }

  login(): void {
    this.oktaAuth.signInWithRedirect();
  }

  logout(): void {
    this.oktaAuth.signOut();
  }
}
