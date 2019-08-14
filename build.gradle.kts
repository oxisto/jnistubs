plugins {
    java
}

group = "com.github.oxisto"
version = "0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testCompile("junit", "junit", "4.12")
    compile("com.github.javaparser", "javaparser-core", "3.14.10")
    compile("com.github.javaparser", "javaparser-symbol-solver-core", "3.14.10")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
}