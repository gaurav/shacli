package org.renci.spec2shacl

import java.io.{File, FileWriter, PrintWriter}
import java.time.ZonedDateTime
import java.util.Calendar

import java.io.{File, FileInputStream, IOException, InputStream, ByteArrayOutputStream}

import org.topbraid.shacl.validation._
import org.apache.jena.ontology.OntDocumentManager
import org.apache.jena.ontology.OntModel
import org.apache.jena.ontology.OntModelSpec
import org.apache.jena.rdf.model.{Model, ModelFactory, Resource, RDFNode, RDFList}
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

import com.typesafe.scalalogging.LazyLogging
import com.github.tototoshi.csv.CSVReader
import scala.collection.JavaConverters;

/**
 * Better validation errors for SHACL
 */

object ValidationErrorPrinter {
  class ValidationResultWrapper(
    shapesModel: OntModel,
    dataModel: OntModel,
    result: ValidationResult
  ) {
    val focusNode = result.getFocusNode
    val focusNodeClass = {
      val statement = dataModel.getProperty(focusNode.asResource, RDF.`type`)
      if (statement == null) RDFS.Class else statement.getObject
    }
    val path = result.getPath
    val value = result.getValue
    val message = result.getMessage

    val summarizedPath = summarizeResource(path)

    override def toString(): String = {
      if (value == null) message else s"${value}: ${message}"
    }
  }

  def wrapResults(report: ValidationReport, shapesModel: OntModel, dataModel: OntModel) =
    JavaConverters.asScalaBuffer(report.results).toSeq.map(new ValidationResultWrapper(shapesModel, dataModel, _))

  def print(results: Seq[ValidationResultWrapper]): Unit = {
    // 1. Group results by focus node class.
    val resultsByClass = results.groupBy(_.focusNodeClass)
    resultsByClass.toSeq.sortBy(_._2.size).foreach({ case (classNode, classResults) =>
      println(s"Class has ${classResults.size} errors: $classNode")

      // 2. Group results by target node.
      val resultsByFocusNode = classResults.groupBy(_.focusNode)
      resultsByFocusNode.toSeq.sortBy(_._2.size).foreach({ case (focusNode, focusNodeResults) =>
        println(s" - Focus node has ${focusNodeResults.size} errors: $focusNode")

        val resultsByPath = focusNodeResults.groupBy(_.path)
        resultsByPath.toSeq.sortBy(_._2.size).foreach({ case (pathNode, pathNodeResults) =>
          println(s"   - ${summarizeResource(pathNode)}")
          pathNodeResults.foreach(result => {
            println(s"     - ${result.toString}")
          })
        })
      })
      println()
    })

    println(s"FAIL ${results.size} failures across ${resultsByClass.keys.size} classes.")
  }

  def summarizeResource(node: RDFNode): String = {
    if (node.canAs(classOf[RDFList])) {
      val list: RDFList = node.as(classOf[RDFList])
      JavaConverters.asScalaBuffer(list.asJavaList).toSeq.map(summarizeResource(_)).mkString(", ")
    } else if (node.asNode.isBlank) {
      val byteArray = new ByteArrayOutputStream()
      node.asResource.listProperties.toModel.write(byteArray, "TURTLE") // Accepts "JSON-LD"!
      byteArray.toString.replaceAll("\n", " ").replaceAll("\t", " ").replaceAll("\\s+", " ")
    } else {
      node.toString
    }
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
    ValidationErrorPrinter.print(ValidationErrorPrinter.wrapResults(report, shapesModel, dataModel))
}
