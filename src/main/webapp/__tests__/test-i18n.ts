import i18next from 'i18next';
import { initReactI18next } from 'react-i18next';
import commonEn from '../public/locales/en/common.json';

const DEFAULT_LOCALE = 'en';

if (!i18next.isInitialized) {
  i18next.use(initReactI18next).init({
    lng: DEFAULT_LOCALE,
    fallbackLng: DEFAULT_LOCALE,
    supportedLngs: [DEFAULT_LOCALE],
    defaultNS: 'common',
    ns: ['common'],
    interpolation: {
      escapeValue: false,
    },
    react: {
      useSuspense: false,
    },
    resources: {
      en: {
        common: commonEn,
      },
    },
  });
}

export default i18next;
