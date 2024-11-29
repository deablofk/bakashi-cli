plugins {
    id("java")
    application
    id("com.gradleup.shadow") version "8.3.5"
    id("org.graalvm.buildtools.native") version "0.10.3"
}

group = "dev.cwby.jasonify"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.jsoup:jsoup:1.18.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.1")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("dev.cwby.bakashi.Main") // replace with your main class path
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("bakashi-cli")
    archiveClassifier.set("")
    archiveVersion.set("")
}


graalvmNative {
    binaries {
        named("main") {
            imageName.set("bakashi-cli")
            mainClass.set("dev.cwby.bakashi.Main")
            buildArgs.add("-O4")
        }
        named("test") {
            buildArgs.add("-O0")
        }
    }
    binaries.all {
        buildArgs.add("--verbose")
        buildArgs.add("--enable-url-protocols=https")
        buildArgs.add("--enable-url-protocols=http")
    }
}