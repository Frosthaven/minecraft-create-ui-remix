"""Serves site/ for the preview pages on 127.0.0.1:18777 with caching off, so a reload always runs the current scripts.

    python3 -I tools/serve.py [port]
"""
import functools, http.server, os, sys

class NoCache(http.server.SimpleHTTPRequestHandler):
    def end_headers(self):
        self.send_header('Cache-Control', 'no-store')
        super().end_headers()

port = int(sys.argv[1]) if len(sys.argv) > 1 else 18777
site = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'site')
http.server.ThreadingHTTPServer(('127.0.0.1', port), functools.partial(NoCache, directory=site)).serve_forever()
