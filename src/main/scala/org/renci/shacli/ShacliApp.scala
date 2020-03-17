package org.renci.shacli

import java.io.File

import java.io.{InputStream, File, ByteArrayOutputStream, StringWriter}

import org.topbraid.shacl.validation._
import org.apache.jena.ontology.OntModel
import org.apache.jena.ontology.OntModelSpec
import org.apache.jena.rdf.model.{Model, ModelFactory, Resource, RDFNode, RDFList}
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.util.FileUtils
import org.topbraid.jenax.util.SystemTriples
import org.topbraid.shacl.util.SHACLSystemModel
import org.topbraid.shacl.vocabulary.SH
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS
import org.rogach.scallop._
import org.rogach.scallop.exceptions._

import com.typesafe.scalalogging.{LazyLogging, Logger}

import scala.collection.JavaConverters._
import scala.collection.mutable

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
  * Command line configuration for Validate.
  */
class Conf(arguments: Seq[String], logger: Logger) extends ScallopConf(arguments) {
  override def onError(e: Throwable) = e match {
    case ScallopException(message) =>
      printHelp
      logger.error(message)
      System.exit(1)
    case ex => super.onError(ex)
  }

  val version = getClass.getPackage.getImplementationVersion
  version("SHACLI: A SHACLI CLI v" + version)
  val shapes: ScallopOption[File] = trailArg[File](descr = "Shapes file to validate (in Turtle)")
  val data: ScallopOption[List[File]]   = trailArg[List[File]](descr = "Data file(s) to validate (in Turtle)")
  val only: ScallopOption[List[String]] = opt[List[String]](
    default = Some(List()),
    descr = "Only display SourceConstraintComponent ending with these strings"
  )
  val ignore: ScallopOption[List[String]] = opt[List[String]](
    default = Some(List()),
    descr = "Don't display SourceConstraintComponent ending with these strings"
  )
  val displayNodes: ScallopOption[Boolean] =
    opt[Boolean](default = Some(false), descr = "Display all failing nodes as Turtle")
  verify()
}

/**
  * Use the given shapes file to validate the given data file.
  * This reads the data file and so can provide better error messages that
  * TopBraid's SHACL engine.
  */
object ShacliApp extends App with LazyLogging {
  // Parse command line arguments.
  val conf = new Conf(args, logger)

  val shapesFile: File                = conf.shapes()
  val dataFiles: List[File]           = conf.data()
  val onlyConstraints: List[String]   = conf.only()
  val ignoreConstraints: List[String] = conf.ignore()

  // Load the shapes.
  val shapesModel: Model = RDFDataMgr.loadModel(shapesFile.toString)

  // Load SHACL and Dash.
  val shaclTTL: InputStream = classOf[SHACLSystemModel].getResourceAsStream("/rdf/shacl.ttl")
  shapesModel.read(shaclTTL, SH.BASE_URI, FileUtils.langTurtle)

  val dashTTL: InputStream = classOf[SHACLSystemModel].getResourceAsStream("/rdf/dash.ttl")
  shapesModel.read(dashTTL, SH.BASE_URI, FileUtils.langTurtle)

  // Load system ontology.
  shapesModel.add(SystemTriples.getVocabularyModel())

  val shapesOntModel: OntModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, shapesModel)

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
      logger.info(s"Checking focus node ${rdfNode}")
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

            return if (dataModelQName != null) dataModelQName
            else if (shapesModelQName != null) shapesModelQName
            else node.getURI
          })
          .mkString(", ")
    }

    if (!resourcesNotChecked.isEmpty) {
      resourcesNotChecked.foreach(rdfNode => {
        val types =
          rdfNode.asResource.listProperties(RDF.`type`).toList.asScala.map(_.getResource).toSeq
        val props = rdfNode.asResource.listProperties.toList.asScala.map(_.getPredicate).toSeq
        logger.warn(
          s"Resource ${rdfNode} (types: ${getShortenedURIs(types)}; props: ${getShortenedURIs(props)}) was not checked."
        )
      })
      logger.warn(f"${resourcesNotChecked.size}%,d resources NOT checked.")
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
                  if (conf.displayNodes()) {
                    // Display focusNode as Turtle.
                    val focusNodeModel =
                      focusNode.inModel(dataModel).asResource.listProperties.toModel
                    focusNodeModel
                      .setNsPrefixes(Map("SEPIO" -> "http://purl.obolibrary.org/obo/SEPIO_").asJava)

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
    System.exit(0);
  else
    System.exit(1);
}
