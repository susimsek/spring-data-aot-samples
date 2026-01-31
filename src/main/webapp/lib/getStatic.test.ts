import { serverSideTranslations } from 'next-i18next/serverSideTranslations';
import rootConfig from '@root/next-i18next.config';
import { getI18nPaths, getI18nProps, getStaticPaths, makeStaticProps } from './getStatic';

jest.mock('next-i18next/serverSideTranslations', () => ({
  serverSideTranslations: jest.fn(async () => ({ _nextI18Next: { initialLocale: 'en', ns: ['common'] } })),
}));

describe('getStatic', () => {
  test('getI18nPaths returns paths for configured locales', () => {
    expect(getI18nPaths().map((p) => p.params.locale)).toEqual(rootConfig.i18n.locales);
  });

  test('getStaticPaths returns non-fallback locale paths', () => {
    expect(getStaticPaths()).toEqual({
      fallback: false,
      paths: getI18nPaths(),
    });
  });

  test('getI18nProps calls serverSideTranslations for locale', async () => {
    const ctx = { params: { locale: 'tr' } } as const;
    const props = await getI18nProps(ctx);

    expect(serverSideTranslations).toHaveBeenCalledWith('tr', ['common'], rootConfig);
    expect(props).toHaveProperty('_nextI18Next');
  });

  test('makeStaticProps delegates to getI18nProps', async () => {
    const ctx = { params: { locale: 'en' } } as const;
    const getStaticProps = makeStaticProps(['common']);
    const result = await getStaticProps(ctx as any);

    expect(serverSideTranslations).toHaveBeenCalledWith('en', ['common'], rootConfig);
    expect(result).toHaveProperty('props');
  });
});
