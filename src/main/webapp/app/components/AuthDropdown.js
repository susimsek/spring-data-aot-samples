'use client';

import Dropdown from 'react-bootstrap/Dropdown';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faUser, faKey, faRightFromBracket } from '@fortawesome/free-solid-svg-icons';
import useAuth from '../lib/useAuth.js';

export default function AuthDropdown({ showChangePassword = false }) {
  const { user, isAuthenticated, logout } = useAuth({ fetchUser: false });

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
        <Dropdown.Header className="text-muted small">Signed in as {user?.username || ''}</Dropdown.Header>
        {showChangePassword ? (
          <Dropdown.Item href="/change-password" className="d-flex align-items-center gap-2">
            <FontAwesomeIcon icon={faKey} />
            <span>Change password</span>
          </Dropdown.Item>
        ) : null}
        <Dropdown.Divider />
        <Dropdown.Item onClick={logout} className="d-flex align-items-center gap-2">
          <FontAwesomeIcon icon={faRightFromBracket} />
          <span>Sign out</span>
        </Dropdown.Item>
      </Dropdown.Menu>
    </Dropdown>
  );
}
