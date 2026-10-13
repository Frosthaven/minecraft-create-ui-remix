// Dev hot reload: polls Last-Modified (python http.server sends it) of this page, its local scripts and styles,
// and reloads when one changes. Active only on a local server.
if (['127.0.0.1', 'localhost'].includes(location.hostname)) {
  const urls = [location.pathname, ...[...document.scripts].map(s => s.src),
    ...[...document.querySelectorAll('link[rel=stylesheet]')].map(l => l.href)].filter(u => u.startsWith('/') || u.startsWith(location.origin));
  const last = {};
  setInterval(async () => {
    for (const u of urls) {
      try {
        const r = await fetch(u, { method: 'HEAD', cache: 'no-store' });
        const m = r.headers.get('last-modified');
        if (last[u] && m && m !== last[u]) return location.reload();
        last[u] = m ?? last[u];
      } catch { /* server restarting: try again next tick */ }
    }
  }, 1000);
}
