import { DOCUMENT } from '@angular/common';
import { Inject, Injectable } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';

export interface SeoData {
  title: string;
  description: string;
  image?: string;
  url?: string;
  type?: 'website' | 'product' | 'article';
}

const SITE_NAME = 'Luv2Shop';

/**
 * Per-route title/meta/canonical + JSON-LD structured data (roadmap #11 — SEO). This is a plain
 * client-side SPA with no server-render pass, so these tags land after the initial paint — fine
 * for crawlers that execute JS (Google, Bing) but not a substitute for prerendering/SSR if that's
 * ever needed for crawlers that don't.
 */
@Injectable({ providedIn: 'root' })
export class SeoService {
  private readonly jsonLdScripts = new Map<string, HTMLScriptElement>();

  constructor(
    private titleService: Title,
    private meta: Meta,
    @Inject(DOCUMENT) private document: Document,
  ) {}

  update(data: SeoData): void {
    const fullTitle = data.title.includes(SITE_NAME) ? data.title : `${data.title} | ${SITE_NAME}`;
    const url = data.url ?? this.document.location.href;
    const image = data.image ?? `${this.document.location.origin}/favicon.ico`;

    this.titleService.setTitle(fullTitle);
    this.meta.updateTag({ name: 'description', content: data.description });
    this.meta.updateTag({ property: 'og:site_name', content: SITE_NAME });
    this.meta.updateTag({ property: 'og:title', content: fullTitle });
    this.meta.updateTag({ property: 'og:description', content: data.description });
    this.meta.updateTag({ property: 'og:type', content: data.type ?? 'website' });
    this.meta.updateTag({ property: 'og:url', content: url });
    this.meta.updateTag({ property: 'og:image', content: image });
    this.meta.updateTag({ name: 'twitter:card', content: 'summary_large_image' });
    this.meta.updateTag({ name: 'twitter:title', content: fullTitle });
    this.meta.updateTag({ name: 'twitter:description', content: data.description });
    this.meta.updateTag({ name: 'twitter:image', content: image });

    this.setCanonical(url);
  }

  private setCanonical(url: string): void {
    let link = this.document.querySelector<HTMLLinkElement>('link[rel="canonical"]');
    if (!link) {
      link = this.document.createElement('link');
      link.setAttribute('rel', 'canonical');
      this.document.head.appendChild(link);
    }
    link.setAttribute('href', url);
  }

  /** Injects a JSON-LD <script> block for structured data, keyed by id so callers can manage their own. */
  setJsonLd(id: string, data: object): void {
    this.removeJsonLd(id);
    const script = this.document.createElement('script');
    script.type = 'application/ld+json';
    script.text = JSON.stringify(data);
    this.document.head.appendChild(script);
    this.jsonLdScripts.set(id, script);
  }

  removeJsonLd(id: string): void {
    this.jsonLdScripts.get(id)?.remove();
    this.jsonLdScripts.delete(id);
  }
}
