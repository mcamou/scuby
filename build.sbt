import de.johoop.jacoco4sbt._
import JacocoPlugin._

val jrubyVersion = "1.7.9"
val specs2Version = "2.3.7"

name := "Scuby"

version := "0.2.2-SNAPSHOT"

isSnapshot := true

scalaVersion := "2.10.3"

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

libraryDependencies += "org.jruby" % "jruby" % jrubyVersion

// Here because of a dependency problem with JRuby 1.7.9 - probably remove with JRuby 1.7.10
libraryDependencies += "org.jruby.joni" % "joni" % "2.1.1"

libraryDependencies += "org.specs2" %% "specs2-core" % specs2Version % "test"

libraryDependencies += "org.specs2" %% "specs2-matcher" % specs2Version % "test"

libraryDependencies += "org.specs2" %% "specs2-matcher-extra" % specs2Version % "test"

libraryDependencies += "org.specs2" %% "specs2-scalacheck" % specs2Version % "test"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Yinline-warnings")

net.virtualvoid.sbt.graph.Plugin.graphSettings

// https://github.com/sbt/jacoco4sbt/wiki
jacoco.settings

// TODO Not working yet
// Sonatype Maven repo settings
// See http://www.scala-sbt.org/0.12.2/docs/Community/Using-Sonatype.html
publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

licenses := Seq("BSD-style" -> url("http://www.opensource.org/licenses/bsd-license.php"))

homepage := Some(url("http://github.com/mcamou/scuby"))

pomExtra := (
            <scm>
              <url>git://github.com/mcamou/scuby.git</url>
              <connection>scm:git:git@github.com:mcamou/scuby</connection>
              <developerConnection>scm:git:git@github.com:mcamou/scuby</developerConnection>
            </scm>
                <inceptionYear>2009</inceptionYear>
                <organization>
                  <name>tecnoguru.com</name>
                  <url>http://www.github.com/mcamou</url>
                </organization>
                <developers>
                  <developer>
                    <id>mcamou</id>
                    <name>Mario Camou</name>
                    <email>mcamou@tecnoguru.com</email>
                    <url>http://www.github.com/mcamou</url>
                    <roles>
                      <role>Original project developer</role>
                    </roles>
                    <timezone>+2</timezone>
                  </developer>
                </developers>
            )
