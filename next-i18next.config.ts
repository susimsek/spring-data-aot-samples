import type { UserConfig } from 'next-i18next';

const config: UserConfig = {
  debug: process.env.NODE_ENV === 'development',
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },
  // Server-side: filesystem path relative to repo root.
  // Client-side: served from Next static export at `/locales`.
  localePath: typeof window === 'undefined' ? 'src/main/webapp/public/locales' : '/locales',
};

export default config;
