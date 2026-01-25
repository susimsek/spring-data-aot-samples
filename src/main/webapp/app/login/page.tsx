import { Suspense } from 'react';
import LoginPageClient from './LoginPageClient';

function LoginFallback() {
  return (
    <div className="min-vh-100 d-flex align-items-center justify-content-center bg-body-tertiary">
      <span className="spinner-border text-primary" role="status" aria-hidden="true" />
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={<LoginFallback />}>
      <LoginPageClient />
    </Suspense>
  );
}
