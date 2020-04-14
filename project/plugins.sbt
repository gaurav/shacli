// Code formatting and linting tools.
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.0.1")
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "2.4.3")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.9.11")

// Publishing to Sonotype OSSRH.
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.9.2")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.0")
