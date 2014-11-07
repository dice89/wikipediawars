name := """World Wide Wikipedia Wars"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  javaWs,
  "com.maxmind.geoip2" % "geoip2" % "2.0.0",
  "org.apache.lucene" % "lucene-core" % "4.10.1",
  "com.typesafe.play.plugins" %% "play-plugins-redis" % "2.3.1",
  "org.jsoup" % "jsoup" % "1.8.1",
  "org.apache.lucene" % "lucene-core" % "4.10.1",
  "org.apache.lucene" % "lucene-analyzers-common" % "4.10.1"
)

resolvers ++= Seq(
  "pk11 repo" at "http://pk11-scratch.googlecode.com/svn/trunk"
)