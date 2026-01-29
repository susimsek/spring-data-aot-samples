'use client';

import { type ReactNode, useMemo } from 'react';
import Spinner from 'react-bootstrap/Spinner';
import { useAppSelector } from '../hooks';
import { isPublicRoute } from '../lib/routes';
import { replaceLocation } from '../lib/window';

interface AuthGuardProps {
  children: ReactNode;
}

function buildLoginUrl(currentPath: string, queryString: string): string {
  const redirectTarget = queryString ? `${currentPath}?${queryString}` : currentPath;
  const encodedRedirect = encodeURIComponent(redirectTarget);
  return `/login?redirect=${encodedRedirect}`;
}

function getLocationInfo(): { path: string; queryString: string } {
  const location = (globalThis as any).location as Location | undefined;
  return {
    path: location?.pathname || '',
    queryString: location?.search?.replace(/^\?/, '') || '',
  };
}

export default function AuthGuard({ children }: Readonly<AuthGuardProps>) {
  const user = useAppSelector((state) => state.auth.user);
  const isAuthenticated = !!user?.username;

  const shouldRender = useMemo(() => {
    const { path, queryString } = getLocationInfo();

    if (isPublicRoute(path)) {
      return true;
    }

    if (!isAuthenticated) {
      const loginUrl = buildLoginUrl(path, queryString);
      replaceLocation(loginUrl);
      return false;
    }

    return true;
  }, [isAuthenticated]);

  if (!shouldRender) {
    return (
      <div className="min-vh-100 d-flex align-items-center justify-content-center bg-body-tertiary">
        <Spinner animation="border" variant="primary" role="status">
          <span className="visually-hidden">Loading...</span>
        </Spinner>
      </div>
    );
  }

  return <>{children}</>;
}
