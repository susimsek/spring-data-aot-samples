'use client';

import Navbar from 'react-bootstrap/Navbar';
import Container from 'react-bootstrap/Container';
import Form from 'react-bootstrap/Form';
import InputGroup from 'react-bootstrap/InputGroup';
import Button from 'react-bootstrap/Button';
import Badge from 'react-bootstrap/Badge';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faBars, faHouse, faShareNodes, faXmark } from '@fortawesome/free-solid-svg-icons';
import { useTranslation } from 'next-i18next';
import ThemeToggleButton from './ThemeToggleButton';
import AuthDropdown from './AuthDropdown';
import Brand from './Brand';
import LanguageSelect from './LanguageSelect';
import useAuth from '../lib/useAuth';

export interface AppNavbarProps {
  search?: string;
  onSearchChange?: (next: string) => void;
  onSearchClear?: () => void;
  showSearch?: boolean;
  showSharedButton?: boolean;
  showHomeButton?: boolean;
  showAuthDropdown?: boolean;
  showChangePassword?: boolean;
  badgeLabel?: string;
  requireAuthForActions?: boolean;
  collapseId?: string;
}

export default function AppNavbar({
  search = '',
  onSearchChange,
  onSearchClear,
  showSearch = false,
  showSharedButton = true,
  showHomeButton = true,
  showAuthDropdown = true,
  showChangePassword = true,
  badgeLabel = '',
  requireAuthForActions = false,
  collapseId = 'appNavbar',
}: Readonly<AppNavbarProps>) {
  const { isAuthenticated } = useAuth();
  const { t } = useTranslation();
  const canShowActions = !requireAuthForActions || isAuthenticated;
  const showShared = showSharedButton && canShowActions;
  const showHome = showHomeButton && canShowActions;
  const showAuth = showAuthDropdown && canShowActions;
  const showBadge = Boolean(badgeLabel);
  const hasSearch = showSearch;
  const hasActions = hasSearch || showShared || showHome || showAuth;

  const brandBlock = (
    <div className="d-flex align-items-center gap-2">
      <Navbar.Brand href="/">
        <Brand />
      </Navbar.Brand>
      {showBadge ? (
        <Badge bg="secondary-subtle" text="secondary">
          {badgeLabel}
        </Badge>
      ) : null}
    </div>
  );

  const languageBlock = <LanguageSelect />;

  if (!hasActions) {
    return (
      <Navbar expand="lg" className="bg-body border-bottom shadow-sm">
        <Container className="d-flex align-items-center gap-3 flex-wrap justify-content-between">
          {brandBlock}
          <div className="ms-auto d-flex align-items-center gap-2">
            {languageBlock}
            <ThemeToggleButton size="sm" />
          </div>
        </Container>
      </Navbar>
    );
  }

  return (
    <Navbar expand="lg" className="bg-body border-bottom shadow-sm">
      <Container>
        {brandBlock}
        <Navbar.Toggle aria-controls={collapseId}>
          <FontAwesomeIcon icon={faBars} />
        </Navbar.Toggle>
        <Navbar.Collapse id={collapseId}>
          {hasSearch ? (
            <div className="d-flex flex-column flex-lg-row align-items-center gap-2 w-100 justify-content-center justify-content-lg-end">
              <InputGroup size="sm" className="w-100" style={{ maxWidth: 360 }}>
                <Form.Control
                  type="text"
                  placeholder={t('nav.search.placeholder')}
                  value={search}
                  onChange={(e) => onSearchChange?.(e.target.value)}
                />
                <Button variant="outline-secondary" onClick={onSearchClear} aria-label={t('nav.search.clearAria')}>
                  <FontAwesomeIcon icon={faXmark} />
                </Button>
              </InputGroup>
              <div className="d-flex align-items-center gap-2 flex-column flex-lg-row justify-content-center justify-content-lg-end order-2 order-lg-2 w-100">
                {showHome ? (
                  <Button variant="outline-secondary" size="sm" className="d-inline-flex align-items-center gap-2 flex-shrink-0" href="/">
                    <FontAwesomeIcon icon={faHouse} />
                    <span>{t('nav.home')}</span>
                  </Button>
                ) : null}
                {showShared ? (
                  <Button
                    variant="outline-secondary"
                    size="sm"
                    className="d-inline-flex align-items-center gap-2 flex-shrink-0"
                    href="/shared-links"
                  >
                    <FontAwesomeIcon icon={faShareNodes} />
                    <span>{t('nav.shared')}</span>
                  </Button>
                ) : null}
                {languageBlock}
                <ThemeToggleButton size="sm" />
                {showAuth ? <AuthDropdown showChangePassword={showChangePassword} /> : null}
              </div>
            </div>
          ) : (
            <div className="d-flex align-items-center gap-2 flex-lg-row flex-column flex-wrap ms-lg-auto w-100 justify-content-lg-end">
              {showHome ? (
                <Button variant="outline-secondary" size="sm" href="/" className="d-inline-flex align-items-center gap-2">
                  <FontAwesomeIcon icon={faHouse} />
                  <span>{t('nav.home')}</span>
                </Button>
              ) : null}
              {showShared ? (
                <Button variant="outline-secondary" size="sm" href="/shared-links" className="d-inline-flex align-items-center gap-2">
                  <FontAwesomeIcon icon={faShareNodes} />
                  <span>{t('nav.shared')}</span>
                </Button>
              ) : null}
              {languageBlock}
              <ThemeToggleButton size="sm" />
              {showAuth ? <AuthDropdown showChangePassword={showChangePassword} /> : null}
            </div>
          )}
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
}
