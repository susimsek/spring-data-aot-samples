'use client';

import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import Api from '@lib/api';
import { clearStoredUser, loadStoredUser, persistUser } from '@lib/auth';
import type { ApiErrorPayload, AuthStatus, StoredUser } from '@lib/types';

export interface AuthState {
  user: StoredUser | null;
  status: AuthStatus;
  sessionChecked: boolean;
  error: string | null;
}

const initialUser = loadStoredUser();

const initialState: AuthState = {
  user: initialUser,
  status: initialUser ? 'succeeded' : 'idle',
  sessionChecked: !initialUser,
  error: null,
};

function normalizeError(err: unknown): ApiErrorPayload {
  if (typeof err === 'string') return { message: err };
  if (typeof err === 'number' || typeof err === 'boolean' || typeof err === 'bigint') return { message: String(err) };
  if (err instanceof Error) return { message: err.message };
  if (!err || typeof err !== 'object') return { message: 'Request failed' };
  const anyErr = err as { message?: unknown; status?: unknown; title?: unknown; body?: unknown };
  if (typeof anyErr.message === 'string') {
    return {
      message: anyErr.message,
      status: typeof anyErr.status === 'number' ? anyErr.status : undefined,
      title: typeof anyErr.title === 'string' ? anyErr.title : undefined,
      body: anyErr.body,
    };
  }

  const status = typeof anyErr.status === 'number' ? anyErr.status : undefined;
  const title = typeof anyErr.title === 'string' ? anyErr.title : undefined;
  const body = anyErr.body;

  if (typeof body === 'string' && body) {
    return { message: body, status, title, body };
  }

  try {
    const serialized = JSON.stringify(err);
    if (serialized && serialized !== '{}' && serialized !== '[]') {
      return { message: serialized, status, title, body };
    }
  } catch {
    // ignore
  }

  return { message: 'Request failed', status, title, body };
}

export interface LoginPayload {
  username: string;
  password: string;
  rememberMe?: boolean;
}

export const loginUser = createAsyncThunk<StoredUser, LoginPayload, { rejectValue: ApiErrorPayload }>(
  'auth/login',
  async (payload: LoginPayload, { rejectWithValue }) => {
    try {
      await Api.login(payload);
      const user = await Api.currentUser();
      persistUser(user);
      return user;
    } catch (err) {
      return rejectWithValue(normalizeError(err));
    }
  },
);

export const verifySession = createAsyncThunk<StoredUser, void, { rejectValue: ApiErrorPayload }>(
  'auth/verifySession',
  async (_: void, { rejectWithValue }) => {
    try {
      const user = await Api.currentUser();
      persistUser(user);
      return user;
    } catch (err) {
      clearStoredUser();
      return rejectWithValue(normalizeError(err));
    }
  },
);

export const logoutUser = createAsyncThunk<void, void, { rejectValue: ApiErrorPayload }>(
  'auth/logout',
  async (_: void, { rejectWithValue }) => {
    try {
      await Api.logout();
    } catch (err) {
      return rejectWithValue(normalizeError(err));
    } finally {
      clearStoredUser();
    }
  },
);

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    clearUser(state) {
      state.user = null;
      state.status = 'idle';
      state.sessionChecked = true;
      state.error = null;
      clearStoredUser();
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(loginUser.pending, (state) => {
        state.status = 'loading';
        state.sessionChecked = true;
        state.error = null;
      })
      .addCase(loginUser.fulfilled, (state, action) => {
        state.status = 'succeeded';
        state.user = action.payload || null;
        state.sessionChecked = true;
        state.error = null;
      })
      .addCase(loginUser.rejected, (state, action) => {
        state.status = 'failed';
        state.sessionChecked = true;
        state.error = action.payload?.message || action.error?.message || 'Login failed';
      })
      .addCase(verifySession.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(verifySession.fulfilled, (state, action) => {
        state.status = 'succeeded';
        state.user = action.payload || null;
        state.sessionChecked = true;
        state.error = null;
      })
      .addCase(verifySession.rejected, (state) => {
        state.status = 'idle';
        state.user = null;
        state.sessionChecked = true;
      })
      .addCase(logoutUser.fulfilled, (state) => {
        state.status = 'idle';
        state.user = null;
        state.sessionChecked = true;
        state.error = null;
      })
      .addCase(logoutUser.rejected, (state) => {
        state.status = 'idle';
        state.user = null;
        state.sessionChecked = true;
      });
  },
});

export const { clearUser } = authSlice.actions;

export default authSlice.reducer;
