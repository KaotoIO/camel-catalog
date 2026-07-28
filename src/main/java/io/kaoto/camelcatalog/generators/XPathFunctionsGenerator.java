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
package io.kaoto.camelcatalog.generators;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.kaoto.camelcatalog.model.KaotoFunctionSignature;
import io.kaoto.camelcatalog.model.KaotoFunctionSignatureArgument;
import io.kaoto.camelcatalog.model.XPathFunction;
import io.kaoto.camelcatalog.model.XPathFunctionArgument;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates XPath 3.1 and XSLT function catalog entries from bundled W3C specification
 * XML resources and a hand-maintained XSLT function list.
 *
 * <p>Three classpath resources are used:</p>
 * <ul>
 *   <li>{@code xpath-function-catalog-31.xml} — W3C XPath/XQuery 3.1 function catalog.
 *       Provides function signatures, argument types, and XPath Data Model (XDM) cardinalities.
 *       Source: <a href="https://www.w3.org/TR/xpath-functions-31/">W3C XPath and XQuery Functions 3.1</a>.</li>
 *   <li>{@code xpath-functions-31-spec.xml} — W3C XPath/XQuery 3.1 specification document.
 *       Used for section-based grouping into categories (String, Numeric, DateAndTime, etc.)
 *       and for extracting summary descriptions.</li>
 *   <li>{@code xslt-functions-3.0.json} — hand-maintained JSON defining XSLT 3.0-specific
 *       functions (e.g. {@code current-group}, {@code document}, {@code key}) that are not
 *       part of the XPath function catalog but are available in XSLT stylesheets.</li>
 * </ul>
 *
 * <p>XPath 3.1 is a stable W3C Recommendation (2017), so the source XML files are bundled
 * as classpath resources rather than fetched at build time.</p>
 */
public class XPathFunctionsGenerator implements Generator {
    private static final Logger LOGGER = Logger.getLogger(XPathFunctionsGenerator.class.getName());

    private static final String CATALOG_RESOURCE = "functions/xslt/xpath-function-catalog-31.xml";
    private static final String SPEC_RESOURCE = "functions/xslt/xpath-functions-31-spec.xml";
    private static final String XSLT_RESOURCE = "functions/xslt/xslt-functions-3.0.json";
    private static final String SPEC_NS = "http://www.w3.org/xpath-functions/spec/namespace";

    private static final Map<String, String> SECTION_TO_GROUP = Map.ofEntries(
            Map.entry("accessors", "Node"),
            Map.entry("errors-and-diagnostics", "Context"),
            Map.entry("numeric-functions", "Numeric"),
            Map.entry("string-functions", "String"),
            Map.entry("anyURI-functions", "String"),
            Map.entry("boolean-functions", "Boolean"),
            Map.entry("durations", "DateAndTime"),
            Map.entry("dates-times", "DateAndTime"),
            Map.entry("QName-funcs", "QName"),
            Map.entry("node-functions", "Node"),
            Map.entry("sequence-functions", "Sequence"),
            Map.entry("json-functions", "Sequence"),
            Map.entry("context", "Context"),
            Map.entry("higher-order-functions", "HigherOrder"),
            Map.entry("substring.functions", "SubstringMatching"),
            Map.entry("string.match", "PatternMatching"),
            Map.entry("map-functions", "MapFunctions"),
            Map.entry("array-functions", "ArrayFunctions")
    );

    static final List<String> ALL_GROUPS = List.of(
            "String", "SubstringMatching", "PatternMatching", "Numeric",
            "DateAndTime", "Boolean", "QName", "Node", "Sequence",
            "Context", "Math", "MapFunctions", "ArrayFunctions", "HigherOrder", "XSLT"
    );

    /** W3C-defined namespace prefix-to-URI bindings used across XPath 3.1 function and type references. */
    static final Map<String, String> NAMESPACE_MAP = new TreeMap<>(Map.of(
            "array", "http://www.w3.org/2005/xpath-functions/array",
            "fn", "http://www.w3.org/2005/xpath-functions",
            "map", "http://www.w3.org/2005/xpath-functions/map",
            "math", "http://www.w3.org/2005/xpath-functions/math",
            "xs", "http://www.w3.org/2001/XMLSchema"
    ));

