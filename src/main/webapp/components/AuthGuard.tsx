'use client';

import { type ReactNode, useEffect } from 'react';
import Spinner from 'react-bootstrap/Spinner';
import { useTranslation } from 'next-i18next';
import { useAppDispatch, useAppSelector } from '@lib/store';
import { getLocalePrefix, isPublicRoute } from '@lib/routes';
import { getLocation, replaceLocation } from '@lib/window';
import { verifySession } from '@slices/authSlice';

interface AuthGuardProps {
  children: ReactNode;
}

function buildLoginUrl(currentPath: string, queryString: string): string {
  const localePrefix = getLocalePrefix(currentPath);
  const redirectTarget = queryString ? `${currentPath}?${queryString}` : currentPath;
  const encodedRedirect = encodeURIComponent(redirectTarget);
  return `${localePrefix}/login?redirect=${encodedRedirect}`;
}

function getLocationInfo(): { path: string; queryString: string } {
  const location = getLocation();
  return {
    path: location?.pathname || '',
    queryString: location?.search?.replace(/^\?/, '') || '',
  };
}

export default function AuthGuard({ children }: Readonly<AuthGuardProps>) {
  const dispatch = useAppDispatch();
  const user = useAppSelector((state) => state.auth.user);
  const sessionChecked = useAppSelector((state) => state.auth.sessionChecked);
  const authStatus = useAppSelector((state) => state.auth.status);
  const isAuthenticated = !!user?.username;
  const { t } = useTranslation('common');

  const { path, queryString } = getLocationInfo();

  useEffect(() => {
    if (isPublicRoute(path)) return;

    if (!isAuthenticated) {
      const loginUrl = buildLoginUrl(path, queryString);
      replaceLocation(loginUrl);
      return;
    }

    if (!sessionChecked) {
      if (authStatus === 'loading') return;
      dispatch(verifySession())
        .unwrap()
        .catch(() => replaceLocation(buildLoginUrl(path, queryString)));
      return;
    }
  }, [authStatus, dispatch, isAuthenticated, path, queryString, sessionChecked]);

  const shouldRender = isPublicRoute(path) || (isAuthenticated && sessionChecked);

  if (!shouldRender) {
    return (
      <div className="min-vh-100 d-flex align-items-center justify-content-center bg-body-tertiary">
        <Spinner animation="border" variant="primary" role="status">
          <span className="visually-hidden">{t('common.loading')}</span>
        </Spinner>
      </div>
    );
  }

  return <>{children}</>;
}
