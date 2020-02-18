package org.renci.shacli

import java.io.{File, FileWriter, PrintWriter}
import java.time.ZonedDateTime
import java.util.Calendar

import java.io.{File, FileInputStream, IOException, InputStream, ByteArrayOutputStream, StringWriter}

import sys.process._

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
  /** Encapsulates the response from exec() */
  case class ExecResult(exitCode: Int, stdout: String, stderr: String)

  /**
   * Execute programs from the command line.
   * Returns the tuple: (exitCode, )
  */
  def exec(args: Seq[String]): ExecResult = {
    val stdout = new StringBuilder
    val stderr = new StringBuilder

    val status = args ! ProcessLogger(stdout append _, stderr append _)
    ExecResult(status, stdout.toString, stderr.toString)
  }

  val tests = Tests {
    test("Whether Shacli can be started with '--help'") {
      val res = exec(Seq("sbt", "run --help"))
      assert(res.exitCode == 0)
      assert(res.stdout contains "[success] Total time")
      assert(res.stdout contains "SHACLI: A SHACLI CLI")
      assert(res.stdout contains "-h, --help")
      assert(res.stdout contains "Show help message")
    }

    test("Whether Shacli validates a file as expected") {
      val test1shapes = new File(getClass.getResource("/test1_shapes.ttl").toURI)
      val test1data = new File(getClass.getResource("/test1_data.ttl").toURI)

      val res = exec(Seq("sbt", s"run $test1shapes $test1data"))
      assert(res.exitCode == 1)
      assert(res.stdout contains "Node http://example.org/Shadow (1 errors)")
      assert(res.stdout contains "- [http://www.w3.org/ns/shacl#MaxCountConstraintComponent] Property may only have 1 value, but found 2")
      assert(res.stdout contains "[info] 1 errors displayed")
      assert(res.stdout contains "Nonzero exit code returned from runner: 1")
    }
  }
}
