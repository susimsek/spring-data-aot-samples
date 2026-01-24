'use client';

import Container from 'react-bootstrap/Container';
import Button from 'react-bootstrap/Button';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faHouse, faTriangleExclamation } from '@fortawesome/free-solid-svg-icons';
import AppNavbar from './components/AppNavbar.js';
import Footer from './components/Footer.js';

export default function NotFoundPage() {
  return (
    <>
      <AppNavbar />
      <main className="flex-grow-1 d-flex align-items-center justify-content-center py-5">
        <Container className="text-center">
          <div className="display-3 text-warning mb-3">
            <FontAwesomeIcon icon={faTriangleExclamation} />
          </div>
          <h1 className="h3 mb-2">Page not found</h1>
          <p className="text-muted mb-4">The page you’re looking for doesn’t exist.</p>
          <Button variant="primary" href="/" className="d-inline-flex align-items-center gap-2">
            <FontAwesomeIcon icon={faHouse} />
            <span>Back to home</span>
          </Button>
        </Container>
      </main>
      <Footer />
    </>
  );
}
