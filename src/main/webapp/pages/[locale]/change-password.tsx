'use client';

import { useEffect, useMemo, useState } from 'react';
import Container from 'react-bootstrap/Container';
import Card from 'react-bootstrap/Card';
import Form from 'react-bootstrap/Form';
import Button from 'react-bootstrap/Button';
import InputGroup from 'react-bootstrap/InputGroup';
import Alert from 'react-bootstrap/Alert';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faEye, faEyeSlash, faKey } from '@fortawesome/free-solid-svg-icons';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'next-i18next';
import AppNavbar from '@components/AppNavbar';
import Footer from '@components/Footer';
import Api, { ApiError } from '@lib/api';
import { getStaticPaths, makeStaticProps } from '@lib/getStatic';
import { useAppDispatch } from '@lib/store';
import { replaceLocation } from '@lib/window';
import { clearUser } from '@slices/authSlice';

const passwordPattern = /^(?=.*[A-ZÇĞİÖŞÜ])(?=.*[a-zçğıöşü])(?=.*\d).+$/;

interface ChangePasswordFormValues {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export default function ChangePasswordPage() {
  const dispatch = useAppDispatch();
  const { t } = useTranslation();
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isValid, isSubmitting },
  } = useForm<ChangePasswordFormValues>({
    mode: 'onChange',
    defaultValues: {
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    },
  });

  const currentPassword = watch('currentPassword');
  const newPassword = watch('newPassword');
  const confirmPassword = watch('confirmPassword');
  const [showCurrent, setShowCurrent] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [error, setError] = useState('');

  const canSubmit = useMemo(
    () => isValid && currentPassword.length > 0 && newPassword.length > 0 && confirmPassword.length > 0,
    [confirmPassword.length, currentPassword.length, isValid, newPassword.length],
  );

  useEffect(() => {
    setError('');
  }, [currentPassword, newPassword, confirmPassword]);

  const onSubmit = handleSubmit(async (data) => {
    setError('');
    try {
      await Api.changePassword({
        currentPassword: data.currentPassword,
        newPassword: data.newPassword,
      });
      dispatch(clearUser());
      replaceLocation('/login');
    } catch (err: unknown) {
      const apiErr = err instanceof ApiError ? err : null;
      const body = (apiErr?.body ?? {}) as { detail?: unknown };
      const message = (typeof body.detail === 'string' && body.detail) || apiErr?.message || t('changePassword.error.failed');
      setError(message);
    }
  });

  return (
    <div className="d-flex flex-column min-vh-100 bg-body-tertiary">
      <AppNavbar />
      <main className="flex-fill d-flex align-items-center justify-content-center py-5">
        <Container className="d-flex justify-content-center">
          <Card className="shadow-sm" style={{ minWidth: 360, maxWidth: 480, width: '100%' }}>
            <Card.Body className="p-4">
              <div className="d-flex align-items-center mb-3 gap-2">
                <FontAwesomeIcon icon={faKey} className="text-primary fs-4" />
                <h1 className="h5 mb-0">{t('changePassword.title')}</h1>
              </div>
              {error ? <Alert variant="danger">{error}</Alert> : null}
              <Form onSubmit={onSubmit} noValidate>
                <Form.Group className="mb-3">
                  <Form.Label>{t('changePassword.form.current.label')}</Form.Label>
                  <InputGroup>
                    <Form.Control
                      type={showCurrent ? 'text' : 'password'}
                      isInvalid={!!errors.currentPassword}
                      {...register('currentPassword', { required: t('validation.required') })}
                    />
                    <Button
                      variant="outline-secondary"
                      onClick={() => setShowCurrent((prev) => !prev)}
                      aria-label={t('common.togglePasswordVisibility')}
                    >
                      <FontAwesomeIcon icon={showCurrent ? faEyeSlash : faEye} />
                    </Button>
                  </InputGroup>
                  {errors.currentPassword ? (
                    <Form.Control.Feedback type="invalid" className="d-block">
                      {errors.currentPassword.message}
                    </Form.Control.Feedback>
                  ) : null}
                </Form.Group>
                <Form.Group className="mb-3">
                  <Form.Label>{t('changePassword.form.new.label')}</Form.Label>
                  <InputGroup>
                    <Form.Control
                      type={showNew ? 'text' : 'password'}
                      isInvalid={!!errors.newPassword}
                      {...register('newPassword', {
                        required: t('validation.required'),
                        minLength: { value: 8, message: t('validation.password.minLength') },
                        maxLength: {
                          value: 64,
                          message: t('validation.password.maxLength'),
                        },
                        validate: (value) => passwordPattern.test(value) || t('validation.password.complexity'),
                      })}
                    />
                    <Button
                      variant="outline-secondary"
                      onClick={() => setShowNew((prev) => !prev)}
                      aria-label={t('common.togglePasswordVisibility')}
                    >
                      <FontAwesomeIcon icon={showNew ? faEyeSlash : faEye} />
                    </Button>
                  </InputGroup>
                  {errors.newPassword ? (
                    <Form.Control.Feedback type="invalid" className="d-block">
                      {errors.newPassword.message}
                    </Form.Control.Feedback>
                  ) : null}
                </Form.Group>
                <Form.Group className="mb-3">
                  <Form.Label>{t('changePassword.form.confirm.label')}</Form.Label>
                  <Form.Control
                    type="password"
                    isInvalid={!!errors.confirmPassword}
                    {...register('confirmPassword', {
                      required: t('validation.required'),
                      validate: (value) => value === watch('newPassword') || t('validation.password.match'),
                    })}
                  />
                  {errors.confirmPassword ? (
                    <Form.Control.Feedback type="invalid" className="d-block">
                      {errors.confirmPassword.message}
                    </Form.Control.Feedback>
                  ) : null}
                </Form.Group>
                <Button type="submit" variant="primary" className="w-100" disabled={!canSubmit || isSubmitting}>
                  {isSubmitting ? t('changePassword.form.submitting') : t('changePassword.form.submit')}
                </Button>
              </Form>
            </Card.Body>
          </Card>
        </Container>
      </main>
      <Footer />
    </div>
  );
}

const getStaticProps = makeStaticProps(['common']);

export { getStaticPaths, getStaticProps };
