'use client';

import Button from 'react-bootstrap/Button';
import type { ButtonProps } from 'react-bootstrap/Button';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faMoon, faSun } from '@fortawesome/free-solid-svg-icons';
import { useTranslation } from 'next-i18next';
import { useTheme } from './ThemeProvider';

export default function ThemeToggleButton({ size = 'sm', className = '' }: Readonly<{ size?: ButtonProps['size']; className?: string }>) {
  const { theme, toggleTheme } = useTheme();
  const { t } = useTranslation();
  const isDark = theme === 'dark';

  return (
    <Button
      variant="outline-secondary"
      size={size}
      className={`d-inline-flex align-items-center gap-2 ${className}`.trim()}
      onClick={toggleTheme}
      aria-pressed={isDark}
      aria-label={isDark ? t('theme.switchToLightAria') : t('theme.switchToDarkAria')}
    >
      <FontAwesomeIcon icon={isDark ? faSun : faMoon} />
      <span>{isDark ? t('theme.light') : t('theme.dark')}</span>
    </Button>
  );
}
