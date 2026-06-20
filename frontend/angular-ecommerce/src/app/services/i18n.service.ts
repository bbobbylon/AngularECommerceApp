import { Injectable, signal } from '@angular/core';

export interface Language {
  code: string;
  name: string;
}

/**
 * Lightweight runtime i18n. Holds the active language (a signal, persisted) and key→string
 * dictionaries; `t(key)` looks up the active language, falling back to English then the key itself.
 * Demonstrated on the nav/footer — add keys + a pipe usage to translate more of the UI incrementally.
 * (A full build-time @angular/localize extraction is the path for shipping every string.)
 */
@Injectable({ providedIn: 'root' })
export class I18nService {

  static readonly LANGUAGES: Language[] = [
    { code: 'en', name: 'English' },
    { code: 'es', name: 'Español' },
    { code: 'fr', name: 'Français' },
  ];

  private static readonly DICT: Record<string, Record<string, string>> = {
    en: {
      'nav.shopAll': 'Shop all', 'nav.sale': 'Sale', 'nav.favorites': 'Favorites',
      'nav.about': 'About', 'nav.account': 'My account',
      'footer.shop': 'Shop', 'footer.company': 'Company', 'footer.help': 'Help',
      'common.language': 'Language', 'common.currency': 'Currency',
    },
    es: {
      'nav.shopAll': 'Comprar todo', 'nav.sale': 'Ofertas', 'nav.favorites': 'Favoritos',
      'nav.about': 'Acerca de', 'nav.account': 'Mi cuenta',
      'footer.shop': 'Tienda', 'footer.company': 'Empresa', 'footer.help': 'Ayuda',
      'common.language': 'Idioma', 'common.currency': 'Moneda',
    },
    fr: {
      'nav.shopAll': 'Tout acheter', 'nav.sale': 'Soldes', 'nav.favorites': 'Favoris',
      'nav.about': 'À propos', 'nav.account': 'Mon compte',
      'footer.shop': 'Boutique', 'footer.company': 'Entreprise', 'footer.help': 'Aide',
      'common.language': 'Langue', 'common.currency': 'Devise',
    },
  };

  private readonly storageKey = 'lang';
  readonly lang = signal<string>(this.load());
  readonly languages = I18nService.LANGUAGES;

  setLang(code: string): void {
    if (I18nService.DICT[code]) {
      this.lang.set(code);
      try {
        localStorage.setItem(this.storageKey, code);
      } catch {
        /* ignore */
      }
    }
  }

  t(key: string): string {
    const lang = this.lang();
    return I18nService.DICT[lang]?.[key] ?? I18nService.DICT['en'][key] ?? key;
  }

  private load(): string {
    try {
      const code = localStorage.getItem(this.storageKey);
      return code && I18nService.DICT[code] ? code : 'en';
    } catch {
      return 'en';
    }
  }
}
