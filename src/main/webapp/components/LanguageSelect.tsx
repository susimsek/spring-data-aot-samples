'use client';

import Dropdown from 'react-bootstrap/Dropdown';
import { useRouter } from 'next/router';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faLanguage } from '@fortawesome/free-solid-svg-icons';
import languageDetector from '../lib/languageDetector';

function stripLocalePrefix(path: string, locales: readonly string[]): string {
  for (const locale of locales) {
    const prefix = `/${locale}`;
    if (path === prefix) return '/';
    if (path.startsWith(`${prefix}/`)) return path.slice(prefix.length) || '/';
  }
  return path || '/';
}

function getCurrentLocale(path: string, locales: readonly string[]): string {
  for (const locale of locales) {
    const prefix = `/${locale}`;
    if (path === prefix || path.startsWith(`${prefix}/`)) return locale;
  }
  return locales[0] ?? 'en';
}

export default function LanguageSelect() {
  const router = useRouter();
  const locales = ['en', 'tr'] as const;
  const currentLocale = getCurrentLocale(router.asPath || '/', locales);

  const currentLabel = currentLocale === 'tr' ? 'Türkçe' : 'English';

  const handleSelect = (nextLocale: string) => {
    const nextPath = stripLocalePrefix(router.asPath || '/', locales);
    const href = `/${nextLocale}${nextPath.startsWith('/') ? '' : '/'}${nextPath}`;

    languageDetector.cache?.(nextLocale);
    if (globalThis.document?.documentElement) {
      globalThis.document.documentElement.lang = nextLocale;
    }

    void router.push(href);
  };

  return (
    <Dropdown>
      <Dropdown.Toggle
        variant="outline-secondary"
        size="sm"
        aria-label="Language"
        className="d-inline-flex align-items-center gap-2 text-nowrap"
      >
        <FontAwesomeIcon icon={faLanguage} />
        <span>{currentLabel}</span>
      </Dropdown.Toggle>
      <Dropdown.Menu>
        <Dropdown.Item as="button" type="button" active={currentLocale === 'en'} onClick={() => handleSelect('en')}>
          English
        </Dropdown.Item>
        <Dropdown.Item as="button" type="button" active={currentLocale === 'tr'} onClick={() => handleSelect('tr')}>
          Türkçe
        </Dropdown.Item>
      </Dropdown.Menu>
    </Dropdown>
  );
}
