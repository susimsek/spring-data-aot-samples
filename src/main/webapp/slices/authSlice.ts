'use client';

import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import Api from '@lib/api';
import { clearStoredUser, loadStoredUser, persistUser } from '@lib/auth';
import type { ApiErrorPayload, AuthStatus, StoredUser } from '@lib/types';

export interface AuthState {
  user: StoredUser | null;
  status: AuthStatus;
  error: string | null;
}

const initialUser = loadStoredUser();

const initialState: AuthState = {
  user: initialUser,
  status: initialUser ? 'succeeded' : 'idle',
  error: null,
};

function normalizeError(err: unknown): ApiErrorPayload {
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
  return { message: String(err) };
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
      state.error = null;
      clearStoredUser();
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(loginUser.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(loginUser.fulfilled, (state, action) => {
        state.status = 'succeeded';
        state.user = action.payload || null;
        state.error = null;
      })
      .addCase(loginUser.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.payload?.message || action.error?.message || 'Login failed';
      })
      .addCase(logoutUser.fulfilled, (state) => {
        state.status = 'idle';
        state.user = null;
        state.error = null;
      })
      .addCase(logoutUser.rejected, (state) => {
        state.status = 'idle';
        state.user = null;
      });
  },
});

export const { clearUser } = authSlice.actions;

export default authSlice.reducer;
