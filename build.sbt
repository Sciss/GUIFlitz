name               := "GUIFlitz"

version            := "0.3.2"

organization       := "de.sciss"

scalaVersion       := "2.11.0"

crossScalaVersions := Seq("2.11.0", "2.10.4")

description        := "Automatic GUI from case classes for rapid prototyping"

homepage           := Some(url("https://github.com/Sciss/" + name.value))

licenses           := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt"))

initialCommands in console := 
  """import de.sciss.swingplus._
    |import scala.swing._
    |import de.sciss.guiflitz._""".stripMargin

libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value

libraryDependencies ++= Seq(
  "de.sciss" %% "swingplus" % "0.1.2",
  "de.sciss" %% "model"     % "0.3.2"
)

// retrieveManaged := true

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Xfuture")

// ---- publishing ----

publishMavenStyle := true

publishTo :=
  Some(if (version.value endsWith "-SNAPSHOT")
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

// ---- ls.implicit.ly ----

seq(lsSettings :_*)

(LsKeys.tags   in LsKeys.lsync) := Seq("swing", "gui")

(LsKeys.ghUser in LsKeys.lsync) := Some("Sciss")

(LsKeys.ghRepo in LsKeys.lsync) := Some(name.value)

