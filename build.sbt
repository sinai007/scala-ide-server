organization  := "com.example"

version       := "0.1"

scalaVersion  := "2.11.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")


  val akkaV = "2.4.9"
  val sprayV = "1.3.2"


val sprayDependencies = {
  Seq(
    // "io.spray"            %%  "spray-testkit" % sprayV  % "test",
    // "com.typesafe.akka"   %%  "akka-testkit"  % akkaV   % "test",
    "io.spray"            %%  "spray-can"     % sprayV,
    "io.spray"            %%  "spray-routing" % sprayV,
    "io.spray"            %%  "spray-json" % sprayV,
    "com.typesafe.akka"   %%  "akka-actor"    % akkaV
  )
}

// https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
libraryDependencies ++= Seq(
    "ch.qos.logback" % "logback-classic" % "1.1.7",
    "org.specs2" %% "specs2-core" % "3.8.5" % "test",
    "org.scala-lang" % "scala-compiler" % "2.11.8"
  ) ++ sprayDependencies

// cancelable in Global := true
fork in run := true
fork in test := true

javaOptions in run := Seq(
  "-DideServerPort=9090"
)
//javaOptions in run +=
