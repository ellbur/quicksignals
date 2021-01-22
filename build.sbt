
ThisBuild / scalaVersion := "3.0.0-M3"

lazy val root = project.in(file(".")).
  aggregate(cross.js, cross.jvm).
  settings(
    publish := {},
    publishLocal := {},
  )

lazy val cross = crossProject(JSPlatform, JVMPlatform).in(file(".")).
  settings(
    organization := "com.ellbur",
    name := "quicksignals",
    description := "A minimal Scala 3 FRP library",
    homepage := Some(url("https://github.com/ellbur/quicksignals")),
    version := "0.2.0",
    scalaVersion := "3.0.0-M3",
    scalaSource in Test := baseDirectory.value / "../test",
    scalaSource in Compile := baseDirectory.value / "../src",
  ).
  jvmSettings(
  ).
  jsSettings(
  )

