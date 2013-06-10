name := "GUIFlitz"

version := "0.0.1-SNAPSHOT"

organization := "de.sciss"

scalaVersion := "2.10.2"

description := "Automatic GUI from case classes for rapid prototyping"

homepage <<= name { n => Some(url("https://github.com/Sciss/" + n)) }

licenses := Seq("GPL v2+" -> url("http://www.gnu.org/licenses/gpl-2.0.txt"))

initialCommands in console := 
  """import de.sciss.swingplus._
    |import scala.swing._
    |import de.sciss.guiflitz._""".stripMargin

libraryDependencies <+= scalaVersion { sv =>
  "org.scala-lang" % "scala-reflect" % sv
}

libraryDependencies ++= Seq(
  "de.sciss" %% "swingplus" % "0.0.+",
  "de.sciss" %% "model"     % "0.3.+"
)

retrieveManaged := true

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

// ---- publishing ----

publishMavenStyle := true

publishTo <<= version { v =>
  Some(if (v endsWith "-SNAPSHOT")
    "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  else
    "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
  )
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra <<= name { n =>
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
