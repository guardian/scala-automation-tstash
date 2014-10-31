import play.PlayScala

name := """scala-automation-tstash"""

version := "1.0"

scalaVersion := "2.11.1"

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "reactivemongo" % "0.10.5.0.akka23",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.0.akka23",
  "com.sksamuel.scrimage" %% "scrimage-core" % "1.4.1"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)
