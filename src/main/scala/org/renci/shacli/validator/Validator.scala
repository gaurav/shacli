package org.renci.shacli.validator

import scala.language.reflectiveCalls
import scala.collection.JavaConverters._
import scala.collection.mutable

import java.io.File
import java.io.{InputStream, File, ByteArrayOutputStream, StringWriter}

import org.topbraid.shacl.validation._
import org.apache.jena.ontology.{OntModel, OntModelSpec}
import org.apache.jena.rdf.model.{Model, ModelFactory, Resource, RDFNode, RDFList}
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.util.FileUtils
import org.topbraid.jenax.util.SystemTriples
import org.topbraid.shacl.util.SHACLSystemModel
import org.topbraid.shacl.vocabulary.SH
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS
import com.typesafe.scalalogging.Logger

import org.renci.shacli.ShacliApp

/*
 * Better validation errors for SHACL
 */

case class ValidationError(
  report: ValidationReport,
  result: ValidationResult,
  classNode: RDFNode,
  focusNode: RDFNode,
  path: String,
  sourceConstraintComponent: Resource,
  message: String,
  value: Option[RDFNode]
)

object ValidationErrorGenerator {
  def generate(
    report: ValidationReport,
    shapesModel: OntModel,
    dataModel: Model
  ): Seq[ValidationError] = {
    val results = report.results.asScala.toSeq

    // 1. Group results by source shape.
    val resultsByClass = results.groupBy({ result =>
      val focusNode = result.getFocusNode
      val statement = dataModel.getProperty(focusNode.asResource, RDF.`type`)

      if (statement == null) RDFS.Class
      else statement.getObject
    })
    resultsByClass.toSeq
      .sortBy(_._2.size)
      .flatMap({
        case (classNode, classResults) =>
          val resultsByFocusNode = classResults.groupBy(_.getFocusNode)
          resultsByFocusNode.toSeq
            .sortBy(_._2.size)
            .flatMap({
              case (focusNode, focusNodeResults) =>
                val resultsByPath = focusNodeResults.groupBy(_.getPath)
                resultsByPath.toSeq
                  .sortBy(_._2.size)
                  .flatMap({
                    case (pathNode, pathNodeResults) =>
                      pathNodeResults.map(result => {
                        ValidationError(
                          report,
                          result,
                          classNode,
                          focusNode,
                          summarizeResource(pathNode),
                          result.getSourceConstraintComponent,
                          result.getMessage,
                          if (result.getValue == null) None else Some(result.getValue)
                        )
                      })
                  })
            })
      })
  }

