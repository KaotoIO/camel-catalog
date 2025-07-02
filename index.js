const CATALOGS = {
  // https://repo1.maven.org/maven2/org/apache/camel/camel-catalog/
  // https://maven.repository.redhat.com/ga/org/apache/camel/camel-catalog/
  Main: ['4.12.0', '4.10.4', '4.8.6', '4.10.3.redhat-00020', '4.8.5.redhat-00008', '4.4.0.redhat-00046'],
  // https://repo1.maven.org/maven2/org/apache/camel/quarkus/camel-quarkus-catalog/
  // https://maven.repository.redhat.com/ga/org/apache/camel/quarkus/camel-quarkus-catalog/
  Quarkus: ['3.24.0', '3.20.2', '3.15.3', '3.20.0.redhat-00002', '3.15.0.redhat-00010', '3.8.0.redhat-00018'],
  // https://repo1.maven.org/maven2/org/apache/camel/springboot/camel-catalog-provider-springboot/
  // https://maven.repository.redhat.com/ga/org/apache/camel/springboot/camel-catalog-provider-springboot/
  SpringBoot: ['4.12.0', '4.10.4', '4.8.6', '4.10.3.redhat-00019', '4.8.5.redhat-00008', '4.4.0.redhat-00039'],
};

const KAMELETS_VERSION = '4.12.0';

module.exports = { CATALOGS, KAMELETS_VERSION };
