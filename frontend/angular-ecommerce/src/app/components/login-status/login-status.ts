import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { OKTA_AUTH, OktaAuthStateService } from '@okta/okta-angular';
import { Observable, map } from 'rxjs';

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
