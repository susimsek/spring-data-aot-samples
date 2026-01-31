import languageDetector from 'next-language-detector';
import type { UserConfig } from 'next-i18next';
import rootConfig from '@root/next-i18next.config';

const i18nextConfig = rootConfig as UserConfig;

export default languageDetector({
  fallbackLng: i18nextConfig.i18n.defaultLocale,
  supportedLngs: i18nextConfig.i18n.locales,
});
