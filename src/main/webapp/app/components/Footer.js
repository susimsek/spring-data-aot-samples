import Container from 'react-bootstrap/Container';

export default function Footer() {
  return (
    <footer className="bg-body border-top py-3 mt-auto w-100">
      <Container className="d-flex justify-content-center gap-3 text-muted small">
        <span>&copy; 2025 Notes</span>
        <span>Spring Boot</span>
      </Container>
    </footer>
  );
}
