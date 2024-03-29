import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.20"
    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.ajalt.clikt:clikt:3.5.0")
    implementation("com.beust:klaxon:5.5")
    implementation("org.seleniumhq.selenium:selenium-java:4.7.2")
    implementation("io.github.bonigarcia:webdrivermanager:5.3.1")
    implementation("org.json:json:20090211")


    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.6.0")
    testImplementation("com.github.stefanbirkner:system-lambda:1.2.0")
}

application {
    // Define the main class for the application.
    mainClass.set("project.time.tracker.AppKt")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "18"
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed")
    }
}

val jar by tasks.getting(Jar::class) {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "project.time.tracker.AppKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
    }
}
