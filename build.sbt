import sbt.Keys.libraryDependencies

name := """play-scala-starter-example"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

resolvers += Resolver.sonatypeRepo("snapshots")

scalaVersion := "2.12.4"

crossScalaVersions := Seq("2.11.12", "2.12.4")

libraryDependencies += guice
libraryDependencies += ehcache
libraryDependencies += cacheApi
libraryDependencies += jcache
libraryDependencies += ws
libraryDependencies += "com.typesafe.play" %% "play-slick" % "3.0.3"
libraryDependencies += "com.typesafe.play" %% "play-slick-evolutions" % "3.0.3"
libraryDependencies += "com.h2database" % "h2" % "1.4.196"
libraryDependencies += "org.jsr107.ri" % "cache-annotations-ri-guice" % "1.0.0"
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.5.11" % Test
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test
