package org.renci.spec2shacl

import java.io.{File, FileWriter, PrintWriter}
import java.time.ZonedDateTime
import java.util.Calendar

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.topbraid.shacl.validation._
import org.apache.jena.ontology.OntDocumentManager
import org.apache.jena.ontology.OntModel
import org.apache.jena.ontology.OntModelSpec
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.util.FileUtils
import org.topbraid.jenax.util.JenaUtil
import org.topbraid.jenax.util.SystemTriples
import org.topbraid.shacl.compact.SHACLC
import org.topbraid.shacl.util.SHACLSystemModel
import org.topbraid.shacl.vocabulary.DASH
import org.topbraid.shacl.vocabulary.SH
import org.topbraid.shacl.vocabulary.TOSH

import com.typesafe.scalalogging.LazyLogging
import com.github.tototoshi.csv.CSVReader
import scala.collection.JavaConverters;

/**
 * Better validation errors for SHACL
 */

object ValidationErrorPrinter {
  def print(report: ValidationReport): Unit = {
    val results = JavaConverters.asScalaBuffer(report.results).toSeq

    // 1. Group results by target node.
    val resultsByFocusNode = results.groupBy(_.getFocusNode)
    resultsByFocusNode.foreach({ case (focusNode, results) =>
      println(s"Focus node has ${results.size} errors: $focusNode")
      results.foreach({result =>
        println(s" - ${result.getPath}: ${result.getMessage}")
      })
      println()
    })

    println(s"FAIL ${results.size} failures across ${resultsByFocusNode.keys.size} focus nodes.")
  }
}

/**
 * Use the given shapes file to validate the given data file.
 * This reads the data file and so can provide better error messages that
 * TopBraid's SHACL engine.
 */

object Validate extends App with LazyLogging {
  val shapesFile = new File(args(0))
  val dataFile = new File(args(1))

  // Set up the base model.
  val dm = new OntDocumentManager()
  val spec = new OntModelSpec(OntModelSpec.OWL_MEM)

  // Load SHACL.
  val shaclTTL = classOf[SHACLSystemModel].getResourceAsStream("/rdf/shacl.ttl")
  val shacl = JenaUtil.createMemoryModel()
  shacl.read(shaclTTL, SH.BASE_URI, FileUtils.langTurtle)
  shacl.add(SystemTriples.getVocabularyModel())
  dm.addModel(SH.BASE_URI, shacl)

  spec.setDocumentManager(dm);

  // Load the shapes.
  val shapesModel = ModelFactory.createOntologyModel(spec)
  shapesModel.read(new FileInputStream(shapesFile), "urn:x:base", FileUtils.langTurtle)

  // Load the data model.
  val dataModel = ModelFactory.createOntologyModel(spec)
  dataModel.read(new FileInputStream(dataFile), "urn:x:base", FileUtils.langTurtle)

  // Create a validation engine.
	val engine = ValidationUtil.createValidationEngine(dataModel, shapesModel, true);
  engine.validateAll()
  val report = engine.getValidationReport

  if (report.conforms)
    println("OK")
  else
    ValidationErrorPrinter.print(report)
}
