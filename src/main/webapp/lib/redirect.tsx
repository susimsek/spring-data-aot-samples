import { useEffect } from 'react';
import { useRouter } from 'next/router';
import rootConfig from '@root/next-i18next.config';
import languageDetector from '@lib/languageDetector';

export const useRedirect = (to?: string): void => {
  const router = useRouter();
  const target = to || router.asPath;

  useEffect(() => {
    const detectedLng = (languageDetector.detect ? languageDetector.detect() : undefined) || rootConfig.i18n.defaultLocale;
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
