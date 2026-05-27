/**
 * @fileoverview
 * This module exports the versions of Camel catalogs and Kamelets used in the project.
 * It includes versions for Main, Quarkus, and SpringBoot catalogs.
 *
 * Camel JBang commands to list available catalogs:
 * - Main: `camel version list --fresh --runtime=main`
 * - Quarkus: `camel version list --fresh --runtime=quarkus`
 * - Spring Boot: `camel version list --fresh --runtime=spring-boot`
 *
 * For Red Hat versions, add `--repo=redhat` to the commands.
 * - Main: `camel version list --fresh --runtime=main --repo=redhat`
 * - Quarkus: `camel version list --fresh --runtime=quarkus --repo=redhat`
 * - Spring Boot: `camel version list --fresh --runtime=spring-boot --repo=redhat`
 */

const CATALOGS = {
  // https://repo1.maven.org/maven2/org/apache/camel/camel-catalog/
  // https://maven.repository.redhat.com/ga/org/apache/camel/camel-catalog/
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
  // https://repo1.maven.org/maven2/org/apache/camel/quarkus/camel-quarkus-catalog/
  // https://maven.repository.redhat.com/ga/org/apache/camel/quarkus/camel-quarkus-catalog/
  Quarkus: [
    //
    '3.35.0',
    '3.33.1',
    '3.27.4',
    '3.20.4',
    '3.27.1.redhat-00007',
    '3.20.0.redhat-00011',
    '3.15.0.redhat-00010',
  ],
  // https://repo1.maven.org/maven2/org/apache/camel/springboot/camel-catalog-provider-springboot/
  // https://maven.repository.redhat.com/ga/org/apache/camel/springboot/camel-catalog-provider-springboot/
  SpringBoot: [
    //
    '4.20.0',
    '4.18.2',
    '4.14.7',
    '4.10.9',
    '4.18.1.redhat-00014',
    '4.14.4.redhat-00010',
    '4.10.7.redhat-00013',
  ],
  // https://repo1.maven.org/maven2/org/citrusframework/citrus-catalog-schema/
  Citrus: [
    //
    '4.10.0',
    '4.10.1',
  ],
};

const KAMELETS_VERSION = '4.20.0';

module.exports = { CATALOGS, KAMELETS_VERSION };
