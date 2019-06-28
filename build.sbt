import Settings._

lazy val root: Project = (project in file("."))
  .settings(commonSettings)
  .settings(
    name := "BFPackage",
    version := "1.0.3-SNAPSHOT",
    libraryDependencies ++= Seq(
    )
  )