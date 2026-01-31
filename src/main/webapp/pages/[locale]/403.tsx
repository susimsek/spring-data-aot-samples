'use client';

import Container from 'react-bootstrap/Container';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faBan, faHouse, faRightToBracket } from '@fortawesome/free-solid-svg-icons';
import { useTranslation } from 'next-i18next';
import AppNavbar from '@components/AppNavbar';
import Footer from '@components/Footer';
import Link from '@components/Link';
import { getStaticPaths, makeStaticProps } from '@lib/getStatic';
import useAuth from '@lib/useAuth';

export default function AccessDeniedPage() {
  const { t } = useTranslation();
  const { isAuthenticated } = useAuth();
  return (
    <div className="d-flex flex-column min-vh-100 bg-body-tertiary">
      <AppNavbar requireAuthForActions showSignInButton />
      <main className="flex-fill d-flex align-items-center justify-content-center py-5">
        <Container className="text-center">
          <div className="display-3 text-warning mb-3">
            <FontAwesomeIcon icon={faBan} />
          </div>
          <h1 className="h3 mb-2">{t('errors.accessDenied.title')}</h1>
          <p className="text-muted mb-4">{t('errors.accessDenied.message')}</p>
          <div className="d-flex gap-2 justify-content-center flex-wrap">
            {isAuthenticated ? (
              <Link href="/" className="btn btn-primary d-inline-flex align-items-center gap-2">
                <FontAwesomeIcon icon={faHouse} />
                <span>{t('common.backToHome')}</span>
              </Link>
            ) : (
              <Link href="/login" className="btn btn-primary d-inline-flex align-items-center gap-2">
                <FontAwesomeIcon icon={faRightToBracket} />
                <span>{t('common.signIn')}</span>
              </Link>
            )}
          </div>
        </Container>
      </main>
      <Footer />
    </div>
  );
}

const getStaticProps = makeStaticProps(['common']);

export { getStaticPaths, getStaticProps };
