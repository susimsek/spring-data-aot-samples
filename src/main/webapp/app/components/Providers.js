'use client';

import ThemeProvider from './ThemeProvider.js';
import ToastProvider from './ToastProvider.js';
import { Provider } from 'react-redux';
import store from '../store.js';

export default function Providers({ children }) {
  return (
    <Provider store={store}>
      <ThemeProvider>
        <ToastProvider>{children}</ToastProvider>
      </ThemeProvider>
    </Provider>
  );
}
