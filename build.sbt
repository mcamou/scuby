import de.johoop.jacoco4sbt._
import JacocoPlugin._
import xerial.sbt.Sonatype.SonatypeKeys
import SonatypeKeys._

val jrubyVersion = "1.7.21"
val specs2Version = "2.3.13"

name := "Scuby"

version := "0.2.5"

isSnapshot := false

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.10.4", "2.11.7")

resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository"

libraryDependencies ++= Seq("org.jruby" % "jruby" % jrubyVersion,
                            "org.specs2" %% "specs2-matcher" % specs2Version % "test",
                            "org.specs2" %% "specs2-matcher-extra" % specs2Version % "test",
                            "org.specs2" %% "specs2-scalacheck" % specs2Version % "test")

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-Yinline-warnings")

net.virtualvoid.sbt.graph.Plugin.graphSettings

// https://github.com/sbt/jacoco4sbt/wiki
jacoco.settings

// Sonatype Maven repo settings
// See https://github.com/xerial/sbt-sonatype

xerial.sbt.Sonatype.sonatypeSettings

organization := "com.tecnoguru"

profileName := "com.tecnoguru"

pomExtra := <url>http://github.com/mcamou/scuby</url>
    <licenses>
      <license>
        <name>BSD</name>
        <url>http://www.opensource.org/licenses/bsd-license.php</url>
      </license>
    </licenses>
    <scm>
      <url>git://github.com/mcamou/scuby.git</url>
      <connection>scm:git:git@github.com:mcamou/scuby</connection>
      <developerConnection>scm:git:git@github.com:mcamou/scuby</developerConnection>
    </scm>
    <inceptionYear>2009</inceptionYear>
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

// To publish all cross-compiled versions, use "+ dist"
addCommandAlias("dist", "; publishSigned; sonatypeRelease")

