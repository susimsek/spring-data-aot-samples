import { library } from '@fortawesome/fontawesome-svg-core';

jest.mock('@fortawesome/fontawesome-svg-core', () => ({
  library: {
    add: jest.fn(),
  },
}));

import { loadIcons } from './iconLoader';

describe('iconLoader', () => {
  test('loadIcons registers icons in the FontAwesome library', () => {
    loadIcons();
    expect(library.add).toHaveBeenCalled();
  });
});
