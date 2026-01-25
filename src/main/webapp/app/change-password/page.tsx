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
import AppNavbar from '../components/AppNavbar';
import Footer from '../components/Footer';
import Api, { ApiError } from '../lib/api';
import { useAppDispatch } from '../hooks';
import { clearUser } from '../slices/authSlice';

const passwordPattern = /^(?=.*[A-ZÇĞİÖŞÜ])(?=.*[a-zçğıöşü])(?=.*\d).+$/;

interface ChangePasswordFormValues {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

export default function ChangePasswordPage() {
  const dispatch = useAppDispatch();
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

  const onSubmit = handleSubmit(async data => {
    setError('');
    try {
      await Api.changePassword({
        currentPassword: data.currentPassword,
        newPassword: data.newPassword,
      });
      dispatch(clearUser());
      window.location.replace('/login');
    } catch (err: unknown) {
      const apiErr = err instanceof ApiError ? err : null;
      const body = (apiErr?.body ?? {}) as { detail?: unknown };
      const message = (typeof body.detail === 'string' && body.detail) || apiErr?.message || 'Password update failed.';
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
                <h1 className="h5 mb-0">Change password</h1>
              </div>
              {error ? <Alert variant="danger">{error}</Alert> : null}
              <Form onSubmit={onSubmit} noValidate>
                <Form.Group className="mb-3">
                  <Form.Label>Current password</Form.Label>
                  <InputGroup>
                    <Form.Control
                      type={showCurrent ? 'text' : 'password'}
                      isInvalid={!!errors.currentPassword}
                      {...register('currentPassword', { required: 'This field is required.' })}
                    />
                    <Button variant="outline-secondary" onClick={() => setShowCurrent(prev => !prev)}>
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
                  <Form.Label>New password</Form.Label>
                  <InputGroup>
                    <Form.Control
                      type={showNew ? 'text' : 'password'}
                      isInvalid={!!errors.newPassword}
                      {...register('newPassword', {
                        required: 'This field is required.',
                        minLength: { value: 8, message: 'Password must be at least 8 characters.' },
                        maxLength: {
                          value: 64,
                          message: 'Password must be at most 64 characters.',
                        },
                        validate: value => passwordPattern.test(value) || 'Password must include upper, lower, and digit.',
                      })}
                    />
                    <Button variant="outline-secondary" onClick={() => setShowNew(prev => !prev)}>
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
                  <Form.Label>Confirm password</Form.Label>
                  <Form.Control
                    type="password"
                    isInvalid={!!errors.confirmPassword}
                    {...register('confirmPassword', {
                      required: 'This field is required.',
                      validate: value => value === watch('newPassword') || 'Passwords do not match.',
                    })}
                  />
                  {errors.confirmPassword ? (
                    <Form.Control.Feedback type="invalid" className="d-block">
                      {errors.confirmPassword.message}
                    </Form.Control.Feedback>
                  ) : null}
                </Form.Group>
                <Button type="submit" variant="primary" className="w-100" disabled={!canSubmit || isSubmitting}>
                  {isSubmitting ? 'Updating...' : 'Update password'}
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
