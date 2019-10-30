.PHONY: all clean test
all: examples.ttl

clean:
	rm examples-graph.jsonld examples.nq examples.ttl

test: examples.ttl
	pyshacl -s shapes/CriterionAssessment.ttl examples.ttl

examples-graph.jsonld: examples.json context.jsonld
	# Create a JSON-LD file from the JSON file generated
	# by ../reformat_examples.rb.
	# The JSON file is in the structure {"term": {"id": "term", ...}, ...}
	# However, to be correctly processed, they need to be in the format:
	#   { "@context": "...", "@graph": [ {"id": "term", ...}, ...] }
	# We can use jq and echo to rewrite the file into this format.
	echo '{\n"@context": "./context.jsonld", ' > examples-graph.jsonld
	echo '"@graph":' >> examples-graph.jsonld
	jq '[to_entries[].value]' examples.json >> examples-graph.jsonld
	echo '}' >> examples-graph.jsonld

examples.nq: examples-graph.jsonld
	# Convert JSON-LD file into n-quads.
	jsonld normalize -q examples-graph.jsonld > examples.nq

examples.ttl: examples.nq
	# Convert n-quads into Turtle.
	rapper -i nquads -o turtle examples.nq > examples.ttl
