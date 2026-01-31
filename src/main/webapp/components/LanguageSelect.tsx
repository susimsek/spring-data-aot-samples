'use client';

import Form from 'react-bootstrap/Form';
import InputGroup from 'react-bootstrap/InputGroup';
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

  return (
    <InputGroup size="sm" style={{ width: 140 }}>
      <InputGroup.Text className="bg-transparent border-secondary">
        <FontAwesomeIcon icon={faLanguage} />
      </InputGroup.Text>
      <Form.Select
        size="sm"
        value={currentLocale}
        aria-label="Language"
        className="bg-transparent border-secondary border-start-0"
        onChange={(e) => {
          const nextLocale = e.currentTarget.value;
          const nextPath = stripLocalePrefix(router.asPath || '/', locales);
          const href = `/${nextLocale}${nextPath.startsWith('/') ? '' : '/'}${nextPath}`;

          languageDetector.cache?.(nextLocale);
          if (globalThis.document?.documentElement) {
            globalThis.document.documentElement.lang = nextLocale;
          }

          void router.push(href);
        }}
      >
        <option value="en">English</option>
        <option value="tr">Türkçe</option>
      </Form.Select>
    </InputGroup>
  );
}
