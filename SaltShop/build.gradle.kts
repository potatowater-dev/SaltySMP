plugins {
    java
}

group = "com.saltysmp"
version = "1.1.3"

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

val coinApiDir = layout.buildDirectory.dir("coin-api")

tasks.register<JavaCompile>("compileCoinApi") {
    source = fileTree("../SaltCoins/src/main/java/com/saltysmp/saltcoins/api") {
        include("*.java")
    }
    classpath = configurations.named("compileClasspath").get()
    destinationDirectory.set(coinApiDir)
    options.release.set(21)
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn("compileCoinApi")
    classpath += files(coinApiDir)
}

tasks.jar {
    archiveBaseName.set("SaltShop")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}
