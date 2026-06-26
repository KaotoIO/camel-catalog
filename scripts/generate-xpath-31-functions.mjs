// @ts-check
/**
 * Generates the XPath function catalog JSON for the Kaoto DataMapper.
 *
 * Fetches two W3C sources:
 * - function-catalog.xml — machine-readable function signatures (names, arguments, return types)
 * - xpath-functions.xml — spec source with section hierarchy used to classify functions into groups
 *
 * Also reads XSLT functions from static/source/xslt-functions-3.0.json (XSLT 3.0 has no
 * machine-readable catalog).
 *
 * Output: a single JSON file containing all functions grouped by function group, with raw
 * XDM type strings preserved from the spec. The kaoto UI conversion layer handles mapping
 * to internal Types enum at runtime.
 *
 * Run: yarn generate:xpath-functions
 *
 * @module generate-xpath-31-functions
 */
import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

import { XMLParser } from 'fast-xml-parser';

const __dirname = dirname(fileURLToPath(import.meta.url));
const PROJECT_ROOT = resolve(__dirname, '..');

const CATALOG_URL = 'https://www.w3.org/TR/xpath-functions-31/function-catalog.xml';
const SPEC_SOURCE_URL =
  'https://raw.githubusercontent.com/w3c/qtspecs/6cef5545618cd2edfd94f32af488a16bb19c598c/specifications/xpath-functions-31/src/xpath-functions.xml';

const XSLT_SOURCE_PATH = resolve(PROJECT_ROOT, 'static/source/xslt-functions-3.0.json');
const OUTPUT_DIR = resolve(PROJECT_ROOT, 'static/final');
const OUTPUT_FILE = resolve(OUTPUT_DIR, 'xpath-functions-3.1.json');

const SECTION_TO_GROUP = {
  accessors: 'Node',
  'errors-and-diagnostics': 'Context',
  'numeric-functions': 'Numeric',
  'string-functions': 'String',
  'anyURI-functions': 'String',
  'boolean-functions': 'Boolean',
  durations: 'DateAndTime',
  'dates-times': 'DateAndTime',
  'QName-funcs': 'QName',
  'node-functions': 'Node',
  'sequence-functions': 'Sequence',
  'json-functions': 'Sequence',
  context: 'Context',
  'higher-order-functions': 'HigherOrder',
  'substring.functions': 'SubstringMatching',
  'string.match': 'PatternMatching',
  'map-functions': 'MapFunctions',
  'array-functions': 'ArrayFunctions',
};

const ALL_GROUPS = [
  'String',
  'SubstringMatching',
  'PatternMatching',
  'Numeric',
  'DateAndTime',
  'Boolean',
  'QName',
  'Node',
  'Sequence',
  'Context',
  'Math',
  'MapFunctions',
  'ArrayFunctions',
  'HigherOrder',
  'XSLT',
];

function parseTypeString(typeStr) {
  if (!typeStr) return { baseType: 'item()', cardinality: '' };

  const trimmed = typeStr.trim();

  if (trimmed === 'none' || trimmed === 'empty-sequence()') {
    return { baseType: trimmed, cardinality: '' };
  }

  const wildcardFnMatch = /^function\(\*\)([?+*])?$/.exec(trimmed);
  if (wildcardFnMatch) {
    return { baseType: 'function(*)', cardinality: wildcardFnMatch[1] ?? '' };
  }

  if (trimmed.startsWith('function(')) {
    return { baseType: 'function(*)', cardinality: '' };
  }

  let cardinality = '';
  let baseType = trimmed;
  const lastChar = trimmed[trimmed.length - 1];

  if (lastChar === '?' || lastChar === '+') {
    cardinality = lastChar;
    baseType = trimmed.slice(0, -1);
  } else if (lastChar === '*' && !baseType.endsWith('(*)')) {
    cardinality = '*';
    baseType = trimmed.slice(0, -1);
  }

  return { baseType, cardinality };
}

function cardinalityToOccurs(cardinality) {
  switch (cardinality) {
    case '?':
      return { minOccurs: 0, maxOccurs: 1 };
    case '*':
      return { minOccurs: 0, maxOccurs: 2147483647 };
    case '+':
      return { minOccurs: 1, maxOccurs: 2147483647 };
    default:
      return { minOccurs: 1, maxOccurs: 1 };
  }
}

