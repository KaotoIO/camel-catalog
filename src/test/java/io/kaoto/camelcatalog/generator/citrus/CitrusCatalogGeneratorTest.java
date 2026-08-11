/*
 * Copyright (C) 2023 Red Hat, Inc.
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
package io.kaoto.camelcatalog.generator.citrus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.kaoto.camelcatalog.model.CatalogDefinition;
import io.kaoto.camelcatalog.model.CatalogRuntime;
import io.kaoto.camelcatalog.model.ResolvedVersions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CitrusCatalogGeneratorTest {

    @TempDir
    Path tempDir;

    private CitrusCatalogGenerator generator;
    private File outputDirectory;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        outputDirectory = tempDir.toFile();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testConstructorInitialization() {
        generator = new CitrusCatalogGenerator("4.10.0", outputDirectory, false);
        assertNotNull(generator);
    }

    @Test
    void testConstructorWithVerboseLogging() {
        generator = new CitrusCatalogGenerator("4.10.0", outputDirectory, true);
        assertNotNull(generator);
    }

    @Test
    void testGenerateReturnsValidCatalogDefinition() {
        generator = new CitrusCatalogGenerator("4.10.0", outputDirectory, false);

        CatalogDefinition catalogDefinition = generator.generate();

        assertNotNull(catalogDefinition);
        assertEquals("Citrus 4.10.0", catalogDefinition.getName());
        assertEquals(CatalogRuntime.Citrus, catalogDefinition.getRuntime());
        assertEquals("4.10.0", catalogDefinition.getVersion());
        assertEquals("index.json", catalogDefinition.getFileName());
    }

    @Test
    void testGenerateWithResolvedVersions() {
        generator = new CitrusCatalogGenerator("4.10.0", outputDirectory, false);

        ResolvedVersions resolvedVersions = new ResolvedVersions(
            "4.15.0",
            "4.15.0",
            "3.27.0",
            "4.15.0"
        );
        generator.setResolvedVersions(resolvedVersions);

        CatalogDefinition catalogDefinition = generator.generate();

        assertNotNull(catalogDefinition);
        assertEquals("4.15.0", catalogDefinition.getCamelCatalogVersion());
        assertEquals("4.15.0", catalogDefinition.getRuntimeProviderVersion());
        assertEquals("3.27.0", catalogDefinition.getFrameworkVersion());
    }

    @Test
    void testGenerateWithSnapshotVersion() {
        generator = new CitrusCatalogGenerator("4.10.0-SNAPSHOT", outputDirectory, false);

        CatalogDefinition catalogDefinition = generator.generate();

        assertNotNull(catalogDefinition);
        assertEquals("Citrus 4.10.0-SNAPSHOT", catalogDefinition.getName());
        assertEquals("4.10.0-SNAPSHOT", catalogDefinition.getVersion());
    }

    @Test
    void testRemoveActionsPropertyFromTestActions() throws Exception {
        String testActionsJson = """
            {
              "echo": {
                "propertiesSchema": {
                  "properties": {
                    "message": {
                      "type": "string"
                    },
                    "actions": {
                      "type": "array",
                      "items": {
                        "type": "object"
                      }
                    }
                  }
                }
              },
              "sleep": {
                "propertiesSchema": {
                  "properties": {
                    "milliseconds": {
                      "type": "number"
                    },
                    "actions": {
                      "type": "array"
                    }
                  }
                }
              }
            }
            """;

        generator = new CitrusCatalogGenerator("4.10.0", outputDirectory, false);

        // Use reflection to access private method
        var method = CitrusCatalogGenerator.class.getDeclaredMethod(
            "postProcessActionSchema", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(generator, testActionsJson);

        JsonNode resultNode = objectMapper.readTree(result);

        // Verify actions property is removed from echo
        assertFalse(resultNode.at("/echo/propertiesSchema/properties").has("actions"),
            "actions property should be removed from echo");
        assertTrue(resultNode.at("/echo/propertiesSchema/properties").has("message"),
            "message property should still exist");

        // Verify actions property is removed from sleep
        assertFalse(resultNode.at("/sleep/propertiesSchema/properties").has("actions"),
            "actions property should be removed from sleep");
        assertTrue(resultNode.at("/sleep/propertiesSchema/properties").has("milliseconds"),
            "milliseconds property should still exist");
    }

    @Test
    void testRemoveActionsPropertyFromTestContainers() throws Exception {
        String testContainersJson = """
            {
              "doFinally": {
                "propertiesSchema": {
                  "properties": {
                    "actions": {
                      "type": "array",
                      "items": {
                        "type": "object"
                      }
                    },
                    "description": {
                      "type": "string"
                    }
                  },
                  "required": ["actions"]
                }
              },
              "iterate": {
                "propertiesSchema": {
                  "properties": {
                    "actions": {
                      "type": "array"
                    },
                    "condition": {
                      "type": "string"
                    }
                  },
                  "required": ["actions", "condition"]
                }
              }
            }
            """;

        generator = new CitrusCatalogGenerator("4.10.0", outputDirectory, false);

        var method = CitrusCatalogGenerator.class.getDeclaredMethod(
            "postProcessActionSchema", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(generator, testContainersJson);

        JsonNode resultNode = objectMapper.readTree(result);

        // Verify actions property is removed from doFinally
        assertFalse(resultNode.at("/doFinally/propertiesSchema/properties").has("actions"),
            "actions property should be removed from doFinally");
        assertTrue(resultNode.at("/doFinally/propertiesSchema/properties").has("description"),
            "description property should still exist");

        // Verify actions is removed from doFinally required array (was the only entry)
        JsonNode doFinallyRequired = resultNode.at("/doFinally/propertiesSchema/required");
        assertTrue(doFinallyRequired.isArray());
        assertEquals(0, doFinallyRequired.size(), "doFinally required array should be empty after removing 'actions'");

        // Verify actions property is removed from iterate
        assertFalse(resultNode.at("/iterate/propertiesSchema/properties").has("actions"),
            "actions property should be removed from iterate");
        assertTrue(resultNode.at("/iterate/propertiesSchema/properties").has("condition"),
            "condition property should still exist");

        // Verify actions is removed from iterate required array but condition remains
        JsonNode iterateRequired = resultNode.at("/iterate/propertiesSchema/required");
        assertTrue(iterateRequired.isArray());
        assertEquals(1, iterateRequired.size(), "iterate required should only contain 'condition'");
        assertEquals("condition", iterateRequired.get(0).asText());
    }

    @Test
    void testRemoveActionsPropertyHandlesInvalidJson() throws Exception {
        String invalidJson = "not valid json";

        generator = new CitrusCatalogGenerator("4.10.0", outputDirectory, false);

        var method = CitrusCatalogGenerator.class.getDeclaredMethod(
            "postProcessActionSchema", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(generator, invalidJson);

        // Should return original content on error
        assertEquals(invalidJson, result);
    }

    @Test
    void testRemoveActionsPropertyHandlesNonObjectJson() throws Exception {
        String arrayJson = "[1, 2, 3]";

        generator = new CitrusCatalogGenerator("4.10.0", outputDirectory, false);

        var method = CitrusCatalogGenerator.class.getDeclaredMethod(
            "postProcessActionSchema", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(generator, arrayJson);

        // Should return original content for non-object JSON
        assertEquals(arrayJson, result);
    }

    @Test
    void testSetResolvedVersions() {
        generator = new CitrusCatalogGenerator("4.10.0", outputDirectory, false);

        ResolvedVersions resolvedVersions = new ResolvedVersions(
            "4.15.0",
            "4.15.0",
            "3.27.0",
            "4.15.0"
        );

        assertDoesNotThrow(() -> generator.setResolvedVersions(resolvedVersions));
    }

    @Test
    void testGenerateCreatesOutputDirectory() {
        // Note: The current implementation requires the output directory to exist
        // Files.writeString doesn't create parent directories automatically
        File nestedDir = new File(tempDir.toFile(), "nested/output");
        nestedDir.mkdirs();
        assertTrue(nestedDir.exists());

        generator = new CitrusCatalogGenerator("4.10.0", nestedDir, false);

        assertDoesNotThrow(() -> generator.generate());
    }

    @Test
    void testCatalogDefinitionFileName() {
        generator = new CitrusCatalogGenerator("4.10.0", outputDirectory, false);

        CatalogDefinition catalogDefinition = generator.generate();

        assertEquals("index.json", catalogDefinition.getFileName(),
            "Catalog definition should always use 'index.json' as filename");
    }

    @Test
    void testCatalogDefinitionNameFormat() {
        String[] versions = {"4.10.0", "4.10.1-SNAPSHOT", "4.11.0.redhat-00001"};

        for (String version : versions) {
            generator = new CitrusCatalogGenerator(version, outputDirectory, false);
            CatalogDefinition catalogDefinition = generator.generate();

            assertEquals("Citrus " + version, catalogDefinition.getName(),
                "Catalog name should be 'Citrus ' + version");
        }
    }

    @Test
    void testGenerateWithNullResolvedVersions() {
        generator = new CitrusCatalogGenerator("4.10.0", outputDirectory, false);
        // Don't set resolved versions

        CatalogDefinition catalogDefinition = generator.generate();

        assertNotNull(catalogDefinition);
        assertNull(catalogDefinition.getCamelCatalogVersion());
        assertNull(catalogDefinition.getRuntimeProviderVersion());
        assertNull(catalogDefinition.getFrameworkVersion());
    }

    @Test
    void testRemoveRequiredFromOneOf() throws Exception {
        String testActionsJson = """
            {
              "send" : {
                "propertiesSchema" : {
                  "$schema" : "http://json-schema.org/draft-07/schema#",
                  "type" : "object",
                  "properties" : {
                    "fork" : {
                      "type" : "boolean",
                      "title" : "Fork",
                      "description" : "When set the send operation does not block while waiting for the response."
                    },
                    "message" : {
                      "type" : "object",
                      "properties" : {
                        "body" : {
                          "type" : "object",
                          "properties" : {
                            "data" : {
                              "type" : "string",
                              "title" : "Data",
                              "description" : "The message body content as inline data."
                            },
                            "resource" : { }
                          },
                          "additionalProperties" : false,
                          "anyOf" : [ {
                            "oneOf" : [ {
                              "required" : [ "data" ]
                            }, {
                              "type" : "object",
                              "properties" : {
                                "resource" : {
                                  "type" : "object",
                                  "properties" : {
                                    "file" : {
                                      "type" : "string",
                                      "title" : "File"
                                    }
                                  },
                                  "additionalProperties" : false,
                                  "title" : "Resource",
                                  "description" : "The message body loaded from a file resource."
                                }
                              },
                              "required" : [ "resource" ]
                            }, {
                              "not" : {
                                "anyOf" : [ {
                                  "required" : [ "data" ]
                                }, {
                                  "required" : [ "resource" ]
                                }, {
                                  "required" : [ "script" ]
                                } ]
                              }
                            } ]
                          } ],
                          "title" : "Body",
                          "description" : "The message body."
                        }
                      },
                      "additionalProperties" : false,
                      "title" : "Message",
                      "description" : "The message to send."
                    }
                  },
                  "required" : [ "endpoint" ],
                  "additionalProperties" : false
                }
              }
            }
            """;

        generator = new CitrusCatalogGenerator("4.10.0", outputDirectory, false);

        // Use reflection to access private method
        var method = CitrusCatalogGenerator.class.getDeclaredMethod(
                "postProcessActionSchema", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(generator, testActionsJson);

        JsonNode expected = objectMapper.readTree("""
            {
              "send" : {
                "propertiesSchema" : {
                  "$schema" : "http://json-schema.org/draft-07/schema#",
                  "type" : "object",
                  "properties" : {
                    "fork" : {
                      "type" : "boolean",
                      "title" : "Fork",
                      "description" : "When set the send operation does not block while waiting for the response."
                    },
                    "message" : {
                      "type" : "object",
                      "properties" : {
                        "body" : {
                          "type" : "object",
                          "properties" : {
                            "data" : {
                              "type" : "string",
                              "title" : "Data",
                              "description" : "The message body content as inline data."
                            },
                            "resource" : { }
                          },
                          "additionalProperties" : false,
                          "anyOf" : [ {
                            "oneOf" : [ {
                              "type" : "object",
                              "properties" : {
                                "resource" : {
                                  "type" : "object",
                                  "properties" : {
                                    "file" : {
                                      "type" : "string",
                                      "title" : "File"
                                    }
                                  },
                                  "additionalProperties" : false,
                                  "title" : "Resource",
                                  "description" : "The message body loaded from a file resource."
                                }
                              }
                            }, {
                              "not" : {
                                "anyOf" : [ {
                                  "required" : [ "data" ]
                                }, {
                                  "required" : [ "resource" ]
                                }, {
                                  "required" : [ "script" ]
                                } ]
                              }
                            } ]
                          } ],
                          "title" : "Body",
                          "description" : "The message body."
                        }
                      },
                      "additionalProperties" : false,
                      "title" : "Message",
                      "description" : "The message to send."
                    }
                  },
                  "required" : [ "endpoint" ],
                  "additionalProperties" : false
                }
              }
            }""");
        assertEquals(expected, objectMapper.readTree(result));
    }

    @Test
    void testRemoveRequiredFromOneOfInArray() throws Exception {
        String testActionsJson = """
            {
              "send" : {
                "propertiesSchema" : {
                  "$schema" : "http://json-schema.org/draft-07/schema#",
                  "type" : "object",
                  "properties" : {
                    "fork" : {
                      "type" : "boolean",
                      "title" : "Fork",
                      "description" : "When set the send operation does not block while waiting for the response."
                    },
                    "headers" : {
                      "title" : "Headers",
                      "description" : "The message headers.",
                      "type" : "array",
                      "items" : {
                        "type" : "object",
                        "properties" : {
                          "data" : {
                            "type" : "string",
                            "title" : "Data",
                            "description" : "The message header value as inline data."
                          },
                          "name" : {
                            "type" : "string",
                            "title" : "Name",
                            "description" : "The message header name."
                          },
                          "resource" : { },
                          "type" : {
                            "type" : "string",
                            "title" : "Type",
                            "description" : "The message header type to create typed message headers.",
                            "$comment" : "group:advanced"
                          },
                          "value" : {
                            "type" : "string",
                            "title" : "Value",
                            "description" : "The message header value."
                          }
                        },
                        "additionalProperties" : false,
                        "anyOf" : [ {
                          "oneOf" : [ {
                            "required" : [ "value" ]
                          }, {
                            "type" : "object",
                            "properties" : {
                              "resource" : {
                                "type" : "object",
                                "properties" : {
                                  "charset" : {
                                    "type" : "string",
                                    "title" : "Charset",
                                    "description" : "Optional file resource charset used to read the file content.",
                                    "$comment" : "group:advanced"
                                  },
                                  "file" : {
                                    "type" : "string",
                                    "title" : "File",
                                    "description" : "The file resource path."
                                  }
                                },
                                "required" : [ "file" ],
                                "additionalProperties" : false,
                                "title" : "Resource",
                                "description" : "The header data loaded from a file resource."
                              }
                            },
                            "required" : [ "resource" ]
                          }, {
                            "required" : [ "data" ]
                          }, {
                            "not" : {
                              "anyOf" : [ {
                                "required" : [ "value" ]
                              }, {
                                "required" : [ "resource" ]
                              }, {
                                "required" : [ "data" ]
                              } ]
                            }
                          } ]
                        } ],
                        "title" : "Headers",
                        "description" : "The message headers."
                      }
                    }
                  },
                  "required" : [ "endpoint" ],
                  "additionalProperties" : false
                }
              }
            }
            """;

        generator = new CitrusCatalogGenerator("4.10.0", outputDirectory, false);

        // Use reflection to access private method
        var method = CitrusCatalogGenerator.class.getDeclaredMethod(
                "postProcessActionSchema", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(generator, testActionsJson);

        JsonNode expected = objectMapper.readTree("""
            {
              "send" : {
                "propertiesSchema" : {
                  "$schema" : "http://json-schema.org/draft-07/schema#",
                  "type" : "object",
                  "properties" : {
                    "fork" : {
                      "type" : "boolean",
                      "title" : "Fork",
                      "description" : "When set the send operation does not block while waiting for the response."
                    },
                    "headers" : {
                      "title" : "Headers",
                      "description" : "The message headers.",
                      "type" : "array",
                      "items" : {
                        "type" : "object",
                        "properties" : {
                          "data" : {
                            "type" : "string",
                            "title" : "Data",
                            "description" : "The message header value as inline data."
                          },
                          "name" : {
                            "type" : "string",
                            "title" : "Name",
                            "description" : "The message header name."
                          },
                          "resource" : { },
                          "type" : {
                            "type" : "string",
                            "title" : "Type",
                            "description" : "The message header type to create typed message headers.",
                            "$comment" : "group:advanced"
                          },
                          "value" : {
                            "type" : "string",
                            "title" : "Value",
                            "description" : "The message header value."
                          }
                        },
                        "additionalProperties" : false,
                        "anyOf" : [ {
                          "oneOf" : [ {
                            "type" : "object",
                            "properties" : {
                              "resource" : {
                                "type" : "object",
                                "properties" : {
                                  "charset" : {
                                    "type" : "string",
                                    "title" : "Charset",
                                    "description" : "Optional file resource charset used to read the file content.",
                                    "$comment" : "group:advanced"
                                  },
                                  "file" : {
                                    "type" : "string",
                                    "title" : "File",
                                    "description" : "The file resource path."
                                  }
                                },
                                "required" : [ "file" ],
                                "additionalProperties" : false,
                                "title" : "Resource",
                                "description" : "The header data loaded from a file resource."
                              }
                            }
                          }, {
                            "not" : {
                              "anyOf" : [ {
                                "required" : [ "value" ]
                              }, {
                                "required" : [ "resource" ]
                              }, {
                                "required" : [ "data" ]
                              } ]
                            }
                          } ]
                        } ],
                        "title" : "Headers",
                        "description" : "The message headers."
                      }
                    }
                  },
                  "required" : [ "endpoint" ],
                  "additionalProperties" : false
                }
              }
            }""");
        assertEquals(expected, objectMapper.readTree(result));
    }

    @Test
    void testRemoveEmptyOneOfAfterCleanup() throws Exception {
        String testActionsJson = """
            {
              "action" : {
                "propertiesSchema" : {
                  "type" : "object",
                  "properties" : {
                    "value" : {
                      "type" : "object",
                      "properties" : {
                        "a" : { "type" : "string" },
                        "b" : { "type" : "string" }
                      },
                      "oneOf" : [ {
                        "required" : [ "a" ]
                      }, {
                        "required" : [ "b" ]
                      } ]
                    }
                  }
                }
              }
            }
            """;

        generator = new CitrusCatalogGenerator("4.10.0", outputDirectory, false);

        var method = CitrusCatalogGenerator.class.getDeclaredMethod(
                "postProcessActionSchema", String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(generator, testActionsJson);

        JsonNode resultNode = objectMapper.readTree(result);
        JsonNode valueNode = resultNode.at("/action/propertiesSchema/properties/value");

        assertFalse(valueNode.has("oneOf"),
            "Empty oneOf should be removed after all required-only branches are cleaned up");
        assertTrue(valueNode.has("properties"),
            "properties should still exist");
    }
}
