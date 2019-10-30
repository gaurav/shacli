.PHONY: all
all: examples.ttl

examples.ttl: examples.nq
	rapper -i nquads -o turtle examples.nq > examples.ttl

examples.nt: examples.jsonld context.jsonld
	jsonld normalize examples.jsonld > examples.nt

examples.nq: examples.jsonld context.jsonld
	jsonld normalize -q examples.jsonld > examples.nq