function stripXmlTags(xml) {
  let result = '';
  let inTag = false;
  for (const ch of xml) {
    if (ch === '<') {
      inTag = true;
    } else if (ch === '>') {
      inTag = false;
    } else if (!inTag) {
      result += ch;
    }
  }
  return result.replace(/\s+/g, ' ').trim();
}

function toDisplayName(name) {
  return name
    .split('-')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ');
}

function ensureArray(val) {
  if (!val) return [];
  return Array.isArray(val) ? val : [val];
}

function buildFunctionSectionMap(specXml) {
  const events = [];

  const divOpenRegex = /<(div[1-3])\s[^>]*?id="([^"]+)"[^>]*>/g;
  let m;
  while ((m = divOpenRegex.exec(specXml)) !== null) {
    events.push({ pos: m.index, type: 'open', level: m[1], id: m[2] });
  }

  const divCloseRegex = /<\/(div[1-3])\s*>/g;
  while ((m = divCloseRegex.exec(specXml)) !== null) {
    events.push({ pos: m.index, type: 'close', level: m[1] });
  }

  const funcRegex = /<\?function\s+(\w+):([\w-]+)\s*\?>/g;
  while ((m = funcRegex.exec(specXml)) !== null) {
    events.push({ pos: m.index, type: 'func', prefix: m[1], name: m[2] });
  }

  events.sort((a, b) => a.pos - b.pos);

  /** @type {Map<string, {div1: string|null, div2: string|null, div3: string|null}>} */
  const map = new Map();
  const DIV_LEVELS = ['div1', 'div2', 'div3'];
  const ids = [null, null, null];

  for (const event of events) {
    const levelIdx = DIV_LEVELS.indexOf(event.level);
    if (event.type === 'open') {
      ids[levelIdx] = event.id;
      for (let i = levelIdx + 1; i < ids.length; i++) ids[i] = null;
    } else if (event.type === 'close') {
      for (let i = levelIdx; i < ids.length; i++) ids[i] = null;
    } else {
      map.set(`${event.prefix}:${event.name}`, { div1: ids[0], div2: ids[1], div3: ids[2] });
    }
  }

  return map;
}

function resolveGroupFromSection(sectionInfo) {
  if (!sectionInfo) return null;
  for (const sectionId of [sectionInfo.div3, sectionInfo.div2, sectionInfo.div1]) {
    if (sectionId && SECTION_TO_GROUP[sectionId]) {
      return SECTION_TO_GROUP[sectionId];
    }
  }
  return null;
}

function resolveGroup(name, prefix, sectionMap) {
  if (prefix === 'math') return 'Math';
  if (prefix === 'map') return 'MapFunctions';
  if (prefix === 'array') return 'ArrayFunctions';

  const sectionInfo = sectionMap.get(`${prefix}:${name}`);
  const group = resolveGroupFromSection(sectionInfo);
  if (group) return group;

  if (sectionInfo) {
    const sectionPath = [sectionInfo.div1, sectionInfo.div2, sectionInfo.div3].filter(Boolean).join(' > ');
    throw new Error(`No SECTION_TO_GROUP mapping for ${prefix}:${name} in section hierarchy: ${sectionPath}`);
  }

  return 'Sequence';
}

function buildSummaryMap(rawXml) {
  const map = new Map();
  const tagRegex = /<fos:function\s([^>]*)>/g;
  let match;
  while ((match = tagRegex.exec(rawXml)) !== null) {
    const attrs = match[1];
    const nameMatch = /\bname="([^"]+)"/.exec(attrs);
    const prefixMatch = /\bprefix="([^"]+)"/.exec(attrs);
    if (!nameMatch || !prefixMatch) continue;

    const startPos = match.index;
    const endPos = rawXml.indexOf('</fos:function>', startPos);
    if (endPos === -1) continue;

    const funcXml = rawXml.substring(startPos, endPos);
    const summaryMatch = funcXml.match(/<fos:summary>([^<]*(?:<(?!\/fos:summary>)[^<]*)*)<\/fos:summary>/);
    if (summaryMatch) {
      const key = `${prefixMatch[1]}:${nameMatch[1]}`;
      map.set(key, stripXmlTags(summaryMatch[1]));
    }
  }
  return map;
}

