// Your profile name of the sonatype account. The default is the same with the organization value
sonatypeProfileName := "org.shacli"

// To sync with Maven central, you need to supply the following information:
publishMavenStyle := true

// Open-source license of your choice
licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT"))

// Where is the source code hosted: GitHub or GitLab?
import xerial.sbt.Sonatype._
sonatypeProjectHosting := Some(GitHubHosting("gaurav", "shacli", "gaurav@ggvaidya.com"))

// or if you want to set these fields manually
homepage := Some(url("https://shacli.org"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/gaurav/shacli"),
    "scm:git@github.com:gaurav/shacli.git"
  )
)
developers := List(
  Developer(id="gaurav", name="Gaurav Vaidya", email="gaurav@ggvaidya.com", url=url("http://ggvaidya.com"))
)
