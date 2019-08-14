import com.palantir.gradle.graal.GraalExtension;
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("com.palantir.graal") version "0.4.0"
  kotlin("jvm") version "1.3.41"
}

configure<GraalExtension> {
  mainClass("com.github.oxisto.jnistubs.JNIStubsCLI")
  outputName("jnistubs")
  option("--initialize-at-build-time")
}

group = "com.github.oxisto"
version = "0.1-SNAPSHOT"

repositories {
  mavenCentral()
}

dependencies {
  implementation("com.github.javaparser", "javaparser-core", "3.14.10")
  implementation("com.github.javaparser", "javaparser-symbol-solver-core", "3.14.10")
  implementation(kotlin("stdlib-jdk8"))
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
  jvmTarget = "1.8"
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
  jvmTarget = "1.8"
}
