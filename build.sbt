// Publication information
name := "Shacli"
ThisBuild / organization := "org.shacli"
ThisBuild / version      := "0.1-SNAPSHOT"

// Code license
licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT"))

// Scalac options.
scalaVersion := "2.12.10"
scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-Ywarn-unused", "-feature")

addCompilerPlugin(scalafixSemanticdb)
scalacOptions in Test ++= Seq("-Yrangepos")

// Set up the main class.
mainClass in (Compile, run) := Some("org.renci.shacli.ShacliApp")

// Fork when running.
fork in run := true

// Set up testing.
testFrameworks += new TestFramework("utest.runner.Framework")

// Code formatting and linting tools.
wartremoverWarnings ++= Warts.unsafe

addCommandAlias(
  "scalafixCheckAll",
  "; compile:scalafix --check ; test:scalafix --check"
)

// Publish to Sonotype OSSRH.
publishTo := sonatypePublishToBundle.value

// Library dependencies.
libraryDependencies ++= {
  Seq(
    // Logging
    "com.typesafe.scala-logging"  %% "scala-logging"          % "3.9.2",
    "ch.qos.logback"              %  "logback-classic"        % "1.2.3",

    // Command line argument parsing.
    "org.rogach"                  %% "scallop"                % "3.3.2",

    // Import Apache Jena to read JSON-LD.
    "org.apache.jena"             % "jena-core"               % "3.14.0",

    // https://mvnrepository.com/artifact/org.apache.jena/jena-arq
    "org.apache.jena"             % "jena-arq"                % "3.14.0",

    // Import a SHACL library.
    "org.topbraid"                % "shacl"                   % "1.3.1",

    // Add support for CSV
    "com.github.tototoshi"        %% "scala-csv"              % "1.3.6",

    // Testing
    "com.lihaoyi"                 %% "utest"                  % "0.7.1" % "test"
  )
}
