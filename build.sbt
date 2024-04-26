lazy val pekkoHttpVersion = "1.0.1"
lazy val pekkoVersion = "1.0.2"

fork := true

lazy val root = (project in file(".")).settings(
  inThisBuild(
    List(
      organization := "com.jestclout",
      scalaVersion := "2.13.13"
    )
  ),
  name := "Jestclout Scala",
  libraryDependencies ++= Seq(
    "org.apache.pekko" %% "pekko-http" % pekkoHttpVersion,
    "org.apache.pekko" %% "pekko-http-spray-json" % pekkoHttpVersion,
    "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
    "org.apache.pekko" %% "pekko-stream" % pekkoVersion,
    "ch.qos.logback" % "logback-classic" % "1.2.13",
    "org.apache.pekko" %% "pekko-http-testkit" % pekkoHttpVersion % Test,
    "org.apache.pekko" %% "pekko-actor-testkit-typed" % pekkoVersion % Test,
    "org.scalatest" %% "scalatest" % "3.2.18" % Test
  )
)
