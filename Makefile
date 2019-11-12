.PHONY: all clean test
all: test

clean:
	rm examples/graph.jsonld examples/examples.nq examples/examples.ttl

test: examples/examples.ttl shapes/shapes.ttl
	pyshacl -s shapes/shapes.ttl examples/examples.ttl

shapes/shapes.ttl: data/**
	sbt 'run "data/DMWG - Interpretation Model"' > shapes/shapes.ttl

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

examples/examples.nq: examples/graph.jsonld
	# Convert JSON-LD file into n-quads.
	jsonld normalize -q examples/graph.jsonld > examples/examples.nq

examples/examples.ttl: examples/examples.nq
	# Convert n-quads into Turtle.
	rapper -i nquads -o turtle examples/examples.nq > examples/examples.ttl
