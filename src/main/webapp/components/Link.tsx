'use client';

import type { AnchorHTMLAttributes, ReactNode } from 'react';
import NextLink from 'next/link';
import { useRouter } from 'next/router';

type Props = Omit<AnchorHTMLAttributes<HTMLAnchorElement>, 'href'> & {
  children: ReactNode;
  href?: string;
  locale?: string;
  skipLocaleHandling?: boolean;
};

export default function Link({ children, skipLocaleHandling, href, locale, ...anchorProps }: Readonly<Props>) {
  const router = useRouter();
  const resolvedLocale = locale || String(router.query.locale || '');

  let resolvedHref = href || router.asPath;

  if (resolvedHref.startsWith('http')) {
    skipLocaleHandling = true;
  }

  if (resolvedLocale && !skipLocaleHandling) {
    resolvedHref = resolvedHref ? `/${resolvedLocale}${resolvedHref}` : router.pathname.replace('[locale]', resolvedLocale);
  }

  return (
    <NextLink href={resolvedHref} legacyBehavior>
      <a {...anchorProps}>{children}</a>
    </NextLink>
  );
}
