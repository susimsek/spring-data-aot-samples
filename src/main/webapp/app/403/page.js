'use client';

import Container from 'react-bootstrap/Container';
import Button from 'react-bootstrap/Button';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faBan, faHouse, faRightToBracket } from '@fortawesome/free-solid-svg-icons';
import AppNavbar from '../components/AppNavbar.js';
import Footer from '../components/Footer.js';

export default function AccessDeniedPage() {
  return (
    <>
      <AppNavbar />
      <main className="flex-grow-1 d-flex align-items-center justify-content-center py-5">
        <Container className="text-center">
          <div className="display-3 text-warning mb-3">
            <FontAwesomeIcon icon={faBan} />
          </div>
          <h1 className="h3 mb-2">Access denied</h1>
          <p className="text-muted mb-4">You don’t have permission to view this page.</p>
          <div className="d-flex gap-2 justify-content-center flex-wrap">
            <Button variant="primary" href="/" className="d-inline-flex align-items-center gap-2">
              <FontAwesomeIcon icon={faHouse} />
              <span>Back to home</span>
            </Button>
            <Button variant="outline-secondary" href="/login" className="d-inline-flex align-items-center gap-2">
              <FontAwesomeIcon icon={faRightToBracket} />
              <span>Sign in</span>
            </Button>
          </div>
        </Container>
      </main>
      <Footer />
    </>
  );
}
