import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-search',
  templateUrl: './search.html'
})
export class SearchComponent {

  constructor(private router: Router) {}

  doSearch(keyword: string): void {
    const trimmed = keyword.trim();
    if (trimmed.length === 0) {
      return;
    }
    this.router.navigate(['/search', trimmed]);
  }
}
