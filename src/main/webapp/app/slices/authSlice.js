'use client';

import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import Api from '../lib/api.js';
import { clearStoredUser, loadStoredUser, persistUser } from '../lib/auth.js';

const initialUser = loadStoredUser();

const initialState = {
  user: initialUser,
  status: initialUser ? 'succeeded' : 'idle',
  error: null,
};

function normalizeError(err) {
  if (!err) return { message: 'Request failed' };
  if (err.message) return { message: err.message, status: err.status, title: err.title, body: err.body };
  return { message: String(err) };
}

export const fetchCurrentUser = createAsyncThunk('auth/fetchCurrentUser', async (_, { rejectWithValue }) => {
  try {
    const user = await Api.currentUser();
    persistUser(user);
    return user;
  } catch (err) {
    clearStoredUser();
    return rejectWithValue(normalizeError(err));
  }
});

export const loginUser = createAsyncThunk('auth/login', async (payload, { rejectWithValue }) => {
  try {
    await Api.login(payload);
    const user = await Api.currentUser();
    persistUser(user);
    return user;
  } catch (err) {
    return rejectWithValue(normalizeError(err));
  }
});

export const logoutUser = createAsyncThunk('auth/logout', async (_, { rejectWithValue }) => {
  try {
    await Api.logout();
  } catch (err) {
    return rejectWithValue(normalizeError(err));
  } finally {
    clearStoredUser();
  }
  return null;
});

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
  extraReducers: builder => {
    builder
      .addCase(fetchCurrentUser.pending, state => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(fetchCurrentUser.fulfilled, (state, action) => {
        state.status = 'succeeded';
        state.user = action.payload || null;
        state.error = null;
      })
      .addCase(fetchCurrentUser.rejected, (state, action) => {
        state.status = 'failed';
        state.user = null;
        state.error = action.payload?.message || action.error?.message || 'Request failed';
      })
      .addCase(loginUser.pending, state => {
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
      .addCase(logoutUser.fulfilled, state => {
        state.status = 'idle';
        state.user = null;
        state.error = null;
      })
      .addCase(logoutUser.rejected, state => {
        state.status = 'idle';
        state.user = null;
      });
  },
});

export const { clearUser } = authSlice.actions;

export default authSlice.reducer;
