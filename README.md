# Variant Interpretation Model in SHACL

This folder contains code for describing the Variant Interpretation Model using [SHACL](https://www.w3.org/TR/shacl/).

## Requirements
- [jq](https://stedolan.github.io/jq/) to transform the examples JSON file for processing.
- [jsonld-cli](https://github.com/digitalbazaar/jsonld-cli) to convert JSON-LD into triples.
- [rapper](http://librdf.org/raptor/rapper.html) from [Raptor RDF Syntax Library](http://librdf.org/raptor/) to convert triples into Turtle.
- [pySHACL](https://github.com/RDFLib/pySHACL) to validate Turtle against SHACL shapes.

## When loading in Protege
1. Load the examples.ttl file.
2. Directly import the [SEPIO ACMG Profile Ontology](https://raw.githubusercontent.com/monarch-initiative/SEPIO-ontology/master/src/ontology/extensions/clingen/clingen-acmg/sepio-clingen-acmg.owl) (this will import SEPIO as well).
3. Directly import the [ClinGen Terms Ontology](https://raw.githubusercontent.com/clingen-data-model/clingen-terms/master/clingen-terms.owl).
