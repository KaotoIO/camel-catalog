/**
 * Catalog and Kamelet versions consumed by the generator.
 *
 * Quarkus entries are Quarkus *platform* versions (what `camel run
 * --quarkus-version` takes); every other list holds the runtime's own version.
 * Provenance (community vs. Red Hat) is inferred from the `redhat-` marker.
 *
 * How versions are sourced, the Quarkus platform -> camel-quarkus mapping, and
 * the registry URLs for verifying them: see ./docs/CATALOG_VERSIONS.md
 */

const CATALOGS = {
  Main: [
    //
    '4.20.0',
    '4.18.2',
    '4.14.7',
    '4.10.9',
    '4.18.1.redhat-00019',
    '4.14.4.redhat-00008',
    '4.10.7.redhat-00009',
  ],
  Quarkus: [
    '3.35.4',
    '3.33.2',
    '3.27.4',
    '3.20.6.1',
    '3.33.1.redhat-00006',
    '3.27.3.redhat-00003',
    '3.20.6.redhat-00004',
  ],
  SpringBoot: [
    '4.20.0',
    '4.18.2',
    '4.14.7',
    '4.10.9',
    '4.18.1.redhat-00014',
    '4.14.4.redhat-00010',
    '4.10.7.redhat-00013',
  ],
  Citrus: ['4.10.0', '4.10.1'],
};

const KAMELETS_VERSION = '4.20.0';

module.exports = { CATALOGS, KAMELETS_VERSION };
