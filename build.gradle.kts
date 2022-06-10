import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
  kotlin("multiplatform") version Versions.kotlin
  kotlin("plugin.serialization") version Versions.kotlin

  id("org.jlleitschuh.gradle.ktlint") version "10.3.0"
  id("io.gitlab.arturbosch.detekt") version "1.20.0"
  id("com.github.ben-manes.versions") version "0.42.0"
}

group = "de.breuco"

repositories {
  mavenCentral()
}

kotlin {
  val nativeTarget = when (System.getProperty("os.name")) {
    "Mac OS X" -> macosX64("native")
    "Linux" -> linuxX64("native")
    else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
  }

  nativeTarget.apply {
    binaries {
      executable {
        entryPoint = "de.breuco.imisu.main"
      }
    }
  }

  sourceSets {
    val nativeMain by getting {
      dependencies {
        implementation("io.ktor:ktor-server-core:${Versions.ktor}")
        implementation("io.ktor:ktor-server-cio:${Versions.ktor}")
        implementation("io.ktor:ktor-client-core:${Versions.ktor}")
        implementation("io.ktor:ktor-client-curl:${Versions.ktor}")

        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
      }
    }
    val nativeTest by getting
  }
}

detekt {
  buildUponDefaultConfig = true
  allRules = true
  config = files("$projectDir/detekt.yaml")
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
  version.set(Versions.ktlint)
}

tasks.named("dependencyUpdates", DependencyUpdatesTask::class.java).configure {
  rejectVersionIf { isNonStable(candidate.version) }

  revision = "release"
  gradleReleaseChannel = "current"
}

fun isNonStable(version: String): Boolean {
  val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
  val regex = "^[0-9,.v-]+(-r)?$".toRegex()
  val isStable = stableKeyword || regex.matches(version)
  return isStable.not()
}
