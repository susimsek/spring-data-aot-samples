import Index from '../pages/index';
import Login from '../pages/[locale]/login';
import Register from '../pages/[locale]/register';
import Share from '../pages/[locale]/share';
import SharedLinks from '../pages/[locale]/shared-links';
import ChangePassword from '../pages/[locale]/change-password';
import Forbidden from '../pages/[locale]/403';
import NotFound from '../pages/[locale]/404';

describe('pages shims', () => {
  test('re-export Redirect as default', () => {
    for (const page of [Index, Login, Register, Share, SharedLinks, ChangePassword, Forbidden, NotFound]) {
      expect(typeof page).toBe('function');
    }
  });
});
