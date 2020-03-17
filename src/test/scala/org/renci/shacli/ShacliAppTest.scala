package org.renci.shacli

import java.io.File

import java.io.File

import sys.process._;

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

  val tests: Tests = Tests {
    test("Whether Shacli can be started with '--help'") {
      val res = exec(Seq("sbt", "run --help"))
      assert(res.exitCode == 0)
      assert(res.stdout contains "[success] Total time")
      assert(res.stdout contains "SHACLI: A SHACLI CLI")
      assert(res.stdout contains "-h, --help")
      assert(res.stdout contains "Show help message")
    }

    test("Whether Shacli validates a Turtle file as expected") {
      val test1shapes = new File(getClass.getResource("/test1_shapes.ttl").toURI)
      val test1data   = new File(getClass.getResource("/test1_data.ttl").toURI)

      val res = exec(Seq("sbt", s"run validate $test1shapes $test1data"))
      assert(res.exitCode == 1)
      assert(res.stdout contains "Starting validation of")
      assert(res.stdout contains "Node http://example.org/Shadow (1 errors)")
      assert(res.stdout contains "Node http://example.org/Buck (1 errors)")
      assert(
        res.stdout contains "- [http://www.w3.org/ns/shacl#MaxCountConstraintComponent] Property may only have 1 value, but found 2"
      )
      assert(res.stdout contains "[info] 2 errors displayed")
      assert(res.stdout contains "Nonzero exit code returned from runner: 1")

      // Make sure we detect that the list in Buck foaf:depiction is broken.
      assert(res.stdout contains "[http://www.w3.org/ns/shacl#NodeConstraintComponent] Value does not have shape dash:ListShape")

      // Additionally, we should provide a warning: there is a resource in the
      // Turtle file (example:CheshireCat) that was not validated.
      assert(res.stdout contains "2 resources checked.")
      assert(res.stdout contains "3 resources NOT checked.")
      assert(
        res.stdout contains "Resource http://example.org/CheshireCat (types: none; props: foaf:depiction, foaf:age, foaf:name, foaf:name) was not checked."
      )
    }

    test("Whether Shacli validates a JSON-LD file as expected") {
      val test1shapes = new File(getClass.getResource("/test1_shapes.ttl").toURI)
      val test1data   = new File(getClass.getResource("/test1_data.jsonld").toURI)

      val res = exec(Seq("sbt", s"run validate $test1shapes $test1data"))
      assert(res.exitCode == 1)
      assert(res.stdout contains "Starting validation of")
      assert(res.stdout contains "Node http://example.org/Shadow (1 errors)")
      assert(
        res.stdout contains "- [http://www.w3.org/ns/shacl#MaxCountConstraintComponent] Property may only have 1 value, but found 2"
      )
      assert(res.stdout contains "[info] 1 errors displayed")
      assert(res.stdout contains "Nonzero exit code returned from runner: 1")
    }

    test("Whether Shacli validates two files as expected") {
      val test1shapes      = new File(getClass.getResource("/test1_shapes.ttl").toURI)
      val test1data_ttl    = new File(getClass.getResource("/test1_data.ttl").toURI)
      val test1data_jsonld = new File(getClass.getResource("/test1_data.jsonld").toURI)

      val res = exec(Seq("sbt", s"run validate $test1shapes $test1data_ttl $test1data_jsonld"))
      assert(res.exitCode == 1)
      assert(res.stdout contains "Starting validation of")
      assert(res.stdout contains "Node http://example.org/Shadow (1 errors)")

      // Make sure we detect that the list in Buck foaf:depiction is broken.
      assert(res.stdout contains "Node http://example.org/Buck (1 errors)")
      assert(res.stdout contains "[http://www.w3.org/ns/shacl#NodeConstraintComponent] Value does not have shape dash:ListShape")

      assert(
        res.stdout contains "- [http://www.w3.org/ns/shacl#MaxCountConstraintComponent] Property may only have 1 value, but found 2"
      )
      assert(res.stdout contains "[info] 2 errors displayed")
      assert(res.stdout contains "Nonzero exit code returned from runner: 1")
      assert(res.stdout contains "test1_data.ttl FAILED VALIDATION")
      assert(res.stdout contains "test1_data.jsonld FAILED VALIDATION")
    }
  }
}
