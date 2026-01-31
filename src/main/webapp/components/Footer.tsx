'use client';

import Container from 'react-bootstrap/Container';
import { useTranslation } from 'next-i18next';

export default function Footer() {
  const { t } = useTranslation();
  return (
    <footer className="bg-body border-top py-3 mt-auto w-100">
      <Container className="d-flex justify-content-center gap-3 text-muted small">
        <span>{t('footer.copyright')}</span>
        <span>{t('footer.tech')}</span>
      </Container>
    </footer>
  );
}
