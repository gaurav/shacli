package org.renci.shacli.generator

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

import org.renci.shacli.ShacliApp

/**
 * Generate SHACL based on Turtle provided.
 */
object Generator {
  def generate(logger: Logger, conf: ShacliApp.Conf): Int = {
    println("Generate with conf: " + conf)
    return 0
  }
}
