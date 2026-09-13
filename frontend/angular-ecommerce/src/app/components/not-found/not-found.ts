import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/** The wildcard `**` route's target — plain static 404 page, no logic. */
@Component({
  selector: 'app-not-found',
  imports: [RouterLink],
  templateUrl: './not-found.html',
})
export class NotFound {}
