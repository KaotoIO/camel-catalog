#!/usr/bin/env ts-node
// @ts-check

import { spawn } from 'node:child_process';
import { createRequire } from 'node:module';
import { dirname } from 'node:path';
import { resolve } from 'path';
import { existsSync } from 'node:fs';

const require = createRequire(import.meta.url);
const { CATALOGS, KAMELETS_VERSION } = require('../index.js');

const generateCatalogs = () => {
  let camelCatalogPath = '';
  try {
    const camelCatalogIndexJsonPath = require.resolve('@kaoto/camel-catalog/catalog-index.d.ts');
    camelCatalogPath = dirname(camelCatalogIndexJsonPath);
  } catch (error) {
    throw new Error(`Could not find '@kaoto/camel-catalog' \n\n ${error}`);
  } finally {
    if (camelCatalogPath) console.log(`Found '@kaoto/camel-catalog' in ${camelCatalogPath}`, '\n');
  }

  const binary = resolve(camelCatalogPath, '../../target/catalog-generator-0.0.1-SNAPSHOT.jar');
  if (!existsSync(binary)) {
    throw new Error(`Could not find the catalog-generator JAR at ${binary}`);
  }

  const destinationFolder = resolve(camelCatalogPath, '../../dist/camel-catalog');
  const args = [
    '-jar',
    binary,
    '-o',
    destinationFolder,
    '-n',
    'Default Kaoto catalog',
    '-k',
    KAMELETS_VERSION,
    ...getVersionArguments(),
  ];

  spawn('java', args, {
    stdio: 'inherit',
  });
};

const getVersionArguments = () => {
  /** @type string[] */
  const starter = [];

  return Object.entries(CATALOGS).reduce((acc, [runtime, versions]) => {
    const flag = runtime.slice(0, 1).toLowerCase();

    versions.forEach((version) => {
      acc.push(`-${flag}`);
      acc.push(version);
    });

    return acc;
  }, starter);
};

generateCatalogs();
