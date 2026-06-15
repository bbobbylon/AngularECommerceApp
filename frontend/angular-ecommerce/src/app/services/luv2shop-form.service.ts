import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, map, of } from 'rxjs';

import { environment } from '../../environments/environment';
import { Country } from '../common/country';
import { State } from '../common/state';

@Injectable({ providedIn: 'root' })
export class Luv2ShopFormService {

  private readonly countriesUrl = `${environment.apiUrl}/countries`;
  private readonly statesUrl = `${environment.apiUrl}/states`;

  constructor(private httpClient: HttpClient) {}

  getCountries(): Observable<Country[]> {
    return this.httpClient
      .get<GetResponseCountries>(this.countriesUrl)
      .pipe(map(response => response._embedded.countries));
  }

  getStates(countryCode: string): Observable<State[]> {
    const url = `${this.statesUrl}/search/findByCountryCode?code=${countryCode}`;
    return this.httpClient
      .get<GetResponseStates>(url)
      .pipe(map(response => response._embedded.states));
  }

  getCreditCardMonths(startMonth: number): Observable<number[]> {
    const months: number[] = [];
    for (let month = startMonth; month <= 12; month++) {
      months.push(month);
    }
    return of(months);
  }

  getCreditCardYears(): Observable<number[]> {
    const years: number[] = [];
    const startYear = new Date().getFullYear();
    for (let year = startYear; year <= startYear + 10; year++) {
      years.push(year);
    }
    return of(years);
  }
}

interface GetResponseCountries {
  _embedded: {
    countries: Country[];
  };
}

interface GetResponseStates {
  _embedded: {
    states: State[];
  };
}
