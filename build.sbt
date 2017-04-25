name               := "GUIFlitz"
version            := "0.5.1"
organization       := "de.sciss"
scalaVersion       := "2.12.2"
crossScalaVersions := Seq("2.12.2", "2.11.11", "2.10.6")
description        := "Automatic GUI from case classes for rapid prototyping"
homepage           := Some(url("https://github.com/Sciss/" + name.value))
licenses           := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt"))

// ---- dependencies ----

lazy val swingPlusVersion = "0.2.2"
lazy val modelVersion     = "0.3.3"

initialCommands in console := 
  """import de.sciss.swingplus._
    |import scala.swing._
    |import de.sciss.guiflitz._""".stripMargin

libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value

libraryDependencies ++= Seq(
  "de.sciss" %% "swingplus" % swingPlusVersion,
  "de.sciss" %% "model"     % modelVersion
)

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xfuture")

// ---- publishing ----

publishMavenStyle := true

publishTo :=
  Some(if (isSnapshot.value)
    "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  else
    "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
  )

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := { val n = name.value
<scm>
  <url>git@github.com:Sciss/{n}.git</url>
  <connection>scm:git:git@github.com:Sciss/{n}.git</connection>
</scm>
<developers>
  <developer>
    <id>sciss</id>
    <name>Hanns Holger Rutz</name>
    <url>http://www.sciss.de</url>
  </developer>
</developers>
}
