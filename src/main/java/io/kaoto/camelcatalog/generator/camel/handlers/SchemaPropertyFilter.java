package io.kaoto.camelcatalog.generator.camel.handlers;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class SchemaPropertyFilter {
    private static final String STEPS = "steps";
    private static final String ONE_OF = "oneOf";
    private final Map<String, List<String>> processorPropertyBlockList;
    /** 
     * Deleted properties from the schema so that in the construction
     * of the forms they don't influence the creation of container
     * i.e. adding an extra component or extra container named steps
     * as steps include inside the components and EIPs
     */
    public SchemaPropertyFilter() {
        this.processorPropertyBlockList = Map.ofEntries(
            Map.entry("choice", List.of("when", "otherwise")),
            Map.entry("doTry", List.of("doCatch", "doFinally",STEPS)),
            Map.entry("when", List.of(STEPS)),
            Map.entry("otherwise", List.of(STEPS)),
            Map.entry("doCatch", List.of(STEPS)),
            Map.entry("doFinally", List.of(STEPS)),
            Map.entry("aggregate", List.of(STEPS)),
            Map.entry("circuitBreaker", List.of(STEPS)),
            Map.entry("filter", List.of(STEPS)),
            Map.entry("loadBalance", List.of(STEPS)),
            Map.entry("loop", List.of(STEPS)),
            Map.entry("multicast", List.of(STEPS)),
            Map.entry("onFallback", List.of(STEPS)),
            Map.entry("pipeline", List.of(STEPS)),
            Map.entry("resequence", List.of(STEPS)),
            Map.entry("saga", List.of(STEPS)),
            Map.entry("split", List.of(STEPS)),
            Map.entry("step", List.of(STEPS)),
            Map.entry("whenSkipSendToEndpoint", List.of(STEPS)),
            Map.entry("from", List.of(STEPS)),
            Map.entry("intercept", List.of(STEPS)),
            Map.entry("interceptFrom", List.of(STEPS)),
            Map.entry("interceptSendToEndpoint", List.of(STEPS)),
            Map.entry("onCompletion", List.of(STEPS)),
            Map.entry("onException", List.of(STEPS)),
            Map.entry("rest", List.of("get", "post", "put", "delete", "head", "patch"))
        );
      }

    void schemaPropertyFilter(String eipName, ObjectNode node) {
        if (!processorPropertyBlockList.containsKey(eipName)) return;

        filterProperties(eipName, node);

        if (node.has(ONE_OF)) {
            var array = (ArrayNode) node.get(ONE_OF);
            array.forEach(element -> {
                filterProperties(eipName, (ObjectNode) element);
            });
        }

        if (node.has("anyOf")) {
            var array = (ArrayNode) node.get("anyOf");
            array.forEach(element -> {
                filterProperties(eipName, (ObjectNode) element);
            });
        }
    }

    void filterProperties(String eipName, ObjectNode node) {
        if (node.has("properties")) {
            var properties = (ObjectNode) node.get("properties");
            Set<String> propToRemove = new HashSet<>();
            properties.properties().forEach(entry -> {
                if (processorPropertyBlockList.get(eipName).contains(entry.getKey())) {
                    propToRemove.add(entry.getKey());
                }
            });
            propToRemove.forEach(properties::remove);
        }
    }

}
