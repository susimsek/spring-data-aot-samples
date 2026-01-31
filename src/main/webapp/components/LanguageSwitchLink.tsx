'use client';

import type { ButtonHTMLAttributes } from 'react';
import NextLink from 'next/link';
import { useRouter } from 'next/router';
import languageDetector from '../lib/languageDetector';

type Props = Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'onClick'> & {
  locale: string;
  href?: string;
  label?: string;
};

function queryValueToString(value: unknown): string {
  if (typeof value === 'string') return value;
  if (Array.isArray(value)) return typeof value[0] === 'string' ? value[0] : '';
  return '';
}

export default function LanguageSwitchLink({ locale, href, label, ...buttonProps }: Readonly<Props>) {
  const router = useRouter();

  let resolvedHref = href || router.asPath;
  let pathname = router.pathname;

  for (const [key, value] of Object.entries(router.query)) {
    if (key === 'locale') {
      pathname = pathname.replace(`[${key}]`, locale);
      continue;
    }
    pathname = pathname.replace(`[${key}]`, queryValueToString(value));
  }

  if (locale) {
    resolvedHref = href ? `/${locale}${href}` : pathname;
  }

  if (!resolvedHref.startsWith(`/${locale}`)) {
    resolvedHref = `/${locale}${resolvedHref}`;
  }

  return (
    <NextLink href={resolvedHref}>
      <button
        type="button"
        style={{ fontSize: 'small' }}
        onClick={() => {
          languageDetector.cache?.(locale);
          if (globalThis.document?.documentElement) {
            globalThis.document.documentElement.lang = locale;
          }
        }}
        {...buttonProps}
      >
        {label ?? locale.toUpperCase()}
      </button>
    </NextLink>
  );
}
