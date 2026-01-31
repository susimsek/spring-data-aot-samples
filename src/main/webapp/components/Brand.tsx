'use client';

import Image from 'next/image';
import { useTranslation } from 'next-i18next';

export default function Brand() {
  const { t } = useTranslation();
  return (
    <span className="d-flex align-items-center gap-2">
      <Image src="/favicon.svg" alt={t('brand.logoAlt')} width={28} height={28} />
      <span>{t('brand.name')}</span>
    </span>
  );
}
