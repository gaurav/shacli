package org.renci.shacli

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
import org.rogach.scallop._
import org.rogach.scallop.exceptions._

import com.typesafe.scalalogging.{LazyLogging, Logger}

import scala.collection.JavaConverters._
import scala.collection.mutable

import org.renci.shacli.validator.Validator
import org.renci.shacli.generator.Generator

/**
  * Use the given shapes file to validate the given data file.
  * This reads the data file and so can provide better error messages that
  * TopBraid's SHACL engine.
  */
object ShacliApp extends App with LazyLogging {

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
    val validate = new Subcommand("validate") {
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
    }
    addSubcommand(validate)

    // If you run `[shacli] generate [data files]`, we will attempt to generate SHACL
    // that represents the presented content.
    val generate = new Subcommand("generate") {
      val data: ScallopOption[List[File]] = trailArg[List[File]](descr = "Data file(s) to validate (in Turtle)")
    }
    addSubcommand(generate)

    verify()
  }

  // Parse command line arguments.
  val conf = new Conf(args, logger)

  conf.subcommand match {
    case Some(conf.validate) => System.exit(Validator.validate(logger, conf))
    case Some(conf.generate) => System.exit(Generator.generate(logger, conf))
    case _ => throw new RuntimeException("Unknown subcommand: " + conf.subcommand)
  }
}
