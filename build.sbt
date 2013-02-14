name := "Movie JSON"

version := "0.0.1"

organization := "com.dmp"

scalaVersion := "2.9.2"

scalariformSettings

org.scalastyle.sbt.ScalastylePlugin.Settings

resolvers ++= Seq("snapshots"     at "http://oss.sonatype.org/content/repositories/snapshots",
                "releases"        at "http://oss.sonatype.org/content/repositories/releases"
                )

scalacOptions ++= Seq("-deprecation", "-unchecked")

libraryDependencies ++= {
  val liftVersion = "2.5-M4"
  Seq(
    "net.databinder.dispatch" %% "dispatch-core"   % "0.9.5",
    "com.typesafe"             % "config"          % "0.4.1",
    "net.liftweb"             %% "lift-json"       % liftVersion,
    "com.weiglewilczek.slf4s" % "slf4s_2.9.1" % "1.0.7", 
    "com.typesafe" % "config" % "0.4.1",
    "ch.qos.logback" % "logback-classic" % "0.9.28"
    )
}

