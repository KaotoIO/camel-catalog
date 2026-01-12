#!/usr/bin/env node
// @ts-check

/**
 * Cross-platform script to copy catalog files.
 * Replaces `copyfiles` which fails on Windows with ENAMETOOLONG error
 * when there are many files to copy.
 */

import { cp, mkdir, access } from 'node:fs/promises';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const projectRoot = resolve(__dirname, '..');

const src = resolve(projectRoot, 'dist/camel-catalog');
const dest = resolve(projectRoot, 'catalog');

async function copyFiles() {
  try {
    await access(src);
  } catch {
    console.error(`Source directory does not exist: ${src}`);
    // eslint-disable-next-line no-undef
    process.exit(1);
  }

  // Ensure destination directory exists
  await mkdir(dest, { recursive: true });

  console.log(`Copying catalog files from ${src} to ${dest}...`);

  // Use Node.js built-in recursive copy (available since Node 16.7.0)
  await cp(src, dest, { recursive: true });

  console.log('Catalog files copied successfully.');
}

copyFiles();
