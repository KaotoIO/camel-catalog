/*
 * Copyright (C) 2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.kaoto.camelcatalog.generator.camel.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.kaoto.camelcatalog.generator.camel.CamelCatalogSchemaEnhancer;
import io.kaoto.camelcatalog.generator.camel.CamelYamlDslSchemaProcessor;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.dsl.yaml.YamlRoutesBuilderLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoadBalancerHandlerTest {

    LoadBalancerHandler loadBalancerHandler;

    @BeforeEach
    void setUp() throws Exception {
        CamelCatalog camelCatalog = new DefaultCamelCatalog();
        ObjectMapper jsonMapper = new ObjectMapper();
        var is = YamlRoutesBuilderLoader.class.getClassLoader().getResourceAsStream("schema/camelYamlDsl.json");
        ObjectNode yamlDslSchema = (ObjectNode) jsonMapper.readTree(is);
        CamelYamlDslSchemaProcessor schemaProcessor = new CamelYamlDslSchemaProcessor(jsonMapper, yamlDslSchema);
        CamelCatalogSchemaEnhancer schemaEnhancer = new CamelCatalogSchemaEnhancer(camelCatalog);
        loadBalancerHandler = new LoadBalancerHandler(camelCatalog, schemaProcessor, schemaEnhancer);
    }

    @Test
    void shouldContainLoadBalancers() {
        var loadBalancerMap = loadBalancerHandler.generate();

        assertTrue(loadBalancerMap.containsKey("roundRobinLoadBalancer"));
        assertTrue(loadBalancerMap.containsKey("randomLoadBalancer"));
        assertTrue(loadBalancerMap.containsKey("failoverLoadBalancer"));
    }

    @Test
    void shouldHavePropertiesSchema() {
        var loadBalancerMap = loadBalancerHandler.generate();

        var failoverNode = loadBalancerMap.get("failoverLoadBalancer");
        assertTrue(failoverNode.has("propertiesSchema"));
        assertTrue(failoverNode.get("propertiesSchema").has("properties"));
    }

    @Test
    void shouldSortPropertiesAccordingToCatalogIndex() {
        var loadBalancerMap = loadBalancerHandler.generate();

        var failoverNode = loadBalancerMap.get("failoverLoadBalancer");
        var propertiesNode = failoverNode.get("propertiesSchema").get("properties");
        List<String> keys = new ArrayList<>();
        propertiesNode.fieldNames().forEachRemaining(keys::add);

        assertEquals(List.of("id", "exception", "roundRobin", "sticky", "maximumFailoverAttempts"), keys);
    }
}
