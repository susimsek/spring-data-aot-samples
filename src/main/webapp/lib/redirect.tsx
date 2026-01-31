import { useEffect } from 'react';
import { useRouter } from 'next/router';
import languageDetector from './languageDetector';
import type { UserConfig } from 'next-i18next';
import rootConfig from '../../../../next-i18next.config.js';

const i18nextConfig = rootConfig as UserConfig;

export const useRedirect = (to?: string): void => {
  const router = useRouter();
  const target = to || router.asPath;

  useEffect(() => {
    const detectedLng = (languageDetector.detect ? languageDetector.detect() : undefined) || i18nextConfig.i18n.defaultLocale;
    if (target.startsWith('/' + detectedLng) && router.route === '/404') {
      // prevent endless loop
      router.replace('/' + detectedLng + router.route);
      return;
    }

    if (languageDetector.cache) {
      languageDetector.cache(detectedLng);
    }
    router.replace('/' + detectedLng + target);
    document.documentElement.lang = detectedLng;
  });
};

export const Redirect = (): null => {
  useRedirect();
  return null;
};

export const getRedirect = (to: string) => (): null => {
  useRedirect(to);
  return null;
};
