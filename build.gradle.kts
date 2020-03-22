import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.70"
    id("application")
    id("com.github.johnrengelman.shadow") version "5.2.0"

}

group = "de.breuco"
version = "0.0.1"

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

application.mainClassName = "de.breuco.imisu.MainKt"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    val http4kVersion = "3.239.0"

    implementation("org.http4k:http4k-core:$http4kVersion")
    implementation("org.http4k:http4k-server-netty:$http4kVersion")
    implementation("org.http4k:http4k-contract:$http4kVersion")
    implementation("org.http4k:http4k-format-jackson:$http4kVersion")

    implementation("org.minidns:minidns-hla:0.3.3")
    runtimeOnly("org.slf4j:slf4j-simple:1.7.30")
    implementation("io.github.microutils:kotlin-logging:1.7.9")
}

tasks.getByName("shadowDistZip") {
    enabled = false
}
tasks.getByName("shadowDistTar") {
    enabled = false
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "11"

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions.jvmTarget = "11"
