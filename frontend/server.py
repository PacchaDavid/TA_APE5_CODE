import argparse
import os
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path


class FrontendRequestHandler(SimpleHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/':
            self.path = '/index.html'
        return super().do_GET()

    def end_headers(self):
        self.send_header('Cache-Control', 'no-store')
        super().end_headers()


def main() -> None:
    parser = argparse.ArgumentParser(description='Serve the APE 5 frontend.')
    parser.add_argument('--port', type=int, default=8000, help='Port to bind the frontend server to.')
    args = parser.parse_args()

    frontend_root = Path(__file__).resolve().parent
    os.chdir(frontend_root)
    server = ThreadingHTTPServer(('127.0.0.1', args.port), FrontendRequestHandler)
    print(f'Frontend server running at http://127.0.0.1:{args.port}')
    print(f'Serving files from {frontend_root}')
    server.serve_forever()


if __name__ == '__main__':
    main()