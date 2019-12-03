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
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS

import com.typesafe.scalalogging.LazyLogging
import com.github.tototoshi.csv.CSVReader
import scala.collection.JavaConverters;

/**
 * Better validation errors for SHACL
 */

object ValidationErrorPrinter {
  def print(report: ValidationReport, shapesModel: OntModel, dataModel: OntModel): Unit = {
    val results = JavaConverters.asScalaBuffer(report.results).toSeq

    // 1. Group results by source shape.
    val resultsByClass = results.groupBy({ result =>
      val focusNode = result.getFocusNode
      val statement = dataModel.getProperty(focusNode.asResource, RDF.`type`)

      if (statement == null) RDFS.Class
      else statement.getObject
    })
    resultsByClass.toSeq.sortBy(_._2.size).foreach({ case (classNode, classResults) =>
      println(s"Class has ${classResults.size} errors: $classNode")

      // 2. Group results by target node.
      val resultsByFocusNode = classResults.groupBy(_.getFocusNode)
      resultsByFocusNode.toSeq.sortBy(_._2.size).foreach({ case (focusNode, focusNodeResults) =>
        println(s" - Focus node has ${focusNodeResults.size} errors: $focusNode")

        val resultsByPath = focusNodeResults.groupBy(_.getPath)
        resultsByPath.toSeq.sortBy(_._2.size).foreach({ case (pathNode, pathNodeResults) =>
          println(s"   - ${pathNode}")
          pathNodeResults.foreach(result => {
            if (result.getValue == null)
              println(s"     - ${result.getMessage}")
            else
              println(s"     - ${result.getValue}: ${result.getMessage}")
          })
        })

        focusNodeResults.foreach({result =>

        })
      })
      println()
    })

    println(s"FAIL ${results.size} failures across ${resultsByClass.keys.size} classes.")
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
    ValidationErrorPrinter.print(report, shapesModel, dataModel)
}
