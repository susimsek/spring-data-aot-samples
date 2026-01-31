'use client';

import type { AnchorHTMLAttributes, ReactNode } from 'react';
import { forwardRef } from 'react';
import NextLink from 'next/link';
import { useRouter } from 'next/router';

type Props = Omit<AnchorHTMLAttributes<HTMLAnchorElement>, 'href'> & {
  children: ReactNode;
  href?: string;
  locale?: string;
  skipLocaleHandling?: boolean;
};

const Link = forwardRef<HTMLAnchorElement, Props>(function Link(
  { children, skipLocaleHandling, href, locale, ...anchorProps }: Readonly<Props>,
  ref,
) {
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
    <NextLink ref={ref} href={resolvedHref} {...anchorProps}>
      {children}
    </NextLink>
  );
});

export default Link;
