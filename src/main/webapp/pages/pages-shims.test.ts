import Index from './index';
import Login from './login';
import Register from './register';
import Share from './share';
import SharedLinks from './shared-links';
import ChangePassword from './change-password';
import Forbidden from './403';
import NotFound from './404';

describe('pages shims', () => {
  test('re-export Redirect as default', () => {
    for (const page of [Index, Login, Register, Share, SharedLinks, ChangePassword, Forbidden, NotFound]) {
      expect(typeof page).toBe('function');
    }
  });
});
