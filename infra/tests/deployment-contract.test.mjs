import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';

const root = path.resolve(import.meta.dirname, '../..');
const deploy = path.join(root, 'deploy');

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), 'utf8');
}

function expectFile(relativePath) {
  assert.equal(fs.existsSync(path.join(root, relativePath)), true, `missing ${relativePath}`);
}

test('deployment bundle contains required files', () => {
  for (const relativePath of [
    '.dockerignore',
    'deploy/backend.Dockerfile',
    'deploy/frontend.Dockerfile',
    'deploy/compose.yaml',
    'deploy/.env.example',
    'deploy/mysql/init/01-init.sh',
    'deploy/nginx/default.conf.template',
    'deploy/README.md'
  ]) {
    expectFile(relativePath);
  }
});

test('compose defines the complete private service topology', () => {
  const compose = read('deploy/compose.yaml');
  for (const service of [
    'mysql:',
    'redis:',
    'nacos:',
    'gateway:',
    'identity:',
    'catalog:',
    'interaction:',
    'media:',
    'nginx:'
  ]) {
    assert.match(compose, new RegExp(`^  ${service.replace(':', '\\:')}$`, 'm'));
  }
  for (const volume of ['mysql-data:', 'redis-data:', 'nacos-data:', 'media-data:']) {
    assert.match(compose, new RegExp(`^  ${volume.replace(':', '\\:')}$`, 'm'));
  }
});

test('only the edge nginx service publishes a host port', () => {
  const compose = read('deploy/compose.yaml');
  const publishedPorts = [...compose.matchAll(/^\s+- "?(\d+):\d+"?$/gm)].map((match) => match[1]);
  assert.deepEqual(publishedPorts, ['80']);
  for (const internalPort of ['3306', '6379', '8848', '8080', '8081', '8082', '8083', '8084']) {
    assert.doesNotMatch(compose, new RegExp(`^\\s+- "?${internalPort}:`, 'm'));
  }
});

test('compose uses secret variables and persistent mounts', () => {
  const compose = read('deploy/compose.yaml');
  for (const variable of [
    'MYSQL_ROOT_PASSWORD',
    'MUSIC_DREAMER_IDENTITY_DB_PASSWORD',
    'MUSIC_DREAMER_CATALOG_DB_PASSWORD',
    'MUSIC_DREAMER_INTERACTION_DB_PASSWORD',
    'MUSIC_DREAMER_MEDIA_DB_PASSWORD'
  ]) {
    assert.match(compose, new RegExp(`\\$\\{${variable}`));
  }
  for (const mount of ['mysql-data:/var/lib/mysql', 'redis-data:/data', 'nacos-data:', 'media-data:']) {
    assert.match(compose, new RegExp(mount.replaceAll('/', '\\/')));
  }
  assert.doesNotMatch(compose, /^\s+[A-Z0-9_]*PASSWORD:\s+(?!\$\{)[^\n]+/m);
});

test('nginx routes API traffic and both history-mode SPAs', () => {
  const nginx = read('deploy/nginx/default.conf.template');
  assert.match(nginx, /location\s+\/api\//);
  assert.match(nginx, /proxy_pass\s+http:\/\/gateway:8080/);
  assert.match(nginx, /try_files\s+\$uri\s+\$uri\/\s+\/index\.html/);
  assert.match(nginx, /\$\{ADMIN_HOST\}/);
  assert.match(nginx, /client_max_body_size\s+50m/);
  assert.match(read('deploy/compose.yaml'), /NGINX_ENVSUBST_FILTER/);
});

test('mysql bootstrap creates four schema-owned application accounts', () => {
  const init = read('deploy/mysql/init/01-init.sh');
  for (const schema of ['music_dreamer_identity', 'music_dreamer_catalog', 'music_dreamer_interaction', 'music_dreamer_media']) {
    assert.match(init, new RegExp(schema));
  }
  for (const variable of [
    'MUSIC_DREAMER_IDENTITY_DB_PASSWORD',
    'MUSIC_DREAMER_CATALOG_DB_PASSWORD',
    'MUSIC_DREAMER_INTERACTION_DB_PASSWORD',
    'MUSIC_DREAMER_MEDIA_DB_PASSWORD'
  ]) {
    assert.match(init, new RegExp(variable));
  }
});

test('deployment environment example is safe and ignored', () => {
  const envExample = read('deploy/.env.example');
  assert.match(envExample, /MUSIC_DREAMER_ADMIN_HOST/);
  assert.match(envExample, /MUSIC_DREAMER_IDENTITY_MEDIA_COOKIE_SECURE/);
  assert.doesNotMatch(envExample, /AKIA|ghp_|-----BEGIN|password123/i);
  assert.match(read('.gitignore'), /(^|\n)\/deploy\/\.env(\n|$)/);
});