    private static final int MAX_OCCURS_UNBOUNDED = 2147483647;
    private static final Pattern WILDCARD_FN_PATTERN = Pattern.compile("^function\\(\\*\\)([?+*])?$");

    private final ObjectMapper jsonMapper;
    private final ClassLoader classLoader;

    /**
     * @param classLoader classloader to use for loading bundled W3C XML resources and XSLT function definitions
     */
    public XPathFunctionsGenerator(ClassLoader classLoader) {
        this.jsonMapper = new ObjectMapper();
        this.classLoader = classLoader;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, ObjectNode> generate() {
        Map<String, List<XPathFunction>> grouped = new LinkedHashMap<>();
        for (String group : ALL_GROUPS) {
            grouped.put(group, new ArrayList<>());
        }

        try {
            String catalogXml = loadResource(CATALOG_RESOURCE);
            String specXml = loadResource(SPEC_RESOURCE);

            if (catalogXml == null || specXml == null) {
                throw new IllegalStateException("Failed to load XPath function catalog resources");
            }

            Map<String, SectionInfo> sectionMap = buildFunctionSectionMap(specXml);
            Map<String, String> summaryMap = buildSummaryMap(catalogXml);
            Document doc = parseXml(catalogXml);

            NodeList functionNodes = doc.getElementsByTagNameNS(
                    SPEC_NS, "function");

            for (int i = 0; i < functionNodes.getLength(); i++) {
                Element funcElem = (Element) functionNodes.item(i);
                XPathFunction func = processFunction(funcElem, summaryMap, sectionMap);
                if (func != null) {
                    String group = resolveGroup(
                            funcElem.getAttribute("name"),
                            funcElem.getAttribute("prefix"),
                            sectionMap);
                    grouped.computeIfAbsent(group, k -> new ArrayList<>()).add(func);
                }
            }

            loadXsltFunctions(grouped);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating XPath function catalog", e);
            throw new RuntimeException("Error generating XPath function catalog", e);
        }

        for (List<XPathFunction> funcs : grouped.values()) {
            funcs.sort(Comparator.comparing(XPathFunction::getName));
        }

        return toResult(grouped);
    }

    private Map<String, ObjectNode> toResult(Map<String, List<XPathFunction>> grouped) {
        Map<String, ObjectNode> result = new LinkedHashMap<>();
        result.put("_metadata", buildMetadata());
        result.put("namespaces", jsonMapper.valueToTree(NAMESPACE_MAP));
        for (String group : ALL_GROUPS) {
            List<XPathFunction> funcs = grouped.getOrDefault(group, List.of());
            ObjectNode groupNode = jsonMapper.createObjectNode();
            for (XPathFunction func : funcs) {
                groupNode.set(func.getName(), jsonMapper.valueToTree(func));
            }
            result.put(group, groupNode);
        }
        return result;
    }

    private ObjectNode buildMetadata() {
        ObjectNode metadata = jsonMapper.createObjectNode();
        ObjectNode fieldSemantics = jsonMapper.createObjectNode();

        fieldSemantics.put("cardinality",
                "XDM type cardinality — what values the parameter accepts: "
                        + "\"\" (exactly one), \"?\" (zero or one), \"*\" (zero or more), \"+\" (one or more). "
                        + "Derived from the type annotation in the W3C spec.");
        fieldSemantics.put("minOccurs",
                "Positional optionality — whether the argument can be omitted from the function call: "
                        + "1 = must appear, 0 = can be omitted. "
                        + "Derived from which overloads/signatures exist.");
        fieldSemantics.put("maxOccurs",
                "Positional arity bound: 1 for all standard arguments, "
                        + "Integer.MAX_VALUE only for the variadic fn:concat.");

        metadata.set("fieldSemantics", fieldSemantics);
        return metadata;
    }

    private String loadResource(String resourceName) {
        try (var is = classLoader.getResourceAsStream(resourceName)) {
            if (is == null) {
                LOGGER.warning("Resource not found: " + resourceName);
                return null;
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error reading resource: " + resourceName, e);
            return null;
        }
    }

    private Document parseXml(String xml) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        var builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    XPathFunction processFunction(Element funcElem, Map<String, String> summaryMap,
                                         Map<String, SectionInfo> sectionMap) {
        String name = funcElem.getAttribute("name");
        String prefix = funcElem.getAttribute("prefix");

        if ("op".equals(prefix)) {
            return null;
        }

        List<Element> protos = getProtos(funcElem);
        if (protos.isEmpty()) {
            return null;
        }

        String summary = summaryMap.getOrDefault(prefix + ":" + name, "");
        Map<Integer, List<String>> propertiesMap = buildPropertiesMap(funcElem);

        if ("concat".equals(name) && "fn".equals(prefix)) {
            return buildConcatFunction(summary, propertiesMap);
        }

        return buildStandardFunction(name, prefix, summary, protos, propertiesMap);
    }

    /**
     * {@code fn:concat} is the only XPath function with a truly variadic signature
     * ({@code $arg1, $arg2, ...}) — the spec defines it as "two or more" arguments
     * with no fixed upper bound. Unlike other multi-arity functions (e.g. {@code fn:substring})
     * whose overloads are enumerated as separate {@code <proto>} elements in the spec XML,
     * {@code concat} has no finite overload list for {@link #buildStandardFunction} to parse.
     */
    private XPathFunction buildConcatFunction(String summary, Map<Integer, List<String>> propertiesMap) {
        var func = new XPathFunction();
        func.setName("concat");
        func.setPrefix("fn");
        func.setDisplayName("Concatenate");
        func.setDescription(summary.isEmpty()
                ? "Concatenates two or more xs:anyAtomicType arguments cast to xs:string."
                : summary);
        func.setReturnType("xs:string");
        func.setReturnCardinality("");

        var arg = new XPathFunctionArgument();
        arg.setName("args");
        arg.setDisplayName("$args");
        arg.setDescription("Arguments");
        arg.setType("xs:anyAtomicType");
        arg.setCardinality("");
        arg.setMinOccurs(2);
        arg.setMaxOccurs(MAX_OCCURS_UNBOUNDED);
        func.setArguments(List.of(arg));

        var sigArg1 = new KaotoFunctionSignatureArgument();
        sigArg1.setName("arg1");
        sigArg1.setType("xs:anyAtomicType?");
        var sigArg2 = new KaotoFunctionSignatureArgument();
        sigArg2.setName("arg2");
        sigArg2.setType("xs:anyAtomicType?");

        var sig = new KaotoFunctionSignature();
        sig.setReturnType("xs:string");
        sig.setArguments(List.of(sigArg1, sigArg2));
        sig.setProperties(propertiesMap.getOrDefault(2, List.of()));
        func.setSignatures(List.of(sig));

        return func;
    }

    private XPathFunction buildStandardFunction(String name, String prefix, String summary,
                                                 List<Element> protos, Map<Integer, List<String>> propertiesMap) {
        var func = new XPathFunction();
        String functionName = "fn".equals(prefix) ? name : prefix + ":" + name;
        func.setName(functionName);
        func.setPrefix(prefix);
        func.setDisplayName(toDisplayName(name));
        func.setDescription(summary);

        protos.sort((a, b) -> getArgElements(b).size() - getArgElements(a).size());
        Element longest = protos.get(0);
        List<Element> longestArgs = getArgElements(longest);
        int shortestArgCount = protos.stream()
                .mapToInt(p -> getArgElements(p).size())
                .min()
                .orElse(0);

        List<XPathFunctionArgument> arguments = new ArrayList<>();
        for (int i = 0; i < longestArgs.size(); i++) {
            Element argElem = longestArgs.get(i);
            String rawType = argElem.getAttribute("type");
            if (rawType.isEmpty()) rawType = "item()";
            TypeInfo typeInfo = parseTypeString(rawType);
            String argName = argElem.getAttribute("name");
            if (argName.isEmpty()) argName = "arg" + (i + 1);

            var arg = new XPathFunctionArgument();
            arg.setName(argName);
            arg.setDisplayName("$" + argName);
            arg.setDescription(toDisplayName(argName));
            arg.setType(typeInfo.baseType);
            arg.setCardinality(typeInfo.cardinality);
            arg.setMinOccurs(i >= shortestArgCount ? 0 : 1);
            arg.setMaxOccurs(1);
            String defaultVal = argElem.getAttribute("default");
            arg.setDefaultValue(defaultVal.isEmpty() ? null : defaultVal);
            String usage = argElem.getAttribute("usage");
            arg.setUsage(usage.isEmpty() ? null : usage);
            arguments.add(arg);
        }
        func.setArguments(arguments);

        String returnTypeStr = longest.getAttribute("return-type");
        if (returnTypeStr.isEmpty()) returnTypeStr = "item()*";
        TypeInfo returnTypeInfo = parseTypeString(returnTypeStr);
        func.setReturnType(returnTypeInfo.baseType);
        func.setReturnCardinality(returnTypeInfo.cardinality);
        func.setReturnCollection("*".equals(returnTypeInfo.cardinality)
                || "+".equals(returnTypeInfo.cardinality));

        List<KaotoFunctionSignature> signatures = new ArrayList<>();
        for (Element proto : protos) {
            List<Element> protoArgs = getArgElements(proto);
            int arity = protoArgs.size();

            var sig = new KaotoFunctionSignature();
            String protoReturnType = proto.getAttribute("return-type");
            sig.setReturnType(protoReturnType.isEmpty() ? "item()*" : protoReturnType);

            List<KaotoFunctionSignatureArgument> sigArgs = new ArrayList<>();
            for (Element protoArg : protoArgs) {
                var sigArg = new KaotoFunctionSignatureArgument();
                sigArg.setName(protoArg.getAttribute("name"));
                String type = protoArg.getAttribute("type");
                sigArg.setType(type.isEmpty() ? "item()" : type);
                String defaultVal = protoArg.getAttribute("default");
                sigArg.setDefaultValue(defaultVal.isEmpty() ? null : defaultVal);
                String usage = protoArg.getAttribute("usage");
                sigArg.setUsage(usage.isEmpty() ? null : usage);
                sigArgs.add(sigArg);
            }
            sig.setArguments(sigArgs);

            List<String> props = propertiesMap.getOrDefault(arity, propertiesMap.getOrDefault(0, List.of()));
            sig.setProperties(props);

            signatures.add(sig);
        }
        func.setSignatures(signatures);

        return func;
    }

    private List<Element> getProtos(Element funcElem) {
        List<Element> protos = new ArrayList<>();
        NodeList signaturesList = funcElem.getElementsByTagNameNS(
                SPEC_NS, "signatures");
        if (signaturesList.getLength() == 0) return protos;

        Element signaturesElem = (Element) signaturesList.item(0);
        NodeList protoNodes = signaturesElem.getElementsByTagNameNS(
                SPEC_NS, "proto");

        for (int i = 0; i < protoNodes.getLength(); i++) {
            protos.add((Element) protoNodes.item(i));
        }
        return protos;
    }

    private List<Element> getArgElements(Element proto) {
        List<Element> args = new ArrayList<>();
        NodeList argNodes = proto.getElementsByTagNameNS(
                SPEC_NS, "arg");
        for (int i = 0; i < argNodes.getLength(); i++) {
            args.add((Element) argNodes.item(i));
        }
        return args;
    }

    Map<Integer, List<String>> buildPropertiesMap(Element funcElem) {
        Map<Integer, List<String>> map = new LinkedHashMap<>();
        NodeList propsNodes = funcElem.getElementsByTagNameNS(
                SPEC_NS, "properties");

        for (int i = 0; i < propsNodes.getLength(); i++) {
            Element propsElem = (Element) propsNodes.item(i);
            if (!propsElem.getParentNode().equals(funcElem)) continue;

            String arityStr = propsElem.getAttribute("arity");
            int arity = arityStr.isEmpty() ? 0 : Integer.parseInt(arityStr);

            List<String> props = new ArrayList<>();
            NodeList propertyNodes = propsElem.getElementsByTagNameNS(
                    SPEC_NS, "property");
            for (int j = 0; j < propertyNodes.getLength(); j++) {
                String text = propertyNodes.item(j).getTextContent().trim();
                if (!text.isEmpty()) {
                    props.add(text);
                }
            }
            map.put(arity, props);
        }
        return map;
    }

    static TypeInfo parseTypeString(String typeStr) {
        if (typeStr == null || typeStr.isBlank()) {
            return new TypeInfo("item()", "");
        }

        String trimmed = typeStr.trim();

        if ("none".equals(trimmed) || "empty-sequence()".equals(trimmed)) {
            return new TypeInfo(trimmed, "");
        }

        Matcher wildcardMatcher = WILDCARD_FN_PATTERN.matcher(trimmed);
        if (wildcardMatcher.matches()) {
            String card = wildcardMatcher.group(1);
            return new TypeInfo("function(*)", card != null ? card : "");
        }

        if (trimmed.startsWith("function(")) {
            return new TypeInfo("function(*)", "");
        }

        String baseType = trimmed;
        String cardinality = "";
        char lastChar = trimmed.charAt(trimmed.length() - 1);

        if (lastChar == '?' || lastChar == '+') {
            cardinality = String.valueOf(lastChar);
            baseType = trimmed.substring(0, trimmed.length() - 1);
        } else if (lastChar == '*' && !baseType.endsWith("(*)")) {
            cardinality = "*";
            baseType = trimmed.substring(0, trimmed.length() - 1);
        }

        return new TypeInfo(baseType, cardinality);
    }

    Map<String, String> buildSummaryMap(String rawXml) {
        Map<String, String> map = new HashMap<>();
        Pattern tagPattern = Pattern.compile("<fos:function\\s([^>]*)>");
        Matcher tagMatcher = tagPattern.matcher(rawXml);

        while (tagMatcher.find()) {
            String attrs = tagMatcher.group(1);
            Matcher nameMatch = Pattern.compile("\\bname=\"([^\"]+)\"").matcher(attrs);
            Matcher prefixMatch = Pattern.compile("\\bprefix=\"([^\"]+)\"").matcher(attrs);
            if (!nameMatch.find() || !prefixMatch.find()) continue;

            int startPos = tagMatcher.start();
            int endPos = rawXml.indexOf("</fos:function>", startPos);
            if (endPos == -1) continue;

            String funcXml = rawXml.substring(startPos, endPos);
            Pattern summaryPattern = Pattern.compile(
                    "<fos:summary>([^<]*(?:<(?!/fos:summary>)[^<]*)*)</fos:summary>");
            Matcher summaryMatcher = summaryPattern.matcher(funcXml);
            if (summaryMatcher.find()) {
                String key = prefixMatch.group(1) + ":" + nameMatch.group(1);
                map.put(key, stripXmlTags(summaryMatcher.group(1)));
            }
        }
        return map;
    }

    Map<String, SectionInfo> buildFunctionSectionMap(String specXml) {
        record Event(int pos, String type, String level, String id, String prefix, String name) {}

        List<Event> events = new ArrayList<>();

        Pattern divOpenPattern = Pattern.compile("<(div[1-3])\\s[^>]*?id=\"([^\"]+)\"[^>]*>");
        Matcher divOpenMatcher = divOpenPattern.matcher(specXml);
        while (divOpenMatcher.find()) {
            events.add(new Event(divOpenMatcher.start(), "open", divOpenMatcher.group(1),
                    divOpenMatcher.group(2), null, null));
        }

        Pattern divClosePattern = Pattern.compile("</(div[1-3])\\s*>");
        Matcher divCloseMatcher = divClosePattern.matcher(specXml);
        while (divCloseMatcher.find()) {
            events.add(new Event(divCloseMatcher.start(), "close", divCloseMatcher.group(1),
                    null, null, null));
        }

        Pattern funcPattern = Pattern.compile("<\\?function\\s+(\\w+):([\\w-]+)\\s*\\?>");
        Matcher funcMatcher = funcPattern.matcher(specXml);
        while (funcMatcher.find()) {
            events.add(new Event(funcMatcher.start(), "func", null, null,
                    funcMatcher.group(1), funcMatcher.group(2)));
        }

        events.sort(Comparator.comparingInt(Event::pos));

        Map<String, SectionInfo> map = new HashMap<>();
        List<String> divLevels = List.of("div1", "div2", "div3");
        String[] ids = {null, null, null};

        for (Event event : events) {
            int levelIdx = event.level() != null ? divLevels.indexOf(event.level()) : -1;
            switch (event.type()) {
                case "open" -> {
                    ids[levelIdx] = event.id();
                    for (int i = levelIdx + 1; i < ids.length; i++) ids[i] = null;
                }
                case "close" -> {
                    for (int i = levelIdx; i < ids.length; i++) ids[i] = null;
                }
                case "func" -> map.put(event.prefix() + ":" + event.name(),
                        new SectionInfo(ids[0], ids[1], ids[2]));
            }
        }

        return map;
    }

    String resolveGroup(String name, String prefix, Map<String, SectionInfo> sectionMap) {
        if ("math".equals(prefix)) return "Math";
        if ("map".equals(prefix)) return "MapFunctions";
        if ("array".equals(prefix)) return "ArrayFunctions";

        SectionInfo sectionInfo = sectionMap.get(prefix + ":" + name);
        if (sectionInfo != null) {
            for (String sectionId : List.of(
                    sectionInfo.div3 != null ? sectionInfo.div3 : "",
                    sectionInfo.div2 != null ? sectionInfo.div2 : "",
                    sectionInfo.div1 != null ? sectionInfo.div1 : "")) {
                String group = SECTION_TO_GROUP.get(sectionId);
                if (group != null) return group;
            }
        }

        return "Sequence";
    }

    private void loadXsltFunctions(Map<String, List<XPathFunction>> grouped) {
        String xsltJson = loadResource(XSLT_RESOURCE);
        if (xsltJson == null) {
            LOGGER.warning("XSLT functions resource not found");
            return;
        }

        try {
            List<XPathFunction> xsltFunctions = jsonMapper.readValue(
                    xsltJson, new TypeReference<List<XPathFunction>>() {});
            for (XPathFunction function : xsltFunctions) {
                String cardinality = function.getReturnCardinality();
                function.setReturnCollection("*".equals(cardinality) || "+".equals(cardinality));
            }
            grouped.computeIfAbsent("XSLT", k -> new ArrayList<>()).addAll(xsltFunctions);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to parse XSLT functions JSON", e);
        }
    }

    private static String stripXmlTags(String xml) {
        String processed = xml
                .replaceAll("<sup>(.*?)</sup>", "^$1")
                .replaceAll("<bibref\\s+ref=\"([^\"]+)\"\\s*/>", "[$1]")
                .replaceAll("<specref\\s+ref=\"([^\"]+)\"\\s*/>", "[$1]");

        StringBuilder result = new StringBuilder();
        boolean inTag = false;
        for (int i = 0; i < processed.length(); i++) {
            char ch = processed.charAt(i);
            if (ch == '<') {
                inTag = true;
            } else if (ch == '>') {
                inTag = false;
            } else if (!inTag) {
                result.append(ch);
            }
        }
        return result.toString().replaceAll("\\s+", " ").trim();
    }

    private static String toDisplayName(String name) {
        String[] parts = name.split("-");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(Character.toUpperCase(parts[i].charAt(0)));
            sb.append(parts[i].substring(1));
        }
        return sb.toString();
    }

    record TypeInfo(String baseType, String cardinality) {}
    record SectionInfo(String div1, String div2, String div3) {}
}
