'use client';

import type { ReactNode } from 'react';
import Navbar from 'react-bootstrap/Navbar';
import Container from 'react-bootstrap/Container';
import Form from 'react-bootstrap/Form';
import InputGroup from 'react-bootstrap/InputGroup';
import Badge from 'react-bootstrap/Badge';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faBars, faHouse, faRightToBracket, faShareNodes, faXmark } from '@fortawesome/free-solid-svg-icons';
import { useTranslation } from 'next-i18next';
import ThemeToggleButton from './ThemeToggleButton';
import AuthDropdown from './AuthDropdown';
import Brand from './Brand';
import LanguageSelect from './LanguageSelect';
import Link from './Link';
import useAuth from '@lib/useAuth';

export interface AppNavbarProps {
  search?: string;
  onSearchChange?: (next: string) => void;
  onSearchClear?: () => void;
  showSearch?: boolean;
  showSharedButton?: boolean;
  showHomeButton?: boolean;
  badgeLabel?: string;
  requireAuthForActions?: boolean;
  showSignInButton?: boolean;
  collapseId?: string;
}

function BrandBlock({ badgeLabel }: Readonly<{ badgeLabel: string }>) {
  return (
    <div className="d-flex align-items-center gap-2">
      <Navbar.Brand as="div">
        <Link href="/" className="text-decoration-none">
          <Brand />
        </Link>
      </Navbar.Brand>
      {badgeLabel ? (
        <Badge bg="secondary-subtle" text="secondary">
          {badgeLabel}
        </Badge>
      ) : null}
    </div>
  );
}

function SearchInput({
  search,
  onSearchChange,
  onSearchClear,
  placeholder,
  clearAriaLabel,
}: Readonly<{
  search: string;
  onSearchChange?: (next: string) => void;
  onSearchClear?: () => void;
  placeholder: string;
  clearAriaLabel: string;
}>) {
  return (
    <InputGroup size="sm" className="w-100" style={{ maxWidth: 360 }}>
      <Form.Control type="text" placeholder={placeholder} value={search} onChange={(event) => onSearchChange?.(event.target.value)} />
      <button type="button" className="btn btn-outline-secondary btn-sm" onClick={onSearchClear} aria-label={clearAriaLabel}>
        <FontAwesomeIcon icon={faXmark} />
      </button>
    </InputGroup>
  );
}

function HomeLink({ show, label }: Readonly<{ show: boolean; label: string }>) {
  if (!show) return null;
  return (
    <Link href="/" className="btn btn-outline-secondary btn-sm d-inline-flex align-items-center gap-2 flex-shrink-0">
      <FontAwesomeIcon icon={faHouse} />
      <span>{label}</span>
    </Link>
  );
}

function SharedLinksButton({ show, label }: Readonly<{ show: boolean; label: string }>) {
  if (!show) return null;
  return (
    <Link href="/shared-links" className="btn btn-outline-secondary btn-sm d-inline-flex align-items-center gap-2 flex-shrink-0">
      <FontAwesomeIcon icon={faShareNodes} />
      <span>{label}</span>
    </Link>
  );
}

function SignInLink({ show, label }: Readonly<{ show: boolean; label: string }>) {
  if (!show) return null;
  return (
    <Link href="/login" className="btn btn-primary btn-sm d-inline-flex align-items-center gap-2 flex-shrink-0">
      <FontAwesomeIcon icon={faRightToBracket} />
      <span>{label}</span>
    </Link>
  );
}

function AuthDropdownBlock({ show }: Readonly<{ show: boolean }>) {
  if (!show) return null;
  return <AuthDropdown />;
}

