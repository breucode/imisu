import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.4.0"
  kotlin("kapt") version "1.4.0"
  id("com.diffplug.spotless") version "5.1.1"
  id("io.gitlab.arturbosch.detekt") version "1.11.1"
  id("com.github.ben-manes.versions") version "0.29.0"
  id("application")
  id("com.github.johnrengelman.shadow") version "6.0.0"
}

group = "de.breuco"
version = "0.0.1"

tasks.wrapper {
  distributionType = Wrapper.DistributionType.ALL
}

application.mainClassName = "de.breuco.imisu.MainKt"

spotless {
  val ktlintVersion = "0.37.2"
  kotlin {
    ktlint(ktlintVersion).userData(
      mapOf(
        Pair("indent_size", "2"),
        Pair("max_line_length", "120")
      )
    )
  }
  kotlinGradle {
    target("*.gradle.kts")

    ktlint(ktlintVersion).userData(
      mapOf(
        Pair("indent_size", "2"),
        Pair("max_line_length", "120")
      )
    )
  }
}

val javaVersion = JavaVersion.VERSION_11.toString()

tasks {
  withType<Detekt> {
    this.jvmTarget = javaVersion
  }
}

detekt {
  failFast = true
  buildUponDefaultConfig = true
  config = files("$projectDir/detekt.yaml")
  // baseline = file("$projectDir/detekt-baseline.xml")
}

fun isNonStable(version: String): Boolean {
  val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
  val regex = "^[0-9,.v-]+(-r)?$".toRegex()
  val isStable = stableKeyword || regex.matches(version)
  return isStable.not()
}

tasks.named("dependencyUpdates", DependencyUpdatesTask::class.java).configure {
  rejectVersionIf {
    isNonStable(candidate.version)
  }

  revision = "release"
  gradleReleaseChannel = "current"
}

repositories {
  mavenCentral()
  jcenter()
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-reflect") {
    version {
      strictly("1.4.0")
    }
  }
  implementation(platform("org.http4k:http4k-bom:3.258.0"))
  implementation("org.http4k:http4k-core")
  implementation("org.http4k:http4k-server-undertow")
  implementation("org.http4k:http4k-contract")
  implementation("org.http4k:http4k-format-jackson")
  implementation("org.http4k:http4k-client-okhttp")

  implementation("org.minidns:minidns-hla:1.0.0")

  val koinVersion = "2.1.6"
  implementation("org.koin:koin-core:$koinVersion")
  implementation("org.koin:koin-core-ext:$koinVersion")
  testImplementation("org.koin:koin-test:$koinVersion")

  val arrowVersion = "0.10.5"
  implementation("io.arrow-kt:arrow-core:$arrowVersion")
  implementation("io.arrow-kt:arrow-syntax:$arrowVersion")
  kapt("io.arrow-kt:arrow-meta:$arrowVersion")

  implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.3.9"))
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

  val hopliteVersion = "1.3.3"
  implementation("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
  implementation("com.sksamuel.hoplite:hoplite-hocon:$hopliteVersion")

  runtimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:2.13.3")
  implementation("io.github.microutils:kotlin-logging:1.8.3")

  val kotestVersion = "4.2.0.RC2"
  testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
  testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
  testImplementation("io.kotest:kotest-assertions-arrow-jvm:$kotestVersion")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.2")

  testImplementation("io.mockk:mockk:1.10.0")
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
