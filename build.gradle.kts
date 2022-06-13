import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.apache.tools.ant.filters.ReplaceTokens

plugins {
  kotlin("jvm") version Versions.kotlin
  kotlin("kapt") version Versions.kotlin
  id("jacoco")
  id("com.diffplug.spotless") version "6.7.2"
  id("io.gitlab.arturbosch.detekt") version "1.20.0"
  id("com.github.ben-manes.versions") version "0.42.0"
  id("application")
  id("org.mikeneck.graalvm-native-image") version "1.4.1"
}

group = "de.breuco"

version = "%ARTIFACT_VERSION%"

tasks.wrapper { distributionType = Wrapper.DistributionType.ALL }

application.mainClass.set("de.breuco.imisu.ApplicationKt")

tasks.jacocoTestReport {
  dependsOn(tasks.test)

  reports { csv.required.set(true) }
}

if (project.property("generateNativeImageConfig").toString().toBoolean()) {
  application {
    applicationDefaultJvmArgs =
      listOf(
        "-agentlib:native-image-agent=config-merge-dir=src/main/resources/META-INF/native-image/"
      )
  }
}

val swaggerUiVersion = "4.1.2"

val createVersionProperties by
  tasks.registering(WriteProperties::class) {
    dependsOn(tasks.processResources)
    property("applicationVersion", version)
    property("swaggerUiVersion", swaggerUiVersion)
    outputFile = File("$buildDir/resources/main/versions.properties")
  }

tasks.processResources {
  filter<ReplaceTokens>("tokens" to mapOf("SWAGGER_UI_VERSION" to swaggerUiVersion))
}

tasks.classes { dependsOn(createVersionProperties) }

spotless {
  val ktfmtVersion = "0.37"
  kotlin { ktfmt(ktfmtVersion).googleStyle() }
  kotlinGradle {
    target("*.gradle.kts")

    ktfmt(ktfmtVersion).googleStyle()
  }
  format("prettier") {
    target("*.md", "*.yaml", ".github/**/*.yaml", "src/**/*.json")

    prettier("2.6.2")
  }
}

detekt {
  buildUponDefaultConfig = true
  allRules = true
  config = files("$projectDir/detekt.yaml")
}

fun isNonStable(version: String): Boolean {
  val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
  val regex = "^[0-9,.v-]+(-r)?$".toRegex()
  val isStable = stableKeyword || regex.matches(version)
  return isStable.not()
}

tasks.named("dependencyUpdates", DependencyUpdatesTask::class.java).configure {
  rejectVersionIf { isNonStable(candidate.version) }

  revision = "release"
  gradleReleaseChannel = "current"
}

repositories { mavenCentral() }

dependencies {
  implementation(platform("org.http4k:http4k-bom:4.27.0.0"))
  implementation("org.http4k:http4k-core")
  implementation("org.http4k:http4k-server-netty")
  implementation("org.http4k:http4k-contract")
  implementation("org.http4k:http4k-format-jackson")
  implementation("org.http4k:http4k-client-okhttp")
  testImplementation("org.http4k:http4k-testing-kotest")

  runtimeOnly("org.webjars:swagger-ui:$swaggerUiVersion")

  implementation("org.minidns:minidns-hla:1.0.3")

  val koinVersion = "3.2.0"
  implementation("io.insert-koin:koin-core:$koinVersion")
  testImplementation("io.insert-koin:koin-test:$koinVersion")

  val hopliteVersion = "2.1.5"
  implementation("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
  implementation("com.sksamuel.hoplite:hoplite-hocon:$hopliteVersion")
  implementation("com.sksamuel.hoplite:hoplite-props:1.0.8")

  runtimeOnly("org.slf4j:slf4j-simple:1.7.36")
  implementation("io.github.microutils:kotlin-logging:2.1.23")

  val kotestVersion = "5.3.1"
  testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion") {
    exclude("junit")
    exclude("org.junit.vintage")
  }
  testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")

  testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
  testImplementation("org.mockito:mockito-inline:4.6.1")
}

tasks.test { useJUnitPlatform() }

tasks.nativeImage {
  setGraalVmHome(System.getProperty("java.home"))
  buildType { buildTypeSelector ->
    buildTypeSelector.executable { main = application.mainClass.get() }
  }
  executableName = "imisu"
  arguments(
    "--no-fallback",
    "--allow-incomplete-classpath",
    "--enable-http",
    "--enable-https",
    "--initialize-at-build-time=" +
      listOf(
          "org.slf4j.impl.SimpleLogger",
          "org.slf4j.LoggerFactory",
          "org.slf4j.impl.StaticLoggerBinder",
          "org.minidns"
        )
        .joinToString(","),
    "--initialize-at-run-time=io.netty.util.internal.logging.Log4JLogger",
    "-H:+StaticExecutableWithDynamicLibC",
    "-H:+InlineBeforeAnalysis"
  )
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(17)) }
