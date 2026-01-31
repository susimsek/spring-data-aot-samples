'use client';

import Container from 'react-bootstrap/Container';
import Button from 'react-bootstrap/Button';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faBan, faHouse, faRightToBracket } from '@fortawesome/free-solid-svg-icons';
import { useTranslation } from 'next-i18next';
import AppNavbar from '../../components/AppNavbar';
import Footer from '../../components/Footer';
import { getStaticPaths, makeStaticProps } from '../../lib/getStatic';

export default function AccessDeniedPage() {
  const { t } = useTranslation();
  return (
    <>
      <AppNavbar />
      <main className="flex-grow-1 d-flex align-items-center justify-content-center py-5">
        <Container className="text-center">
          <div className="display-3 text-warning mb-3">
            <FontAwesomeIcon icon={faBan} />
          </div>
          <h1 className="h3 mb-2">{t('errors.accessDenied.title')}</h1>
          <p className="text-muted mb-4">{t('errors.accessDenied.message')}</p>
          <div className="d-flex gap-2 justify-content-center flex-wrap">
            <Button variant="primary" href="/" className="d-inline-flex align-items-center gap-2">
              <FontAwesomeIcon icon={faHouse} />
              <span>{t('common.backToHome')}</span>
            </Button>
            <Button variant="outline-secondary" href="/login" className="d-inline-flex align-items-center gap-2">
              <FontAwesomeIcon icon={faRightToBracket} />
              <span>{t('common.signIn')}</span>
            </Button>
          </div>
        </Container>
      </main>
      <Footer />
    </>
  );
}

const getStaticProps = makeStaticProps(['common']);

export { getStaticPaths, getStaticProps };
