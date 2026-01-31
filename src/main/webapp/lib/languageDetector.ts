import languageDetector from 'next-language-detector';
import rootConfig from '@root/next-i18next.config';

export default languageDetector({
  fallbackLng: rootConfig.i18n.defaultLocale,
  supportedLngs: rootConfig.i18n.locales,
});
