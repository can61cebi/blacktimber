plugins {
    java
}

group = "tr.cebi"
version = "1.3.0"
description = "Smart tree felling that leaves player builds untouched."

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Folia API. Provided by the server at runtime, never shaded.
    // folia-api guarantees the threadedregions scheduler package at compile time.
    // Pinned to build 8, the same build the server jar (folia-26.1.2-8) ships.
    compileOnly("dev.folia:folia-api:26.1.2.build.8-stable")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
    options.compilerArgs.add("-Xlint:all")
}

tasks.processResources {
    filteringCharset = "UTF-8"
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("BlackTimber")
    archiveClassifier.set("")
}
