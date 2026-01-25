import './styles.css';
import 'bootstrap/dist/css/bootstrap.min.css';
import './lib/fontawesome';
import Script from 'next/script';
import type { Metadata } from 'next';
import type { ReactNode } from 'react';
import Providers from './components/Providers';

export const metadata: Metadata = {
  title: 'Notes',
  description: 'Notes app',
  icons: {
    icon: [
      { url: '/favicon.svg', type: 'image/svg+xml' },
      { url: '/favicon-32x32.png', sizes: '32x32', type: 'image/png' },
      { url: '/favicon-16x16.png', sizes: '16x16', type: 'image/png' },
    ],
    shortcut: ['/favicon.ico'],
  },
};

const themeInitScript = `
(() => {
  try {
    const stored = globalThis.localStorage && globalThis.localStorage.getItem('theme');
    const system =
      globalThis.matchMedia && globalThis.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    const theme = stored || system;
    if (globalThis.document && globalThis.document.documentElement) {
      globalThis.document.documentElement.dataset.bsTheme = theme;
    }
  } catch {
    // ignore
  }
})();
`;

export default function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className="bg-body-tertiary d-flex flex-column min-vh-100">
        <Script id="theme-init" strategy="beforeInteractive" dangerouslySetInnerHTML={{ __html: themeInitScript }} />
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
