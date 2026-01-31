'use client';

import Container from 'react-bootstrap/Container';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faHouse, faTriangleExclamation } from '@fortawesome/free-solid-svg-icons';
import { useTranslation } from 'next-i18next';
import AppNavbar from '@components/AppNavbar';
import Footer from '@components/Footer';
import Link from '@components/Link';
import { getStaticPaths, makeStaticProps } from '@lib/getStatic';

export default function NotFoundRoutePage() {
  const { t } = useTranslation();
  return (
    <div className="d-flex flex-column min-vh-100 bg-body-tertiary">
      <AppNavbar requireAuthForActions />
      <main className="flex-fill d-flex align-items-center justify-content-center py-5">
        <Container className="text-center">
          <div className="display-3 text-warning mb-3">
            <FontAwesomeIcon icon={faTriangleExclamation} />
          </div>
          <h1 className="h3 mb-2">{t('errors.notFound.title')}</h1>
          <p className="text-muted mb-4">{t('errors.notFound.message')}</p>
          <Link href="/" className="btn btn-primary d-inline-flex align-items-center gap-2">
            <FontAwesomeIcon icon={faHouse} />
            <span>{t('common.backToHome')}</span>
          </Link>
        </Container>
      </main>
      <Footer />
    </div>
  );
}

const getStaticProps = makeStaticProps(['common']);

export { getStaticPaths, getStaticProps };
