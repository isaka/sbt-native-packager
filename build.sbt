name := "sbt-native-packager"
organization := "com.typesafe.sbt"
homepage := Some(url("https://github.com/sbt/sbt-native-packager"))

Global / onChangedBuildSource := ReloadOnSourceChanges
Global / scalaVersion := "2.12.12"

// crossBuildingSettings
crossSbtVersions := Vector("0.13.17", "1.1.6")

Compile / scalacOptions ++= Seq("-deprecation")
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

// put jdeb on the classpath for scripted tests
classpathTypes += "maven-plugin"
libraryDependencies ++= Seq(
  // these dependencies have to be explicitly added by the user
  "com.spotify" % "docker-client" % "8.14.3" % Provided,
  "org.vafer" % "jdeb" % "1.7" % Provided artifacts Artifact("jdeb", "jar", "jar"),
  "org.apache.commons" % "commons-compress" % "1.20",
  // for jdkpackager
  "org.apache.ant" % "ant" % "1.10.5",
  // workaround for the command line size limit
  "com.github.eldis" % "tool-launcher" % "0.2.2",
  "org.scalatest" %% "scalatest" % "3.0.5" % Test
)

// sbt dependent libraries
libraryDependencies ++= {
  (pluginCrossBuild / sbtVersion).value match {
    case v if v.startsWith("1.") =>
      Seq("org.scala-sbt" %% "io" % "1.2.2")
    case _ => Seq()
  }
}

// scala version depended libraries
libraryDependencies ++= {
  scalaBinaryVersion.value match {
    case "2.10" => Nil
    case _ =>
      Seq(
        "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.1",
        "org.scala-lang.modules" %% "scala-xml" % "1.1.1"
      )
  }
}

// configure github page
enablePlugins(SphinxPlugin, SiteScaladocPlugin, GhpagesPlugin, SbtPlugin)

git.remoteRepo := "git@github.com:sbt/sbt-native-packager.git"

// scripted test settings
scriptedLaunchOpts += "-Dproject.version=" + version.value

// binary compatibility settings
mimaPreviousArtifacts := {
  val m = organization.value %% moduleName.value % "1.3.15"
  val sbtBinV = (sbtBinaryVersion in pluginCrossBuild).value
  val scalaBinV = (scalaBinaryVersion in update).value
  Set(Defaults.sbtPluginExtra(m cross CrossVersion.disabled, sbtBinV, scalaBinV))
}

// Release configuration
publishMavenStyle := false

// The release task doesn't run any tests. We rely on travis.ci and appveyor,
// because it's impossible to run all tests (linux, macos, windows) on a single computer.
import ReleaseTransformations._
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  runTest,
  releaseStepCommandAndRemaining("^ publish"),
  updateReadme,
  commitReadme,
  pushChanges,
  generateReleaseChangelog,
  commitChangelog,
  pushChanges,
  releaseStepTask(ghpagesPushSite)
)

// bintray config
bintrayOrganization := Some("sbt")
bintrayRepository := "sbt-plugin-releases"

addCommandAlias("scalafmtFormatAll", "; ^scalafmtAll ; scalafmtSbt")
// ci commands
addCommandAlias("validateFormatting", "; scalafmtCheckAll ; scalafmtSbtCheck")
addCommandAlias("validate", "; clean ; update ; validateFormatting ; test ; mimaReportBinaryIssues")

// List all scripted test separately to schedule them in different travis-ci jobs.
// Travis-CI has hard timeouts for jobs, so we run them in smaller jobs as the scripted
// tests take quite some time to run.
// Ultimately we should run only those tests that are necessary for a change
addCommandAlias("validateUniversal", "scripted universal/*")
addCommandAlias("validateJar", "scripted jar/*")
addCommandAlias("validateBash", "scripted bash/*")
addCommandAlias("validateAsh", "scripted ash/*")
addCommandAlias("validateGraalVMNativeImage", "scripted graalvm-native-image/*")
addCommandAlias("validateRpm", "scripted rpm/*")
addCommandAlias("validateDebian", "scripted debian/*")
addCommandAlias("validateDocker", "scripted docker/*")
addCommandAlias("validateJdkPackager", "scripted jdkpackager/*")
// travis ci's jdk8 version doesn't support nested association elements.
// error: Caused by: class com.sun.javafx.tools.ant.Info doesn't support the nested "association" element.
addCommandAlias(
  "validateJdkPackagerTravis",
  "scripted jdkpackager/test-package-minimal jdkpackager/test-package-mappings"
)
addCommandAlias("validateMacOS", "; validate ; validateUniversal")

// TODO check the cygwin scripted tests and run them on appveyor
addCommandAlias("validateWindows", "; testOnly * -- -n windows ; scripted universal/dist universal/stage windows/*")

addCommandAlias("validateJlink", "scripted jlink/*")

addCommandAlias("releaseFromTravis", "release with-defaults")
