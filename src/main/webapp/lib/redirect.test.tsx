import { render } from '@testing-library/react';

const routerState = {
  asPath: '/login',
  pathname: '/login',
  query: {},
  route: '/login',
  replace: jest.fn(),
  push: jest.fn(),
  prefetch: jest.fn(),
};

jest.mock('next/router', () => ({
  __esModule: true,
  useRouter() {
    return routerState;
  },
}));

jest.mock('@lib/languageDetector', () => ({
  __esModule: true,
  default: {
    detect: jest.fn(() => 'tr'),
    cache: jest.fn(),
  },
}));

jest.mock('@root/next-i18next.config', () => ({
  __esModule: true,
  default: {
    i18n: {
      defaultLocale: 'en',
    },
  },
}));

import { Redirect } from './redirect';
import languageDetector from '@lib/languageDetector';

describe('redirect', () => {
  beforeEach(() => {
    routerState.replace.mockClear();
    (languageDetector.detect as jest.Mock).mockClear();
    (languageDetector.cache as jest.Mock).mockClear();
    document.documentElement.lang = '';
  });

  test('Redirect uses detected locale and updates document lang', async () => {
    routerState.asPath = '/login';
    routerState.route = '/login';

    render(<Redirect />);

    expect(routerState.replace).toHaveBeenCalledWith('/tr/login');
    expect(languageDetector.cache).toHaveBeenCalledWith('tr');
    expect(document.documentElement.lang).toBe('tr');
  });

  test('Redirect prevents endless loop for locale-prefixed 404 routes', async () => {
    routerState.asPath = '/tr/whatever';
    routerState.route = '/404';

    render(<Redirect />);

    expect(routerState.replace).toHaveBeenCalledWith('/tr/404');
    expect(languageDetector.cache).not.toHaveBeenCalled();
    expect(document.documentElement.lang).toBe('');
  });
});
