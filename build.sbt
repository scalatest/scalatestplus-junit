name := "junit-4.12"

organization := "org.scalatestplus"

version := "3.2.0.0-M4"

homepage := Some(url("https://github.com/scalatest/scalatestplus-junit"))

licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

developers := List(
  Developer(
    "bvenners",
    "Bill Venners",
    "bill@artima.com",
    url("https://github.com/bvenners")
  ),
  Developer(
    "cheeseng",
    "Chua Chee Seng",
    "cheeseng@amaseng.com",
    url("https://github.com/cheeseng")
  )
)

crossScalaVersions := List(
  "2.10.7", "2.11.12", "2.12.10", "2.13.1",
  "0.22.0", "0.23.0", "0.24.0-RC1"
)

/** Add src/main/scala-{2|3} to Compile / unmanagedSourceDirectories */
Compile / unmanagedSourceDirectories ++= {
  val sourceDir = (Compile / sourceDirectory).value
  CrossVersion.partialVersion(scalaVersion.value).map {
    case (0 | 3, _) => sourceDir / "scala-3"
    case (n, _) => sourceDir / s"scala-$n"
  }
}

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest-core" % "3.2.0-M4",
  "org.scalatest" %% "scalatest-wordspec" % "3.2.0-M4" % "test",
  "org.scalatest" %% "scalatest-funspec" % "3.2.0-M4" % "test",
  "org.scalatest" %% "scalatest-funsuite" % "3.2.0-M4" % "test",
  "org.scalatest" %% "scalatest-shouldmatchers" % "3.2.0-M4" % "test",
  "junit" % "junit" % "4.12"
)
Test / scalacOptions ++= (if (isDotty.value) Seq("-language:implicitConversions") else Nil)

import scala.xml.{Node => XmlNode, NodeSeq => XmlNodeSeq, _}
import scala.xml.transform.{RewriteRule, RuleTransformer}

// skip dependency elements with a scope
pomPostProcess := { (node: XmlNode) =>
  new RuleTransformer(new RewriteRule {
    override def transform(node: XmlNode): XmlNodeSeq = node match {
      case e: Elem if e.label == "dependency"
          && e.child.exists(child => child.label == "scope") =>
        def txt(label: String): String = "\"" + e.child.filter(_.label == label).flatMap(_.text).mkString + "\""
        Comment(s""" scoped dependency ${txt("groupId")} % ${txt("artifactId")} % ${txt("version")} % ${txt("scope")} has been omitted """)
      case _ => node
    }
  }).transform(node).head
}

testOptions in Test :=
  Seq(
    Tests.Argument(TestFrameworks.ScalaTest,
      "-m", "org.scalatestplus.junit",
    ))

enablePlugins(SbtOsgi)

osgiSettings

OsgiKeys.exportPackage := Seq(
  "org.scalatestplus.junit.*"
)

OsgiKeys.importPackage := Seq(
  "org.scalatest.*",
  "org.scalactic.*", 
  "scala.*;version=\"$<range;[==,=+);$<replace;"+scalaBinaryVersion.value+";-;.>>\"",
  "*;resolution:=optional"
)

OsgiKeys.additionalHeaders:= Map(
  "Bundle-Name" -> "ScalaTestPlusJUnit",
  "Bundle-Description" -> "ScalaTest+JUnit is an open-source integration library between ScalaTest and JUnit for Scala projects.",
  "Bundle-DocURL" -> "http://www.scalatest.org/",
  "Bundle-Vendor" -> "Artima, Inc."
)

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  Some("publish-releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

pgpSecretRing := file((Path.userHome / ".gnupg" / "secring.gpg").getAbsolutePath)

pgpPassphrase := None
