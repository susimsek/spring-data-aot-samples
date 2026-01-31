import type { GetStaticPathsResult, GetStaticPropsContext } from 'next';
import { serverSideTranslations } from 'next-i18next/serverSideTranslations';
import type { UserConfig } from 'next-i18next';
import rootConfig from '../../../../next-i18next.config';

const i18nextConfig = rootConfig as UserConfig;

type LocaleParams = {
  locale: string;
};

export const getI18nPaths = () =>
  i18nextConfig.i18n.locales.map((locale) => ({
    params: {
      locale,
    },
  }));

export const getStaticPaths = (): GetStaticPathsResult<LocaleParams> => ({
  fallback: false,
  paths: getI18nPaths(),
});

export const getI18nProps = async (ctx: GetStaticPropsContext<LocaleParams> | undefined, ns: string[] = ['common']) => {
  const locale = ctx?.params?.locale || i18nextConfig.i18n.defaultLocale;
  return {
    ...(await serverSideTranslations(locale, ns, i18nextConfig)),
  };
};

export const makeStaticProps =
  (ns: string[] = ['common']) =>
  async (ctx: GetStaticPropsContext<LocaleParams>) => ({
    props: await getI18nProps(ctx, ns),
  });
