type BrowserGlobals = typeof globalThis & {
  location?: Location;
  document?: Document;
  matchMedia?: (query: string) => MediaQueryList;
};

function getBrowserGlobals(): BrowserGlobals {
  return globalThis as BrowserGlobals;
}

export function getLocation(): Location | undefined {
  return getBrowserGlobals().location;
}

export function getDocument(): Document | undefined {
  return getBrowserGlobals().document;
}

export function getMatchMedia(): ((query: string) => MediaQueryList) | undefined {
  return getBrowserGlobals().matchMedia;
}

export function replaceLocation(url: string): void {
  const location = getLocation();
  if (!location) return;
  location.replace(url);
}

export function reloadPage(): void {
  const location = getLocation();
  if (!location) return;
  location.reload();
}
