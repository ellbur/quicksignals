
ThisBuild / scalaVersion := "3.0.0-M3"

lazy val root = project.in(file(".")).
  aggregate(cross.js, cross.jvm).
  settings(
    publish := {},
    publishLocal := {},
  )

lazy val cross = crossProject(JSPlatform, JVMPlatform).in(file(".")).
  settings(
    organization := "com.github.ellbur",
    name := "quicksignals",
    version := "0.1.2",
    scalaVersion := "3.0.0-M3",
    scalaSource in Test := baseDirectory.value / "../test",
    scalaSource in Compile := baseDirectory.value / "../src",
  ).
  jvmSettings(
  ).
  jsSettings(
  )

