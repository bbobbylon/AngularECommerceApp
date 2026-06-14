import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-search',
  imports: [],
  templateUrl: './search.html',
})
export class Search {

  constructor(private router: Router) {}

  doSearch(value: string): void {
    const keyword = value.trim();
    if (keyword.length > 0) {
      this.router.navigate(['/search', keyword]);
    }
  }
}
