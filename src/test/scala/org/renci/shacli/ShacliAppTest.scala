package org.renci.shacli

import java.io.{File, FileWriter, PrintWriter}
import java.time.ZonedDateTime
import java.util.Calendar

import java.io.{File, FileInputStream, IOException, InputStream, ByteArrayOutputStream, StringWriter}

import org.topbraid.shacl.validation._
import org.apache.jena.ontology.OntDocumentManager
import org.apache.jena.ontology.OntModel
import org.apache.jena.ontology.OntModelSpec
import org.apache.jena.rdf.model.{Model, ModelFactory, Resource, RDFNode, RDFList, SimpleSelector}
import org.apache.jena.util.FileUtils
import org.topbraid.jenax.util.JenaUtil
import org.topbraid.jenax.util.SystemTriples
import org.topbraid.shacl.compact.SHACLC
import org.topbraid.shacl.util.SHACLSystemModel
import org.topbraid.shacl.vocabulary.DASH
import org.topbraid.shacl.vocabulary.SH
import org.topbraid.shacl.vocabulary.TOSH
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS
import org.rogach.scallop._

import com.typesafe.scalalogging.LazyLogging
import com.github.tototoshi.csv.CSVReader
import scala.collection.JavaConverters;

import utest._

/**
 * Test whether Shacli works as an executable on particular inputs.
 */

object ShacliAppTest extends TestSuite {
  val tests = Tests {
    test("Whether Shacli can be started") {
      ShacliApp.main(Array("--help"))
    }
  }
}
