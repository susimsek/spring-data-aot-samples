import type { GetStaticPathsResult, GetStaticPropsContext } from 'next';
import { serverSideTranslations } from 'next-i18next/serverSideTranslations';
import rootConfig from '@root/next-i18next.config';

type LocaleParams = {
  locale: string;
};

export const getI18nPaths = () =>
  rootConfig.i18n.locales.map((locale) => ({
    params: {
      locale,
    },
  }));

export const getStaticPaths = (): GetStaticPathsResult<LocaleParams> => ({
  fallback: false,
  paths: getI18nPaths(),
});

export const getI18nProps = async (ctx: GetStaticPropsContext<LocaleParams> | undefined, ns: string[] = ['common']) => {
  const locale = ctx?.params?.locale || rootConfig.i18n.defaultLocale;
  return {
    ...(await serverSideTranslations(locale, ns, rootConfig)),
  };
};

export const makeStaticProps =
  (ns: string[] = ['common']) =>
  async (ctx: GetStaticPropsContext<LocaleParams>) => ({
    props: await getI18nProps(ctx, ns),
  });
