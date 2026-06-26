export interface XPathFunctionCatalog {
  [group: string]: XPathFunctionEntry[];
}

export interface XPathFunctionEntry {
  name: string;
  prefix: string;
  displayName: string;
  description: string;
  returnType: string;
  returnCardinality: string;
  arguments: XPathFunctionArgument[];
  signatures: XPathFunctionSignature[];
}

export interface XPathFunctionArgument {
  name: string;
  displayName: string;
  description: string;
  type: string;
  cardinality: string;
  minOccurs: number;
  maxOccurs: number;
  default: string | null;
  usage: string | null;
}

export interface XPathFunctionSignature {
  returnType: string;
  arguments: XPathFunctionSignatureArgument[];
  properties: string[];
}

export interface XPathFunctionSignatureArgument {
  name: string;
  type: string;
  default: string | null;
  usage: string | null;
}
