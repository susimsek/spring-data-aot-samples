import '@testing-library/jest-dom';
import '@testing-library/react';
import * as React from 'react';

// Next.js Image component relies on optimization APIs that are not available in jsdom.
jest.mock('next/image', () => ({
  __esModule: true,
  default: function NextImage(props: any) {
    // eslint-disable-next-line @next/next/no-img-element
    return React.createElement('img', props);
  },
}));
