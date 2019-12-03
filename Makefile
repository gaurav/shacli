.PHONY: all clean test
all: test

# Clean up all generated files.
clean:
	rm shapes/shapes.ttl \
		 examples/graph.jsonld \
		 examples/examples.nq \
		 examples/examples.ttl

# Test whether all examples validate against the generated SHACL.
test: examples/examples.ttl shapes/shapes.ttl
	pyshacl -s shapes/shapes.ttl examples/examples.ttl

# Build the SHACL shapes from the specification downloaded from Google Docs.
shapes/shapes.ttl: src/main/scala/org/renci/spec2shacl/SpecToSHACL.scala
	sbt -warn 'run "data/DMWG - Interpretation Model" shapes/shapes.ttl'

# Generating the examples graph is a three step process:
#	- First, we generate graph.jsonld by creating a JSON-LD file from the input
#   JSON file.
# - Then, we convert this JSON-LD file into N-quads using `jsonld`.
# - Finally, we convert the N-quads into Turtle using `rapper`.

# Generate JSON-LD file from the input JSON file.
examples/graph.jsonld: examples/examples.json examples/context.jsonld
	# Create a JSON-LD file from the JSON file generated
	# by ../reformat_examples.rb.
	# The JSON file is in the structure {"term": {"id": "term", ...}, ...}
	# However, to be correctly processed, they need to be in the format:
	#   { "@context": "...", "@graph": [ {"id": "term", ...}, ...] }
	# We can use jq and echo to rewrite the file into this format.
	echo '{\n"@context": "./context.jsonld", ' > examples/graph.jsonld
	echo '"@graph":' >> examples/graph.jsonld
	jq '[to_entries[].value]' examples/examples.json >> examples/graph.jsonld
	echo '}' >> examples/graph.jsonld

# Generate the examples in N-quads file from the JSON-LD file.
examples/examples.nq: examples/graph.jsonld
	# Convert JSON-LD file into n-quads.
	jsonld normalize -q examples/graph.jsonld > examples/examples.nq

# Generate the examples in Turtle from the N-quads file.
examples/examples.ttl: examples/examples.nq
	# Convert n-quads into Turtle.
	rapper -i nquads -o turtle examples/examples.nq > examples/examples.ttl
