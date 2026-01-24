import { Suspense } from 'react';
import SharePageClient from './SharePageClient.js';

function ShareFallback() {
  return (
    <div className="min-vh-100 d-flex align-items-center justify-content-center bg-body-tertiary">
      <span className="spinner-border text-primary" role="status" aria-hidden="true" />
    </div>
  );
}

export default function SharePage() {
  return (
    <Suspense fallback={<ShareFallback />}>
      <SharePageClient />
    </Suspense>
  );
}
