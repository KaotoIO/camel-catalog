import { cp, mkdir } from 'node:fs/promises';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

// __dirname equivalent in ES modules
const __dirname = dirname(fileURLToPath(import.meta.url));

// The root of the camel-catalog project
const projectRoot = resolve(__dirname, '..');

// Where Maven unpacked the files to (matches <outputDirectory> in pom.xml)
const src = resolve(projectRoot, 'target/forage-catalog/catalog');

// Where we want them in the committed catalog/ tree
const dest = resolve(projectRoot, 'catalog/forage');

console.log(`Copying Forage catalog files...`);
console.log(`  From: ${src}`);
console.log(`  To:   ${dest}`);

await mkdir(dest, { recursive: true });
await cp(src, dest, { recursive: true });

console.log('Done. Forage catalog files copied successfully.');