function NavbarActions({
  showHome,
  showShared,
  showSignIn,
  showAuth,
  homeLabel,
  sharedLabel,
  signInLabel,
}: Readonly<{
  showHome: boolean;
  showShared: boolean;
  showSignIn: boolean;
  showAuth: boolean;
  homeLabel: string;
  sharedLabel: string;
  signInLabel: string;
}>) {
  return (
    <>
      <HomeLink show={showHome} label={homeLabel} />
      <SharedLinksButton show={showShared} label={sharedLabel} />
      <LanguageSelect />
      <ThemeToggleButton size="sm" />
      <SignInLink show={showSignIn} label={signInLabel} />
      <AuthDropdownBlock show={showAuth} />
    </>
  );
}

function CollapseContentWithSearch({
  search,
  onSearchChange,
  onSearchClear,
  searchPlaceholder,
  searchClearAriaLabel,
  actions,
}: Readonly<{
  search: string;
  onSearchChange?: (next: string) => void;
  onSearchClear?: () => void;
  searchPlaceholder: string;
  searchClearAriaLabel: string;
  actions: ReactNode;
}>) {
  return (
    <div className="d-flex flex-column flex-lg-row align-items-center gap-2 w-100 justify-content-center justify-content-lg-end">
      <SearchInput
        search={search}
        onSearchChange={onSearchChange}
        onSearchClear={onSearchClear}
        placeholder={searchPlaceholder}
        clearAriaLabel={searchClearAriaLabel}
      />
      <div className="d-flex align-items-center gap-2 flex-column flex-lg-row justify-content-center justify-content-lg-end order-2 order-lg-2 w-100">
        {actions}
      </div>
    </div>
  );
}

function CollapseContentWithoutSearch({ actions }: Readonly<{ actions: ReactNode }>) {
  return (
    <div className="d-flex align-items-center gap-2 flex-lg-row flex-column flex-wrap ms-lg-auto w-100 justify-content-lg-end">
      {actions}
    </div>
  );
}

export default function AppNavbar({
  search = '',
  onSearchChange,
  onSearchClear,
  showSearch = false,
  showSharedButton = true,
  showHomeButton = true,
  badgeLabel = '',
  requireAuthForActions = false,
  showSignInButton = false,
  collapseId = 'appNavbar',
}: Readonly<AppNavbarProps>) {
  const { isAuthenticated } = useAuth();
  const { t } = useTranslation();
  const canShowActions = !requireAuthForActions || isAuthenticated;
  const showShared = showSharedButton && canShowActions;
  const showHome = showHomeButton && canShowActions;
  const showAuth = isAuthenticated && canShowActions;
  const showSignIn = showSignInButton && !isAuthenticated;
  const hasActions = showSearch || showShared || showHome || showAuth || showSignIn;

  const actions = (
    <NavbarActions
      showHome={showHome}
      showShared={showShared}
      showSignIn={showSignIn}
      showAuth={showAuth}
      homeLabel={t('nav.home')}
      sharedLabel={t('nav.shared')}
      signInLabel={t('common.signIn')}
    />
  );

  if (!hasActions) {
    return (
      <Navbar expand="lg" className="bg-body border-bottom shadow-sm">
        <Container className="d-flex align-items-center gap-3 flex-wrap justify-content-between">
          <BrandBlock badgeLabel={badgeLabel} />
          <div className="ms-auto d-flex align-items-center gap-2">
            <LanguageSelect />
            <ThemeToggleButton size="sm" />
          </div>
        </Container>
      </Navbar>
    );
  }

  return (
    <Navbar expand="lg" className="bg-body border-bottom shadow-sm">
      <Container>
        <BrandBlock badgeLabel={badgeLabel} />
        <Navbar.Toggle aria-controls={collapseId}>
          <FontAwesomeIcon icon={faBars} />
        </Navbar.Toggle>
        <Navbar.Collapse id={collapseId}>
          {showSearch ? (
            <CollapseContentWithSearch
              search={search}
              onSearchChange={onSearchChange}
              onSearchClear={onSearchClear}
              searchPlaceholder={t('nav.search.placeholder')}
              searchClearAriaLabel={t('nav.search.clearAria')}
              actions={actions}
            />
          ) : (
            <CollapseContentWithoutSearch actions={actions} />
          )}
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
}
