import Settings._

lazy val root: Project = (project in file("."))
  .settings(commonSettings)
  .settings(
    name := "BFPackage",
    version := "2.0.1-SNAPSHOT",
    libraryDependencies ++= Seq(
    )
  )