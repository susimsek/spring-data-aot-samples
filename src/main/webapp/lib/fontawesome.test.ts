jest.mock('@fortawesome/fontawesome-svg-core/styles.css', () => ({}));

const config = { autoAddCss: true };

jest.mock('@fortawesome/fontawesome-svg-core', () => ({
  config,
}));

const loadIcons = jest.fn();

jest.mock('./iconLoader', () => ({
  loadIcons,
}));

describe('fontawesome', () => {
  test('disables autoAddCss and loads icons', async () => {
    await import('./fontawesome');
    expect(config.autoAddCss).toBe(false);
    expect(loadIcons).toHaveBeenCalled();
  });
});