function buildPropertiesMap(func) {
  /** @type {Map<number, string[]>} */
  const map = new Map();
  const propertiesGroups = ensureArray(func['fos:properties']);
  for (const group of propertiesGroups) {
    const arity = parseInt(group['@_arity'] ?? '0', 10);
    const props = ensureArray(group['fos:property']).map((p) => (typeof p === 'string' ? p : p['#text'] || ''));
    map.set(arity, props);
  }
  return map;
}

function mergeSignatures(protos, propertiesMap) {
  const sorted = [...protos].sort((a, b) => {
    const aLen = ensureArray(a['fos:arg']).length;
    const bLen = ensureArray(b['fos:arg']).length;
    return bLen - aLen;
  });

  const longest = sorted[0];
  const longestArgs = ensureArray(longest['fos:arg']);
  const shortestArgCount = Math.min(...protos.map((p) => ensureArray(p['fos:arg']).length));

  const args = longestArgs.map((arg, i) => {
    const rawType = arg['@_type'] || 'item()';
    const { baseType, cardinality } = parseTypeString(rawType);
    const occurs = cardinalityToOccurs(cardinality);
    const argName = arg['@_name'] || `arg${i + 1}`;

    if (i >= shortestArgCount) {
      occurs.minOccurs = 0;
    }

    return {
      name: argName,
      displayName: `$${argName}`,
      description: toDisplayName(argName),
      type: baseType,
      cardinality,
      minOccurs: occurs.minOccurs,
      maxOccurs: occurs.maxOccurs,
      default: arg['@_default'] || null,
      usage: arg['@_usage'] || null,
    };
  });

  const returnTypeStr = longest['@_return-type'] || 'item()*';
  const { baseType: returnType, cardinality: returnCardinality } = parseTypeString(returnTypeStr);

  const signatures = protos.map((proto) => {
    const protoArgs = ensureArray(proto['fos:arg']);
    const arity = protoArgs.length;
    return {
      returnType: proto['@_return-type'] || 'item()*',
      arguments: protoArgs.map((arg) => ({
        name: arg['@_name'] || '',
        type: arg['@_type'] || 'item()',
        default: arg['@_default'] || null,
        usage: arg['@_usage'] || null,
      })),
      properties: propertiesMap.get(arity) || propertiesMap.get(0) || [],
    };
  });

  return { args, returnType, returnCardinality, signatures };
}

function processFunction(func, summaryMap, sectionMap) {
  const name = func['@_name'];
  const prefix = func['@_prefix'];

  if (prefix === 'op') return null;

  const protos = ensureArray(func['fos:signatures']?.['fos:proto']);
  if (protos.length === 0) return null;

  const group = resolveGroup(name, prefix, sectionMap);
  const summary = summaryMap.get(`${prefix}:${name}`) || '';
  const displayName = toDisplayName(name);
  const propertiesMap = buildPropertiesMap(func);

  let funcDef;
  if (name === 'concat' && prefix === 'fn') {
    funcDef = {
      name,
      prefix,
      displayName: 'Concatenate',
      description: summary || 'Concatenates two or more xs:anyAtomicType arguments cast to xs:string.',
      returnType: 'xs:string',
      returnCardinality: '',
      arguments: [
        {
          name: 'args',
          displayName: '$args',
          description: 'Arguments',
          type: 'xs:anyAtomicType',
          cardinality: '',
          minOccurs: 2,
          maxOccurs: 2147483647,
          default: null,
          usage: null,
        },
      ],
      signatures: [
        {
          returnType: 'xs:string',
          arguments: [
            { name: 'arg1', type: 'xs:anyAtomicType?', default: null, usage: null },
            { name: 'arg2', type: 'xs:anyAtomicType?', default: null, usage: null },
          ],
          properties: propertiesMap.get(2) || [],
        },
      ],
    };
  } else {
    const { args, returnType, returnCardinality, signatures } = mergeSignatures(protos, propertiesMap);
    const functionName = prefix === 'fn' ? name : `${prefix}:${name}`;
    funcDef = {
      name: functionName,
      prefix,
      displayName,
      description: summary,
      returnType,
      returnCardinality,
      arguments: args,
      signatures,
    };
  }

  return { group, ...funcDef };
}

