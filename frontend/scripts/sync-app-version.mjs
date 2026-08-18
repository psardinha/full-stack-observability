import {readFileSync, writeFileSync} from 'node:fs';
import {dirname, resolve} from 'node:path';
import {fileURLToPath} from 'node:url';

const currentDir = dirname(fileURLToPath(import.meta.url));
const projectRoot = resolve(currentDir, '..');
const packageJsonPath = resolve(projectRoot, 'package.json');
const outputPath = resolve(projectRoot, 'src/environments/version.ts');

const packageJson = JSON.parse(readFileSync(packageJsonPath, 'utf8'));
const appVersion = packageJson.version;

if (typeof appVersion !== 'string' || appVersion.trim().length === 0) {
  throw new Error('package.json version is missing or invalid.');
}

const output = `export const APP_VERSION = '${appVersion}';\n`;
writeFileSync(outputPath, output, 'utf8');