  def summarizeResource(node: RDFNode): String = {
    if (node == null) {
      "(null)"
    } else if (node.canAs(classOf[RDFList])) {
      val list: RDFList = node.as(classOf[RDFList])
      list.asJavaList // Convert the JavaList into a java.util.List<RDFNode>,
      .asScala        // which we then convert into a Scala Buffer, and then
      .toSeq          // to a Seq.
        .map(summarizeResource(_))
        .mkString(", ")
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
  * Validator
  */
object Validator {
  def validate(logger: Logger, conf: ShacliApp.Conf): Int = {
    val shapesFile: File                = conf.validate.shapes()
    val dataFiles: List[File]           = conf.validate.data()
    val onlyConstraints: List[String]   = conf.validate.only()
    val ignoreConstraints: List[String] = conf.validate.ignore()

    // Load the shapes.
    val shapesModel: Model = RDFDataMgr.loadModel(shapesFile.toString)

    // Load SHACL and Dash.
    val toshTTL: InputStream = classOf[SHACLSystemModel].getResourceAsStream("/rdf/tosh.ttl")
    shapesModel.read(toshTTL, SH.BASE_URI, FileUtils.langTurtle)
    shapesModel.add(SHACLSystemModel.getSHACLModel)

    // Load system ontology.
    shapesModel.add(SystemTriples.getVocabularyModel())

    val shapesOntModel: OntModel =
      ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, shapesModel)

    def checkDataFile(dataFile: File): Boolean = {
      logger.info(s"Starting validation of $dataFile against $shapesFile.")

      // Load the data model.
      val dataModel: Model                = RDFDataMgr.loadModel(dataFile.toString);
      val resourcesToCheck: Seq[Resource] = dataModel.listSubjects.toList.asScala
      logger.debug(s"Resources to check: ${resourcesToCheck}")

      // Create a validation engine.
      val config: ValidationEngineConfiguration = new ValidationEngineConfiguration()
        .setReportDetails(true)
        .setValidateShapes(true)

      val engine: ValidationEngine =
        ValidationUtil.createValidationEngine(dataModel, shapesOntModel, config)

      // Track progress.
      // TODO: Implement a progress monitor so we can track long-running jobs.
      // engine.setProgressMonitor(new SimpleProgressMonitor("Progress"))

      // Count off validated nodes.
      val resourcesCheckedSet: mutable.Set[RDFNode] = mutable.Set()
      engine.setFocusNodeFilter(rdfNode => {
        logger.debug(s"Checking focus node ${rdfNode}")
        resourcesCheckedSet.add(rdfNode)
        true
      })

      engine.validateAll()
      val report = engine.getValidationReport

      // Report on any nodes that were not checked.
      val resourcesChecked: Set[RDFNode] = resourcesCheckedSet.toSet
      val resourcesNotChecked: Seq[Resource] =
        resourcesToCheck.filter(rdfNode => !resourcesChecked.contains(rdfNode))

      /** Summarize a set of URIs as a string. */
      def getShortenedURIs(nodes: Seq[Resource]): String = {
        if (nodes.isEmpty) return "none"
        else
          return nodes
            .map(node => {
              // Try to use either the data model or the shape model to shorten URLs.
              val dataModelQName   = dataModel.qnameFor(node.getURI)
              val shapesModelQName = shapesModel.qnameFor(node.getURI)

              if (dataModelQName != null) dataModelQName
              else if (shapesModelQName != null) shapesModelQName
              else node.getURI
            })
            .mkString(", ")
      }

      if (!resourcesNotChecked.isEmpty) {
        val filteredResourcesNotChecked = resourcesNotChecked
          .filter(rdfNode => {
            // Filter out any RDF Nodes that appear to be well-formed rdf:List.
            // This means they should have *both* rdf:first and rdf:rest.
            val props = rdfNode.asResource.listProperties.toList.asScala.map(_.getPredicate).toSet

            if (props.size == 2 &&
                (props contains RDF.first) &&
                (props contains RDF.rest)) false
            else true
          })

        filteredResourcesNotChecked
          .foreach(rdfNode => {
            val types =
              rdfNode.asResource.listProperties(RDF.`type`).toList.asScala.map(_.getResource).toSeq
            val props = rdfNode.asResource.listProperties.toList.asScala.map(_.getPredicate).toSeq
            logger.warn(
              s"Resource ${rdfNode} (types: ${getShortenedURIs(types)}; props: ${getShortenedURIs(props)}) was not checked."
            )
          })
        logger.warn(f"${filteredResourcesNotChecked.size}%,d resources NOT checked.")
      }
      logger.info(f"${resourcesChecked.size}%,d resources checked.")

      if (report.conforms) {
        println(dataFile + " OK")
        return true;
      } else {
        val errors = ValidationErrorGenerator.generate(report, shapesOntModel, dataModel)

        def stringEndsWithOneOf(str: String, oneOf: Seq[String]): Boolean =
          oneOf.exists(str.endsWith(_))

        val filteredErrors = errors
          .filter(
            err =>
              onlyConstraints.isEmpty || stringEndsWithOneOf(
                err.sourceConstraintComponent.toString,
                onlyConstraints
              )
          )
          .filter(
            err =>
              ignoreConstraints.isEmpty || !stringEndsWithOneOf(
                err.sourceConstraintComponent.toString,
                ignoreConstraints
              )
          )

        filteredErrors
          .groupBy(_.classNode)
          .foreach({
            case (classNode, classErrors) =>
              // TODO: look up the classNode label
              println(s"CLASS <${classNode}> (${classErrors.length} errors)")
              classErrors
                .groupBy(_.focusNode)
                .foreach({
                  case (focusNode, focusErrors) =>
                    println(s"Node ${focusNode} (${focusErrors.length} errors)")
                    focusErrors
                      .groupBy(_.path)
                      .foreach({
                        case (path, pathErrors) =>
                          println(s" - Path <${path}> (${pathErrors.length} errors)")
                          pathErrors.foreach(error => {
                            println(
                              s"   - [${error.sourceConstraintComponent}] ${error.message} ${error.value
                                .map(value => s"(value: $value)")
                                .mkString(", ")}"
                            )
                          })
                          println()
                      })
                    println()
                    if (conf.validate.displayNodes()) {
                      // Display focusNode as Turtle.
                      val focusNodeModel =
                        focusNode.inModel(dataModel).asResource.listProperties.toModel
                      focusNodeModel
                        .setNsPrefixes(
                          Map("SEPIO" -> "http://purl.obolibrary.org/obo/SEPIO_").asJava
                        )

                      val stringWriter = new StringWriter
                      focusNodeModel.write(stringWriter, "Turtle")
                      println(s"Focus node model:\n${stringWriter.toString}")
                    }
                })
          })

        println()
        println(s"${filteredErrors.length} errors displayed")

        val ignoredErrors = errors diff filteredErrors
        if (!ignoredErrors.isEmpty) {
          println(s"${ignoredErrors.length} errors ignored because:")
          onlyConstraints.foreach(
            only => println(s" - Only displaying sourceConstraintComponents ending in '$only'")
          )
          ignoreConstraints.foreach(
            ignored => println(s" - Ignoring sourceConstraintComponents ending in '$ignored'")
          )
        }

        println(dataFile + " FAILED VALIDATION")
        return false;
      }
    }

    if (dataFiles.map(checkDataFile(_)).forall(identity))
      return 0
    else
      return 1
  }
}
