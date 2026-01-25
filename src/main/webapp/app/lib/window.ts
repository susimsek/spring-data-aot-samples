export function replaceLocation(url: string): void {
  if (typeof window === 'undefined') return;
  window.location.replace(url);
}

export function reloadPage(): void {
  if (typeof window === 'undefined') return;
  window.location.reload();
}
