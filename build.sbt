import Settings._

lazy val root: Project = (project in file("."))
  .settings(commonSettings)
  .settings(
    name := "BFPackage",
    libraryDependencies ++= Seq(
    )
  )