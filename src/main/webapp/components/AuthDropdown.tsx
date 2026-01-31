'use client';

import Dropdown from 'react-bootstrap/Dropdown';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faUser, faKey, faRightFromBracket } from '@fortawesome/free-solid-svg-icons';
import { useTranslation } from 'next-i18next';
import useAuth from '../lib/useAuth';

export default function AuthDropdown({ showChangePassword = false }: Readonly<{ showChangePassword?: boolean }>) {
  const { user, isAuthenticated, logout } = useAuth();
  const { t } = useTranslation();

  if (!isAuthenticated) {
    return null;
  }

  return (
    <Dropdown align="end">
      <Dropdown.Toggle variant="primary" size="sm" className="d-inline-flex align-items-center gap-2">
        <FontAwesomeIcon icon={faUser} />
        <span>{user?.username || ''}</span>
      </Dropdown.Toggle>
      <Dropdown.Menu>
        <Dropdown.Header className="text-muted small">{t('auth.dropdown.signedInAs', { username: user?.username || '' })}</Dropdown.Header>
        {showChangePassword ? (
          <Dropdown.Item href="/change-password" className="d-flex align-items-center gap-2">
            <FontAwesomeIcon icon={faKey} />
            <span>{t('auth.dropdown.changePassword')}</span>
          </Dropdown.Item>
        ) : null}
        <Dropdown.Divider />
        <Dropdown.Item onClick={logout} className="d-flex align-items-center gap-2">
          <FontAwesomeIcon icon={faRightFromBracket} />
          <span>{t('auth.dropdown.signOut')}</span>
        </Dropdown.Item>
      </Dropdown.Menu>
    </Dropdown>
  );
}
