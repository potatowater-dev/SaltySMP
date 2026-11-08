plugins {
    java
}

group = "com.saltysmp"
version = "1.1.7"

val saltCratesJar = file("../SaltCrates/build/libs/SaltCrates-1.3.3.jar")

val buildSaltCrates = tasks.register<Exec>("buildSaltCrates") {
    workingDir = file("../SaltCrates")
    commandLine("cmd", "/c", "gradlew.bat", "jar", "-q")
    onlyIf { !saltCratesJar.exists() }
}

tasks.compileJava {
    dependsOn(buildSaltCrates)
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly(files(saltCratesJar))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.jar {
    archiveBaseName.set("SaltKOTH")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}
