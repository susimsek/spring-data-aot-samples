import reducer, { setTheme, toggleTheme } from './themeSlice';

describe('themeSlice', () => {
  test('setTheme sets the theme', () => {
    expect(reducer({ theme: 'light' }, setTheme('dark'))).toEqual({ theme: 'dark' });
  });

  test('toggleTheme toggles between light and dark', () => {
    expect(reducer({ theme: 'light' }, toggleTheme())).toEqual({ theme: 'dark' });
    expect(reducer({ theme: 'dark' }, toggleTheme())).toEqual({ theme: 'light' });
  });
});