async function fetchSpecSources() {
  console.log('Fetching function catalog and spec source...');
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 30_000);

  try {
    const [catalogResponse, specResponse] = await Promise.all([
      fetch(CATALOG_URL, { signal: controller.signal }),
      fetch(SPEC_SOURCE_URL, { signal: controller.signal }),
    ]);

    if (!catalogResponse.ok) {
      throw new Error(`Failed to fetch catalog: ${catalogResponse.status} ${catalogResponse.statusText}`);
    }
    if (!specResponse.ok) {
      throw new Error(`Failed to fetch spec source: ${specResponse.status} ${specResponse.statusText}`);
    }

    const catalogXml = await catalogResponse.text();
    const specXml = await specResponse.text();
    console.log(`Downloaded catalog (${catalogXml.length} bytes) and spec source (${specXml.length} bytes)`);
    return { catalogXml, specXml };
  } finally {
    clearTimeout(timeout);
  }
}

async function loadXsltFunctions() {
  const content = await readFile(XSLT_SOURCE_PATH, 'utf-8');
  return JSON.parse(content);
}

function groupFunctions(functions, summaryMap, sectionMap) {
  /** @type {Record<string, Array<*>>} */
  const grouped = {};
  let skippedOp = 0;
  let processed = 0;
  const unmappedFunctions = [];

  for (const func of functions) {
    const result = processFunction(func, summaryMap, sectionMap);
    if (!result) {
      skippedOp++;
      continue;
    }
    processed++;
    const { group, ...funcData } = result;
    if (!grouped[group]) grouped[group] = [];
    grouped[group].push(funcData);

    const key = `${func['@_prefix']}:${func['@_name']}`;
    if (!sectionMap.has(key) && func['@_prefix'] !== 'op') {
      unmappedFunctions.push(key);
    }
  }

  if (unmappedFunctions.length > 0) {
    console.log(`\nNote: ${unmappedFunctions.length} functions not found in spec source (defaulted to Sequence):`);
    console.log(`  ${unmappedFunctions.join(', ')}`);
  }

  return { grouped, processed, skippedOp };
}

async function main() {
  const { catalogXml, specXml } = await fetchSpecSources();

  const sectionMap = buildFunctionSectionMap(specXml);
  console.log(`Extracted section mapping for ${sectionMap.size} functions from spec source`);

  const parser = new XMLParser({
    ignoreAttributes: false,
    attributeNamePrefix: '@_',
    textNodeName: '#text',
    isArray: (name) => ['fos:function', 'fos:proto', 'fos:arg', 'fos:property', 'fos:properties', 'p'].includes(name),
  });
  const doc = parser.parse(catalogXml);
  const functions = doc['fos:functions']?.['fos:function'] || [];
  console.log(`Parsed ${functions.length} function definitions from catalog`);

  const summaryMap = buildSummaryMap(catalogXml);
  const { grouped, processed, skippedOp } = groupFunctions(functions, summaryMap, sectionMap);

  for (const group of ALL_GROUPS) {
    if (!grouped[group]) grouped[group] = [];
  }

  const xsltFunctions = await loadXsltFunctions();
  grouped['XSLT'] = xsltFunctions;
  console.log(`Loaded ${xsltFunctions.length} XSLT functions from ${XSLT_SOURCE_PATH}`);

  /** @type {Record<string, Array<*>>} */
  const output = {};
  for (const group of ALL_GROUPS) {
    const funcs = grouped[group] || [];
    funcs.sort((a, b) => a.name.localeCompare(b.name));
    output[group] = funcs;
  }

  await mkdir(OUTPUT_DIR, { recursive: true });
  const json = JSON.stringify(output, null, 2) + '\n';
  await writeFile(OUTPUT_FILE, json);
  console.log(`\nWritten ${OUTPUT_FILE}`);

  let totalFunctions = 0;
  for (const group of ALL_GROUPS) {
    const count = output[group].length;
    totalFunctions += count;
    console.log(`  ${group}: ${count} functions`);
  }

  console.log(
    `\nDone: ${processed} XPath functions + ${xsltFunctions.length} XSLT functions = ${totalFunctions} total`,
  );
  console.log(`Skipped: ${skippedOp} op: functions`);
}

try {
  await main();
} catch (err) {
  console.error('Error:', err.message);
  throw err;
}
