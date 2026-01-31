import '../styles/styles.css';
import 'bootstrap/dist/css/bootstrap.min.css';
import '@lib/fontawesome';
import type { AppProps } from 'next/app';
import { appWithTranslation } from 'next-i18next';
import AppProviders from '@components/AppProviders';
import rootConfig from '@root/next-i18next.config';

function MyApp({ Component, pageProps }: AppProps) {
  return (
    <AppProviders>
      <Component {...pageProps} />
    </AppProviders>
  );
}

export default appWithTranslation(MyApp, rootConfig);
