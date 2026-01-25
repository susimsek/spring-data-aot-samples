export function replaceLocation(url: string): void {
  const location = (globalThis as any).location as Location | undefined;
  if (!location) return;
  location.replace(url);
}

export function reloadPage(): void {
  const location = (globalThis as any).location as Location | undefined;
  if (!location) return;
  location.reload();
}
