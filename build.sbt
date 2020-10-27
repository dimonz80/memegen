

name := """Memegen"""

organization := "ru.dimonz80"

version := "0.0"

scalaVersion := "2.13.1"

lazy val anormVersion = "2.6.5"


lazy val root = (project in file(".")).enablePlugins(PlayScala)


libraryDependencies ++= Seq(
  jdbc,
  ehcache,
  ws,
  specs2 % Test)

libraryDependencies += evolutions

libraryDependencies += filters

libraryDependencies += guice

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % "test"

libraryDependencies += "org.playframework.anorm" %% "anorm" % anormVersion

libraryDependencies += "com.h2database" % "h2" % "1.4.194"

libraryDependencies += "org.postgresql" % "postgresql" % "42.1.1"

val circeVersion = "0.12.3"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

libraryDependencies += "com.dripower" %% "play-circe" % "2812.0"

// Disable genereate doc when dist
// sources in(Compile, doc) := Seq.empty

publishArtifact in(Compile, packageDoc) := false

//// Конфиг для тестов
///javaOptions in Test += "-Dconfig.file=conf/application.test.conf"

//// Конфиг для разработки
//javaOptions in Compile += "-Dconfig.file=conf/application.dev.conf"


//add files/* to universal/stage
mappings in Universal ++= {
  (baseDirectory.value / "files" ** "*").get.map { f =>
    val path = f.getAbsolutePath.replace(baseDirectory.value.getAbsolutePath, "")
    f -> path
  }
}


// All in UTF-8!!!
javacOptions ++= Seq("-encoding", "UTF-8")

// scalac options
scalacOptions ++= Seq("-Ymacro-annotations", "-language:postfixOps")
