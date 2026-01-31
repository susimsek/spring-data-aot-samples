import { fireEvent, render } from '@testing-library/react';

jest.mock('@lib/languageDetector', () => ({
  __esModule: true,
  default: {
    cache: jest.fn(),
  },
}));

const routerState = {
  asPath: '/en/notes/123?foo=bar',
  pathname: '/[locale]/notes/[id]',
  query: { locale: 'en', id: '123' },
  route: '/[locale]/notes/[id]',
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

jest.mock('next/link', () => ({
  __esModule: true,
  default: function NextLink({ href, children }: Readonly<{ href: string; children: React.ReactNode }>) {
    return (
      <a href={href} data-testid="next-link">
        {children}
      </a>
    );
  },
}));

import LanguageSwitchLink from './LanguageSwitchLink';
import languageDetector from '@lib/languageDetector';

describe('LanguageSwitchLink', () => {
  beforeEach(() => {
    (languageDetector.cache as jest.Mock).mockClear();
    document.documentElement.lang = '';
  });

  test('builds locale-specific href from router pathname/query', () => {
    const { container } = render(<LanguageSwitchLink locale="tr" />);
    const link = container.querySelector('a');
    expect(link?.getAttribute('href')).toBe('/tr/notes/123');
  });

  test('click caches locale and updates document lang', () => {
    const { getByRole } = render(<LanguageSwitchLink locale="tr" label="TR" />);
    fireEvent.click(getByRole('button', { name: 'TR' }));
    expect(languageDetector.cache).toHaveBeenCalledWith('tr');
    expect(document.documentElement.lang).toBe('tr');
  });
});
