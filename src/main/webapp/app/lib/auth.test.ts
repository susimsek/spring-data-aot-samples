import { buildRedirectQuery, clearStoredUser, isAdmin, loadStoredUser, persistUser } from './auth';

describe('auth storage helpers', () => {
  beforeEach(() => {
    localStorage.clear();
    window.history.replaceState({}, '', '/');
  });

  test('persistUser stores and loadStoredUser reads', () => {
    const user = { username: 'alice', authorities: ['ROLE_USER'] };
    persistUser(user as any);
    expect(loadStoredUser()).toEqual(user);
  });

  test('persistUser(null) clears storage', () => {
    persistUser({ username: 'alice' } as any);
    expect(loadStoredUser()).toEqual({ username: 'alice' });
    persistUser(null);
    expect(loadStoredUser()).toBeNull();
  });

  test('clearStoredUser clears storage', () => {
    persistUser({ username: 'alice' } as any);
    clearStoredUser();
    expect(loadStoredUser()).toBeNull();
  });

  test('isAdmin checks ROLE_ADMIN', () => {
    expect(isAdmin(null)).toBe(false);
    expect(isAdmin({ username: 'alice', authorities: ['ROLE_USER'] } as any)).toBe(false);
    expect(isAdmin({ username: 'admin', authorities: ['ROLE_ADMIN'] } as any)).toBe(true);
  });

  test('buildRedirectQuery encodes the current location', () => {
    window.history.replaceState({}, '', '/notes?q=x#top');
    expect(buildRedirectQuery()).toBe(encodeURIComponent('/notes?q=x#top'));
  });

  test('loadStoredUser returns null when storage throws', () => {
    const getItemSpy = jest.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('boom');
    });

    expect(loadStoredUser()).toBeNull();
    getItemSpy.mockRestore();
  });

  test('persistUser ignores storage failures', () => {
    const setItemSpy = jest.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('boom');
    });

    expect(() => persistUser({ username: 'alice' } as any)).not.toThrow();
    setItemSpy.mockRestore();
  });
});
