addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.3")              // to publish to ls.implicit.ly

resolvers ++= Seq(
  Classpaths.sbtPluginReleases,
  Opts.resolver.sonatypeReleases
)