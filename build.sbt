import com.typesafe.sbt.SbtNativePackager._
import NativePackagerKeys._

packageArchetype.java_server

name := """World Wide Wikipedia Wars"""

version := "1.0"

packageDescription in Rpm := "My package Description"

rpmVendor in Rpm := "Alexander Mueller"

packageSummary in Rpm:= "test"

packageDescription := "A web application to monitor kafka web console"

rpmGroup := Some("Test")

rpmRelease := "1"

rpmVendor := "Gopal Patwa"

rpmUrl := Some("http://github.com/muuki88/sbt-native-packager")

rpmLicense := Some("Apache v2")



lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

javaOptions ++= Seq("-Xmx1g", "-Xmx4g", "-XX:MaxPermSize=4g")

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
  "org.apache.lucene" % "lucene-analyzers-common" % "4.10.1",
  "redis.clients" % "jedis" % "2.6.0",
  "mysql" % "mysql-connector-java" % "5.1.34"
)

resolvers ++= Seq(
  "pk11 repo" at "http://pk11-scratch.googlecode.com/svn/trunk"
)

