# Shacli: A SHACL CLI

[![Build Status](https://travis-ci.org/gaurav/shacli.svg?branch=master)](https://travis-ci.org/gaurav/shacli)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.shacli/shacli_2.12/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.shacli/shacli_2.12)

Shacli is a command-line interface to the [Shapes Constraint Language (SHACL)](https://www.w3.org/TR/shacl/).
It was originally developed as part of the effort to convert the [ClinGen Interpretation Model to SHACL](https://github.com/clingen-data-model/spec2shacl),
where we needed a tool that provided clear error messages to anyone using SHACL to validate incoming data.

It is implemented as a wrapper around [TopQuadrant's SHACL library](https://github.com/TopQuadrant/shacl).

## How to run Shacli

Shacli can be run directly by using [Coursier](https://get-coursier.io/).
Coursier itself will [need to be installed first](https://get-coursier.io/docs/cli-overview.html#install-native-launcher), and is available on package managers
such as [Homebrew](https://brew.sh/).

Once Coursier is installed, you can start Shacli. Note the use of `--` to separate
Coursier's arguments from Shacli's arguments.

```console
$ coursier launch org.shacli:shacli_2.12:0.1 -- --help
SHACLI: A SHACLI CLI v0.1
  -h, --help      Show help message
  -v, --version   Show version of this program

Subcommand: validate
  -d, --display-nodes      Display all failing nodes as Turtle
  -i, --ignore  <arg>...   Don't display SourceConstraintComponent ending with
                           these strings
  -o, --only  <arg>...     Only display SourceConstraintComponent ending with
                           these strings
  -h, --help               Show help message

 trailing arguments:
  shapes (required)   Shapes file to validate (in Turtle)
  data (required)     Data file(s) to validate (in Turtle)
Subcommand: generate
  -b, --base-u-r-i  <arg>   Base URI of the shapes to generate
  -o, --output  <arg>       Output file where SHACL should be written
  -r, --reasoning  <arg>    Choose reasoning: none (default), rdfs or owl
  -h, --help                Show help message

 trailing arguments:
  data (required)   Data file(s) or directories to validate (in Turtle)
```

You can try validating the ClinGen examples files from the base directory using

```console
$ ls
CHANGELOG.md LICENSE      README.md    build.sbt    docs         project      sonatype.sbt src          target
$ git branch
  master
* test-clingen-requirements
$ coursier launch org.shacli:shacli_2.12:0.1 -- validate src/test/resources/clingen_shapes.ttl src/test/resources/clingen_data.jsonld
10:55:08.015 [main] INFO  org.renci.shacli.ShacliApp$ - Starting validation of src/test/resources/clingen_data.jsonld against src/test/resources/clingen_shapes.ttl.
10:55:08.545 [main] WARN  org.apache.jena.riot - Bad IRI: <7/2/2012 8:45 AM> Spaces are not legal in URIs/IRIs.
10:55:08.629 [main] WARN  org.renci.shacli.ShacliApp$ - Resource http://dataexchange.clinicalgenome.org/CGEX_CondMech165 (types: http://purl.obolibrary.org/obo/SEPIO_0000228; props: DC:source, http://purl.obolibrary.org/obo/SEPIO_0000278, http://purl.obolibrary.org/obo/SEPIO_0000276, http://purl.obolibrary.org/obo/SEPIO_0000197, http://purl.obolibrary.org/obo/SEPIO_0000167, rdf:type) was not checked.
10:55:08.629 [main] WARN  org.renci.shacli.ShacliApp$ - Resource http://purl.obolibrary.org/obo/SEPIO_0000174 (types: RDFS:Class; props: RDFS:subClassOf, rdf:type) was not checked.
10:55:08.630 [main] WARN  org.renci.shacli.ShacliApp$ - Resource http://purl.obolibrary.org/obo/SEPIO_0000265 (types: none; props: RDFS:label) was not checked.
10:55:08.630 [main] WARN  org.renci.shacli.ShacliApp$ - Resource http://purl.obolibrary.org/obo/SEPIO_0000154 (types: none; props: RDFS:label) was not checked.
10:55:08.630 [main] WARN  org.renci.shacli.ShacliApp$ - Resource http://dataexchange.clinicalgenome.org/CGEX_Null406 (types: http://purl.obolibrary.org/obo/SEPIO_0000274; props: http://purl.obolibrary.org/obo/SEPIO_0000275, http://purl.obolibrary.org/obo/SEPIO_0000197, rdf:type) was not checked.
10:55:08.630 [main] WARN  org.renci.shacli.ShacliApp$ - Resource http://dataexchange.clinicalgenome.org/CGEX_Contrib519 (types: http://purl.obolibrary.org/obo/SEPIO_0000158; props: http://purl.obolibrary.org/obo/SEPIO_0000160, http://purl.obolibrary.org/obo/SEPIO_0000017, http://purl.obolibrary.org/obo/BFO_0000055, rdf:type) was not checked.
10:55:08.630 [main] WARN  org.renci.shacli.ShacliApp$ - Resource http://reg.genome.network/allele/CA256496 (types: http://purl.obolibrary.org/obo/GENO_0000890; props: http://dataexchange.clinicalgenome.org/CG_0001, rdf:type) was not checked.
10:55:08.630 [main] WARN  org.renci.shacli.ShacliApp$ - Resource http://dataexchange.clinicalgenome.org/CGEX_GenCond054 (types: http://purl.obolibrary.org/obo/SEPIO_0000219; props: http://purl.obolibrary.org/obo/SEPIO-CG_98901, rdf:type) was not checked.
10:55:08.631 [main] WARN  org.renci.shacli.ShacliApp$ - Resource http://dataexchange.clinicalgenome.org/CGEX_EvLn132 (types: http://purl.obolibrary.org/obo/SEPIO_0000002; props: http://purl.obolibrary.org/obo/SEPIO_0000084, http://purl.obolibrary.org/obo/SEPIO_0000084, http://purl.obolibrary.org/obo/SEPIO_0000084, http://purl.obolibrary.org/obo/SEPIO_0000084, rdf:type) was not checked.
10:55:08.631 [main] WARN  org.renci.shacli.ShacliApp$ - Resource http://purl.obolibrary.org/obo/SEPIO_0000268 (types: none; props: RDFS:label) was not checked.
10:55:08.631 [main] WARN  org.renci.shacli.ShacliApp$ - Resource http://dataexchange.clinicalgenome.org/CGEX_Agent010 (types: PROV:agent; props: RDFS:label, rdf:type) was not checked.
10:55:08.631 [main] WARN  org.renci.shacli.ShacliApp$ - Resource http://purl.obolibrary.org/obo/Orphanet_247 (types: none; props: RDFS:label) was not checked.
10:55:08.631 [main] WARN  org.renci.shacli.ShacliApp$ - Resource http://dataexchange.clinicalgenome.org/CGEX_BMVR161 (types: http://purl.obolibrary.org/obo/SEPIO_0000246; props: DC:description, http://purl.obolibrary.org/obo/SEPIO_0000278, http://purl.obolibrary.org/obo/SEPIO_0000197, rdf:type) was not checked.
10:55:08.631 [main] WARN  org.renci.shacli.ShacliApp$ - Resource http://dataexchange.clinicalgenome.org/CGEX_CtxAll099 (types: http://purl.obolibrary.org/obo/GENO_0000891; props: http://dataexchange.clinicalgenome.org/CG_0100, http://dataexchange.clinicalgenome.org/CG_0003, rdf:type) was not checked.
10:55:08.631 [main] WARN  org.renci.shacli.ShacliApp$ - Resource http://purl.obolibrary.org/obo/SEPIO_0000329 (types: none; props: RDFS:label) was not checked.
10:55:08.632 [main] WARN  org.renci.shacli.ShacliApp$ - Resource http://purl.obolibrary.org/obo/SEPIO-CG_99034 (types: http://purl.obolibrary.org/obo/SEPIO_0000192; props: RDFS:label, DC:description, http://purl.obolibrary.org/obo/SEPIO_0000196, http://purl.obolibrary.org/obo/SEPIO_0000184, rdf:type) was not checked.
10:55:08.632 [main] WARN  org.renci.shacli.ShacliApp$ - Resource http://purl.obolibrary.org/obo/SEPIO-CG_99015 (types: none; props: RDFS:label) was not checked.
10:55:08.632 [main] WARN  org.renci.shacli.ShacliApp$ - Resource http://www.genenames.org/12403 (types: none; props: RDFS:label) was not checked.
10:55:08.632 [main] WARN  org.renci.shacli.ShacliApp$ - Resource http://dataexchange.clinicalgenome.org/CGEX_MolCon169 (types: http://purl.obolibrary.org/obo/SEPIO_0000229; props: http://purl.obolibrary.org/obo/SEPIO_0000275, http://purl.obolibrary.org/obo/SEPIO_0000197, rdf:type) was not checked.
10:55:08.632 [main] WARN  org.renci.shacli.ShacliApp$ - Resource SEPI:0000224 (types: none; props: RDFS:label) was not checked.
10:55:08.632 [main] WARN  org.renci.shacli.ShacliApp$ - Resource http://dataexchange.clinicalgenome.org/CGEX_CtxNm9099 (types: http://dataexchange.clinicalgenome.org/CG_0020; props: http://dataexchange.clinicalgenome.org/CG_0007, http://dataexchange.clinicalgenome.org/CG_0006, rdf:type) was not checked.
10:55:08.632 [main] WARN  org.renci.shacli.ShacliApp$ - Resource http://purl.obolibrary.org/obo/SEPIO_0000191 (types: RDFS:Class; props: RDFS:subClassOf, rdf:type) was not checked.
10:55:08.632 [main] WARN  org.renci.shacli.ShacliApp$ - Resource http://purl.obolibrary.org/obo/SO_0001583 (types: none; props: RDFS:label) was not checked.
10:55:08.633 [main] WARN  org.renci.shacli.ShacliApp$ - 23 resources NOT checked.
10:55:08.633 [main] INFO  org.renci.shacli.ShacliApp$ - 1 resources checked.
CLASS <http://purl.obolibrary.org/obo/SEPIO_0000191> (2 errors)
Node http://dataexchange.clinicalgenome.org/CGEX_CritAssess156 (2 errors)
 - Path <http://www.w3.org/2000/01/rdf-schema#label> (1 errors)
   - [http://www.w3.org/ns/shacl#MinCountConstraintComponent] Property needs to have at least 1 values, but found 0

 - Path <http://purl.obolibrary.org/obo/SEPIO_0000197> (1 errors)
   - [http://www.w3.org/ns/shacl#NodeConstraintComponent] Value does not have shape cgshapes:CriterionAssessmentOutcomeValueSetShape (value: SEPI:0000224)



2 errors displayed
src/test/resources/clingen_data.jsonld FAILED VALIDATION
```
