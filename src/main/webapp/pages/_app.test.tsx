import type { AppProps } from 'next/app';
import React from 'react';
import { render, screen } from '@testing-library/react';

jest.mock('../styles/styles.css', () => ({}));
jest.mock('bootstrap/dist/css/bootstrap.min.css', () => ({}));
jest.mock('@lib/fontawesome', () => ({}));
jest.mock('@components/AppProviders', () => ({
  __esModule: true,
  default: function AppProviders({ children }: Readonly<{ children: React.ReactNode }>) {
    return <>{children}</>;
  },
}));
jest.mock('next-i18next', () => ({
  appWithTranslation: (Component: unknown) => Component,
}));

import App from './_app';

describe('_app', () => {
  test('wraps pages with providers', () => {
    const Component = () => <div>page</div>;
    const props = { Component, pageProps: {} } as unknown as AppProps;
    render(React.createElement(App as unknown as React.ComponentType<AppProps>, props));
    expect(screen.getByText('page')).toBeInTheDocument();
  });
});
