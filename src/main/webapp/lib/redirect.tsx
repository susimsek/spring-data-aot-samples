import { useEffect } from 'react';
import { useRouter } from 'next/router';
import languageDetector from './languageDetector';
import type { UserConfig } from 'next-i18next';
import rootConfig from '../../../../next-i18next.config.js';

const i18nextConfig = rootConfig as UserConfig;

export function useRedirect(to?: string) {
  const router = useRouter();
  const target = to || router.asPath;

  useEffect(() => {
    const detectedLng = languageDetector.detect?.() || i18nextConfig.i18n.defaultLocale;

    if (target.startsWith(`/${detectedLng}`) && router.route === '/404') {
      // prevent endless loop
      router.replace(`/${detectedLng}${router.route}`);
      return;
    }

    languageDetector.cache?.(detectedLng);
    router.replace(`/${detectedLng}${target}`);

    if (globalThis.document?.documentElement) {
      globalThis.document.documentElement.lang = detectedLng;
    }
  }, [router, target]);

  return null;
}

export function Redirect() {
  useRedirect();
  return null;
}

export const getRedirect = (to: string) =>
  function RedirectTo() {
    useRedirect(to);
    return null;
  };
