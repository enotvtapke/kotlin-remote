plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "integration-tests"

includeBuild("..")

include("bank")
include("todoapp")
include("todoapp-kotlinrpc")
include("mandelbrot")
include("social")
include("social-kotlinrpc")
include("cms")
include("cms-kotlinrpc")
include("pubsub")
include("pubsub-kotlinrpc")
include("bench")
include("bench-kotlinrpc")
include("growth-bench")
include("growth-bench-kotlinrpc")