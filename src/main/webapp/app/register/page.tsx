'use client';

import { useEffect, useMemo, useRef, useState } from 'react';
import Container from 'react-bootstrap/Container';
import Card from 'react-bootstrap/Card';
import Form from 'react-bootstrap/Form';
import Button from 'react-bootstrap/Button';
import InputGroup from 'react-bootstrap/InputGroup';
import Alert from 'react-bootstrap/Alert';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faEye, faEyeSlash, faUserPlus } from '@fortawesome/free-solid-svg-icons';
import { useForm } from 'react-hook-form';
import AppNavbar from '../components/AppNavbar';
import Footer from '../components/Footer';
import Api, { ApiError } from '../lib/api';
import { replaceLocation } from '../lib/window';

const usernamePattern = /^[a-zA-Z0-9_-]+$/;
const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/;

interface RegisterFormValues {
  username: string;
  email: string;
  password: string;
  confirm: string;
}

type ServerField = '' | 'username' | 'email';

export default function RegisterPage() {
  const {
    register,
    handleSubmit,
    watch,
    setError: setFieldError,
    clearErrors,
    formState: { errors, isValid, isSubmitting },
  } = useForm<RegisterFormValues>({
    mode: 'onChange',
    defaultValues: {
      username: '',
      email: '',
      password: '',
      confirm: '',
    },
  });

  const username = watch('username');
  const email = watch('email');
  const password = watch('password');
  const confirm = watch('confirm');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
  const [serverField, setServerField] = useState<ServerField>('');
  const serverFieldValueRef = useRef<string | null>(null);

  useEffect(() => {
    setError('');
  }, [username, email, password, confirm]);

  useEffect(() => {
    if (!serverField) {
      serverFieldValueRef.current = null;
      return;
    }
    const fieldValues: RegisterFormValues = { username, email, password, confirm };
    const current = fieldValues[serverField] ?? '';
    if (serverFieldValueRef.current == null) {
      serverFieldValueRef.current = current;
      return;
    }
    if (current !== serverFieldValueRef.current) {
      clearErrors(serverField);
      setServerField('');
      serverFieldValueRef.current = null;
    }
  }, [serverField, username, email, password, confirm, clearErrors]);

  const usernameValid = username.trim().length >= 3 && username.trim().length <= 50 && usernamePattern.test(username.trim());
  const emailValid = email.trim().length > 0 && email.trim().length <= 255;
  const passwordValid = password.length >= 8 && password.length <= 64 && passwordPattern.test(password);
  const confirmValid = confirm.length >= 8 && confirm.length <= 64 && confirm === password;

  const canSubmit = useMemo(
    () => isValid && usernameValid && emailValid && passwordValid && confirmValid,
    [isValid, usernameValid, emailValid, passwordValid, confirmValid],
  );

  const onSubmit = handleSubmit(async (data) => {
    setError('');
    try {
      await Api.register({
        username: data.username.trim(),
        email: data.email.trim(),
        password: data.password,
      });
      replaceLocation('/login?registered=1');
    } catch (err: unknown) {
      const apiErr = err instanceof ApiError ? err : null;
      const body = (apiErr?.body ?? {}) as { detail?: unknown; field?: unknown };
      const message = (typeof body.detail === 'string' && body.detail) || apiErr?.message || 'Registration failed';
      const field = body.field;
      if (field === 'username' || field === 'email') {
        setFieldError(field, { type: 'server', message });
        setServerField(field);
        return;
      }
      setError(message);
    }
  });

  return (
    <div className="d-flex flex-column min-vh-100 bg-body-tertiary">
      <AppNavbar />
      <main className="flex-fill d-flex align-items-center justify-content-center py-5">
        <Container className="d-flex justify-content-center">
          <Card className="shadow-sm" style={{ minWidth: 360, maxWidth: 460, width: '100%' }}>
            <Card.Body className="p-4">
              <div className="d-flex align-items-center mb-3 gap-2">
                <FontAwesomeIcon icon={faUserPlus} className="text-primary fs-4" />
                <h1 className="h5 mb-0">Create your account</h1>
              </div>
              {error ? <Alert variant="danger">{error}</Alert> : null}
              <Form onSubmit={onSubmit} noValidate>
                <Form.Group className="mb-3">
                  <Form.Label>Username</Form.Label>
                  <Form.Control
                    type="text"
                    autoComplete="username"
                    isInvalid={!!errors.username}
                    placeholder="Username"
                    {...register('username', {
                      required: 'This field is required.',
                      minLength: { value: 3, message: 'Username must be at least 3 characters.' },
                      maxLength: { value: 50, message: 'Username must be at most 50 characters.' },
                      pattern: {
                        value: usernamePattern,
                        message: 'Username can include letters, numbers, underscores, and hyphens only.',
                      },
                    })}
                  />
                  {errors.username ? <Form.Control.Feedback type="invalid">{errors.username.message}</Form.Control.Feedback> : null}
                </Form.Group>
                <Form.Group className="mb-3">
                  <Form.Label>Email</Form.Label>
                  <Form.Control
                    type="email"
                    autoComplete="email"
                    isInvalid={!!errors.email}
                    placeholder="Email"
                    {...register('email', {
                      required: 'This field is required.',
                      maxLength: {
                        value: 255,
                        message: 'Please enter a valid email address (max 255 chars).',
                      },
                    })}
                  />
                  {errors.email ? <Form.Control.Feedback type="invalid">{errors.email.message}</Form.Control.Feedback> : null}
                </Form.Group>
                <Form.Group className="mb-3">
                  <Form.Label>Password</Form.Label>
                  <InputGroup>
                    <Form.Control
                      type={showPassword ? 'text' : 'password'}
                      autoComplete="new-password"
                      isInvalid={!!errors.password}
                      placeholder="Password"
                      {...register('password', {
                        required: 'This field is required.',
                        minLength: { value: 8, message: 'Password must be at least 8 characters.' },
                        maxLength: {
                          value: 64,
                          message: 'Password must be at most 64 characters.',
                        },
                        validate: (value) => passwordPattern.test(value) || 'Password must include upper, lower, and digit.',
                      })}
                    />
                    <Button variant="outline-secondary" onClick={() => setShowPassword((prev) => !prev)} aria-label="Show password">
                      <FontAwesomeIcon icon={showPassword ? faEyeSlash : faEye} />
                    </Button>
                  </InputGroup>
                  {errors.password ? (
                    <Form.Control.Feedback type="invalid" className="d-block">
                      {errors.password.message}
                    </Form.Control.Feedback>
                  ) : null}
                </Form.Group>
                <Form.Group className="mb-3">
                  <Form.Label>Confirm password</Form.Label>
                  <Form.Control
                    type="password"
                    autoComplete="new-password"
                    isInvalid={!!errors.confirm}
                    placeholder="Confirm password"
                    {...register('confirm', {
                      required: 'This field is required.',
                      validate: (value) => value === watch('password') || 'Passwords do not match.',
                    })}
                  />
                  {errors.confirm ? <Form.Control.Feedback type="invalid">{errors.confirm.message}</Form.Control.Feedback> : null}
                </Form.Group>
                <Button type="submit" variant="primary" className="w-100" disabled={!canSubmit || isSubmitting}>
                  <FontAwesomeIcon icon={faUserPlus} className="me-2" />
                  {isSubmitting ? 'Creating...' : 'Create account'}
                </Button>
              </Form>
              <div className="d-flex justify-content-center mt-3">
                <a className="link-secondary small" href="/login">
                  Already have an account? Sign in
                </a>
              </div>
            </Card.Body>
          </Card>
        </Container>
      </main>
      <Footer />
    </div>
  );
}
