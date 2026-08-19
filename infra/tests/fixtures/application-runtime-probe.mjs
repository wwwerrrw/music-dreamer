import { writeFileSync } from 'node:fs';
import { createServer } from 'node:http';

const port = Number.parseInt(process.env.MUSIC_DREAMER_TEST_PORT ?? '', 10);
const outputPath = process.env.MUSIC_DREAMER_TEST_OUTPUT;
const marker = process.env.MUSIC_DREAMER_TEST_MARKER;

if (!Number.isInteger(port) || port < 1 || port > 65535) {
  throw new Error('MUSIC_DREAMER_TEST_PORT must be a valid TCP port.');
}
if (!outputPath || !marker) {
  throw new Error('Probe output and marker environment variables are required.');
}

writeFileSync(
  outputPath,
  JSON.stringify({ cwd: process.cwd(), marker }, null, 2),
  { encoding: 'utf8' },
);

const server = createServer((request, response) => {
  const acceptsHtml = (request.headers.accept ?? '').includes('text/html');
  if (request.url !== '/ready' || !acceptsHtml) {
    response.writeHead(404, { 'content-type': 'text/plain; charset=utf-8' });
    response.end('NOT READY');
    return;
  }

  response.writeHead(200, { 'content-type': 'text/plain; charset=utf-8' });
  response.end('READY');
});

server.listen(port, '127.0.0.1', () => {
  process.stdout.write(`READY ${port}\n`);
});
