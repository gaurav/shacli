# SHACLI: A SHACLI CLI

[![Build Status](https://travis-ci.org/gaurav/shacli.svg?branch=master)](https://travis-ci.org/gaurav/shacli)

`shacli` is a command-line interface to the [Shapes Constraint Language (SHACL)](https://www.w3.org/TR/shacl/).
It was originally developed as part of the effort to convert the [ClinGen Interpretation Model to SHACL](https://github.com/clingen-data-model/spec2shacl),
where we needed a tool that provided clear error messages to anyone using SHACL to validate incoming data.

It is implemented as a wrapper around [TopQuadrant's SHACL library](https://github.com/TopQuadrant/shacl).
