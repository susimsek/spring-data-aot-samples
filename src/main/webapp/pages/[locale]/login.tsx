'use client';

import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'next-i18next';
import Link from 'next/link';
import Container from 'react-bootstrap/Container';
import Card from 'react-bootstrap/Card';
import Form from 'react-bootstrap/Form';
import Button from 'react-bootstrap/Button';
import InputGroup from 'react-bootstrap/InputGroup';
import Alert from 'react-bootstrap/Alert';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faEye, faEyeSlash, faRightToBracket, faUser } from '@fortawesome/free-solid-svg-icons';
import { useForm } from 'react-hook-form';
import AppNavbar from '../../components/AppNavbar';
import Footer from '../../components/Footer';
import { useAppDispatch } from '../../lib/store';
import { loginUser } from '../../slices/authSlice';
import { useToasts } from '../../components/ToastProvider';
import { getLocation, replaceLocation } from '../../lib/window';
import { getLocalePrefix } from '../../lib/routes';
import { getStaticPaths, makeStaticProps } from '../../lib/getStatic';

interface LoginFormValues {
  username: string;
  password: string;
  rememberMe: boolean;
}

export default function LoginPageClient() {
  const dispatch = useAppDispatch();
  const { t } = useTranslation();

  const searchParams = useMemo(() => new URLSearchParams(getLocation()?.search || ''), []);
  const localePrefix = getLocalePrefix(getLocation()?.pathname || '');
  const registered = (searchParams.get('registered') ?? '') === '1';
  const redirect = searchParams.get('redirect') ?? '/';
  const { pushToast } = useToasts();

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isValid, isSubmitting },
  } = useForm<LoginFormValues>({
    mode: 'onChange',
    defaultValues: {
      username: '',
      password: '',
      rememberMe: false,
    },
  });

  const username = watch('username');
  const password = watch('password');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    setError('');
  }, [username, password]);

  const canSubmit = useMemo(() => isValid && username.trim().length > 0 && password.length > 0, [isValid, username, password]);

  const onSubmit = handleSubmit(async (data) => {
    setError('');
    try {
      await dispatch(
        loginUser({
          username: data.username.trim(),
          password: data.password,
          rememberMe: !!data.rememberMe,
        }),
      ).unwrap();
      pushToast(t('login.toast.signedIn'), 'success');
      replaceLocation(redirect || '/');
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : t('login.error.failed'));
    }
  });

  return (
    <div className="d-flex flex-column min-vh-100 bg-body-tertiary">
      <AppNavbar />
      <main className="flex-fill d-flex align-items-center justify-content-center py-5">
        <Container className="d-flex justify-content-center">
          <Card className="shadow-sm border-0" style={{ maxWidth: 420, width: '100%' }}>
            <Card.Body className="p-4">
              <h1 className="h5 mb-3 d-flex align-items-center gap-2">
                <FontAwesomeIcon icon={faUser} className="text-primary" />
                <span>{t('login.title')}</span>
              </h1>
              {registered ? <Alert variant="success">{t('register.accountCreatedSignIn')}</Alert> : null}
              {error ? <Alert variant="danger">{error}</Alert> : null}
              <Form onSubmit={onSubmit} noValidate>
                <Form.Group className="mb-3">
                  <Form.Label>{t('login.form.username.label')}</Form.Label>
                  <Form.Control
                    type="text"
                    placeholder={t('login.form.username.placeholder')}
                    isInvalid={!!errors.username}
                    {...register('username', {
                      required: t('validation.required'),
                    })}
                  />
                  {errors.username ? <Form.Control.Feedback type="invalid">{errors.username.message}</Form.Control.Feedback> : null}
                </Form.Group>
                <Form.Group className="mb-3">
                  <Form.Label>{t('login.form.password.label')}</Form.Label>
                  <InputGroup>
                    <Form.Control
                      type={showPassword ? 'text' : 'password'}
                      placeholder={t('login.form.password.placeholder')}
                      isInvalid={!!errors.password}
                      {...register('password', {
                        required: t('validation.required'),
                      })}
                    />
                    <Button
                      variant="outline-secondary"
                      onClick={() => setShowPassword((prev) => !prev)}
                      aria-label={t('login.form.password.toggleAria')}
                    >
                      <FontAwesomeIcon icon={showPassword ? faEyeSlash : faEye} />
                    </Button>
                  </InputGroup>
                  {errors.password ? (
                    <Form.Control.Feedback type="invalid" className="d-block">
                      {errors.password.message}
                    </Form.Control.Feedback>
                  ) : null}
                </Form.Group>
                <Form.Check type="checkbox" label={t('login.form.rememberMe')} className="mb-3" {...register('rememberMe')} />
                <Button type="submit" variant="primary" className="w-100" disabled={!canSubmit || isSubmitting}>
                  {isSubmitting ? (
                    <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true" />
                  ) : (
                    <FontAwesomeIcon icon={faRightToBracket} className="me-2" />
                  )}
                  {isSubmitting ? t('login.form.submitting') : t('login.form.submit')}
                </Button>
              </Form>
              <div className="text-muted small mt-3">{t('login.defaultUsers')}</div>
              <div className="mt-2">
                <Link className="link-secondary small" href={`${localePrefix}/register`}>
                  {t('login.link.register')}
                </Link>
              </div>
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
