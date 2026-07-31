package io.kaoto.camelcatalog.generator.camel;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

public interface CatalogEntryHandler {
    Map<String, ObjectNode> generate();
}
