/*
 * Copyright (C) 2026 Red Hat, Inc.
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
package io.kaoto.camelcatalog.generator.xslt;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class XPathFunctionsGeneratorTest {
    XPathFunctionsGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new XPathFunctionsGenerator(getClass().getClassLoader());
    }

    @Test
    void shouldProduceAllExpectedGroups() {
        var result = generator.generate();
        for (String group : XPathFunctionsGenerator.ALL_GROUPS) {
            assertTrue(result.containsKey(group), "Missing group: " + group);
        }
    }

    @Test
    void shouldContainFunctionsInEachXPathGroup() {
        var result = generator.generate();
        for (String group : XPathFunctionsGenerator.ALL_GROUPS) {
            if ("XSLT".equals(group)) continue;
            ObjectNode groupNode = result.get(group);
            assertNotNull(groupNode, "Group node null: " + group);
            assertFalse(groupNode.isEmpty(), "Group should have functions: " + group);
        }
    }

    @Test
    void shouldContainSpecificNumericFunctions() {
        var result = generator.generate();
        ObjectNode numericNode = result.get("Numeric");
        assertTrue(numericNode.has("abs"), "Numeric group should contain abs");
        assertTrue(numericNode.has("ceiling"), "Numeric group should contain ceiling");
        assertTrue(numericNode.has("floor"), "Numeric group should contain floor");
        assertTrue(numericNode.has("round"), "Numeric group should contain round");
    }

    @Test
    void shouldContainConcatInStringGroup() {
        var result = generator.generate();
        ObjectNode stringNode = result.get("String");
        assertTrue(stringNode.has("concat"), "String group should contain concat");

        var concatNode = stringNode.get("concat");
        assertEquals("Concatenate", concatNode.get("displayName").asText());
        assertEquals("fn", concatNode.get("prefix").asText());
        assertEquals("xs:string", concatNode.get("returnType").asText());
    }

    @Test
    void shouldSkipOpPrefixFunctions() {
        var result = generator.generate();
        for (Map.Entry<String, ObjectNode> entry : result.entrySet()) {
            var groupNode = entry.getValue();
            var fields = groupNode.fields();
            while (fields.hasNext()) {
                var field = fields.next();
                assertFalse(field.getKey().startsWith("op:"),
                        "Should not contain op: functions, found " + field.getKey()
                                + " in group " + entry.getKey());
            }
        }
    }

    @Test
    void shouldParseFunctionArgumentsCorrectly() {
        var result = generator.generate();
        ObjectNode stringNode = result.get("String");
        var substringNode = stringNode.get("substring");
        assertNotNull(substringNode, "String group should contain substring");

        var argsNode = substringNode.get("arguments");
        assertNotNull(argsNode);
        assertTrue(argsNode.isArray());
        assertTrue(argsNode.size() >= 2, "substring should have at least 2 arguments");

        var firstArg = argsNode.get(0);
        assertNotNull(firstArg.get("name"));
        assertNotNull(firstArg.get("type"));
        assertNotNull(firstArg.get("displayName"));
    }

    @Test
    void shouldParseSignaturesWithProperties() {
        var result = generator.generate();
        ObjectNode numericNode = result.get("Numeric");
        var absNode = numericNode.get("abs");
        assertNotNull(absNode);

        var signaturesNode = absNode.get("signatures");
        assertNotNull(signaturesNode);
        assertTrue(signaturesNode.isArray());
        assertTrue(signaturesNode.size() > 0, "abs should have at least one signature");

        var firstSig = signaturesNode.get(0);
        assertNotNull(firstSig.get("returnType"));
        assertNotNull(firstSig.get("properties"));
        assertTrue(firstSig.get("properties").isArray());
        assertTrue(firstSig.get("properties").size() > 0, "abs signature should have properties");
    }

    @Test
    void shouldContainXsltFunctions() {
        var result = generator.generate();
        ObjectNode xsltNode = result.get("XSLT");
        assertNotNull(xsltNode);
        assertFalse(xsltNode.isEmpty(), "XSLT group should have functions");
        assertTrue(xsltNode.has("current"), "XSLT group should contain current");
        assertTrue(xsltNode.has("document"), "XSLT group should contain document");
    }

    @Test
    void shouldUsePrefixedNamesForNonFnFunctions() {
        var result = generator.generate();
        ObjectNode mathNode = result.get("Math");
        assertFalse(mathNode.isEmpty(), "Math group should have functions");

        var fields = mathNode.fields();
        while (fields.hasNext()) {
            var field = fields.next();
            assertTrue(field.getKey().startsWith("math:"),
                    "Math functions should have math: prefix, found: " + field.getKey());
        }
    }

    @Test
    void shouldParseTypeStringCorrectly() {
        assertEquals("xs:string", XPathFunctionsGenerator.parseTypeString("xs:string").baseType());
        assertEquals("", XPathFunctionsGenerator.parseTypeString("xs:string").cardinality());

        assertEquals("xs:string", XPathFunctionsGenerator.parseTypeString("xs:string?").baseType());
        assertEquals("?", XPathFunctionsGenerator.parseTypeString("xs:string?").cardinality());

        assertEquals("item()", XPathFunctionsGenerator.parseTypeString("item()*").baseType());
        assertEquals("*", XPathFunctionsGenerator.parseTypeString("item()*").cardinality());

        assertEquals("xs:integer", XPathFunctionsGenerator.parseTypeString("xs:integer+").baseType());
        assertEquals("+", XPathFunctionsGenerator.parseTypeString("xs:integer+").cardinality());

        assertEquals("function(*)", XPathFunctionsGenerator.parseTypeString("function(*)").baseType());
        assertEquals("", XPathFunctionsGenerator.parseTypeString("function(*)").cardinality());

        assertEquals("function(*)", XPathFunctionsGenerator.parseTypeString("function(*)?").baseType());
        assertEquals("?", XPathFunctionsGenerator.parseTypeString("function(*)?").cardinality());
    }

    @Test
    void shouldContainNamespacesMap() {
        var result = generator.generate();
        assertTrue(result.containsKey("namespaces"), "Result should contain namespaces key");

        var namespacesNode = result.get("namespaces");
        assertEquals("http://www.w3.org/2005/xpath-functions", namespacesNode.get("fn").asText());
        assertEquals("http://www.w3.org/2005/xpath-functions/math", namespacesNode.get("math").asText());
        assertEquals("http://www.w3.org/2005/xpath-functions/map", namespacesNode.get("map").asText());
        assertEquals("http://www.w3.org/2005/xpath-functions/array", namespacesNode.get("array").asText());
        assertEquals("http://www.w3.org/2001/XMLSchema", namespacesNode.get("xs").asText());
    }

    @Test
    void shouldContainMetadataWithFieldSemantics() {
        var result = generator.generate();
        assertTrue(result.containsKey("_metadata"), "Result should contain _metadata key");

        var metadata = result.get("_metadata");
        assertTrue(metadata.has("fieldSemantics"));
        var fieldSemantics = metadata.get("fieldSemantics");
        assertTrue(fieldSemantics.has("cardinality"));
        assertTrue(fieldSemantics.has("minOccurs"));
        assertTrue(fieldSemantics.has("maxOccurs"));
    }

    @Test
    void shouldSeparateTypeCardinalityFromPositionalOptionalityForXPathFunctions() {
        var result = generator.generate();

        var absNode = result.get("Numeric").get("abs");
        var absArg = absNode.get("arguments").get(0);
        assertEquals("?", absArg.get("cardinality").asText());
        assertEquals(1, absArg.get("minOccurs").asInt());
        assertEquals(1, absArg.get("maxOccurs").asInt());

        var substringNode = result.get("String").get("substring");
        var lengthArg = substringNode.get("arguments").get(2);
        assertEquals("length", lengthArg.get("name").asText());
        assertEquals("", lengthArg.get("cardinality").asText());
        assertEquals(0, lengthArg.get("minOccurs").asInt());
        assertEquals(1, lengthArg.get("maxOccurs").asInt());

        var sourceArg = substringNode.get("arguments").get(0);
        assertEquals("?", sourceArg.get("cardinality").asText());
        assertEquals(1, sourceArg.get("minOccurs").asInt());
        assertEquals(1, sourceArg.get("maxOccurs").asInt());
    }
}
