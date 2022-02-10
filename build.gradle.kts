import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.apache.tools.ant.filters.ReplaceTokens

plugins {
  kotlin("jvm") version Versions.kotlin
  kotlin("kapt") version Versions.kotlin
  id("jacoco")
  id("com.diffplug.spotless") version "6.1.0"
  id("io.gitlab.arturbosch.detekt") version "1.19.0"
  id("com.github.ben-manes.versions") version "0.41.0"
  id("application")
  id("org.mikeneck.graalvm-native-image") version "1.4.1"
}

group = "de.breuco"
version = "0.8.1"

tasks.wrapper {
  distributionType = Wrapper.DistributionType.ALL
}

application.mainClass.set("de.breuco.imisu.ApplicationKt")

tasks.jacocoTestReport {
  dependsOn(tasks.test)

  reports {
    csv.required.set(true)
  }
}

if (project.property("generateNativeImageConfig").toString().toBoolean()) {
  application {
    applicationDefaultJvmArgs = listOf("-agentlib:native-image-agent=config-merge-dir=src/main/resources/META-INF/native-image/")
  }
}

val swaggerUiVersion = "4.1.2"

val createVersionProperties by tasks.registering(WriteProperties::class) {
  dependsOn(tasks.processResources)
  property("applicationVersion", version)
  property("swaggerUiVersion", swaggerUiVersion)
  outputFile = File("$buildDir/resources/main/versions.properties")
}

tasks.processResources {
  filter<ReplaceTokens>("tokens" to mapOf("SWAGGER_UI_VERSION" to swaggerUiVersion))
}

tasks.classes {
  dependsOn(createVersionProperties)
}

spotless {
  val ktlintVersion = "0.42.1"
  kotlin {
    ktlint(ktlintVersion).userData(
      mapOf(
        Pair("indent_size", "2")
      )
    )
  }
  kotlinGradle {
    target("*.gradle.kts")

    ktlint(ktlintVersion).userData(
      mapOf(
        Pair("indent_size", "2")
      )
    )
  }
  format("prettier") {
    target("*.md", "*.yaml", ".github/**/*.yaml", "src/**/*.json")

    prettier("2.4.0")
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
  rejectVersionIf {
    isNonStable(candidate.version)
  }

  revision = "release"
  gradleReleaseChannel = "current"
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(platform("org.http4k:http4k-bom:4.17.7.0"))
  implementation("org.http4k:http4k-core")
  implementation("org.http4k:http4k-server-netty")
  implementation("org.http4k:http4k-contract")
  implementation("org.http4k:http4k-format-jackson")
  implementation("org.http4k:http4k-client-okhttp")
  testImplementation("org.http4k:http4k-testing-kotest")

  runtimeOnly("org.webjars:swagger-ui:$swaggerUiVersion")

  implementation("org.minidns:minidns-hla:1.0.2")

  val koinVersion = "3.1.4"
  implementation("io.insert-koin:koin-core:$koinVersion")
  testImplementation("io.insert-koin:koin-test:$koinVersion")

  val hopliteVersion = "1.4.16"
  implementation("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
  implementation("com.sksamuel.hoplite:hoplite-hocon:$hopliteVersion")
  implementation("com.sksamuel.hoplite:hoplite-props:1.0.8")

  runtimeOnly("org.slf4j:slf4j-simple:1.7.32")
  implementation("io.github.microutils:kotlin-logging:2.1.21")

  val kotestVersion = "5.1.0"
  testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion") {
    exclude("junit")
    exclude("org.junit.vintage")
  }
  testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")

  testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
  testImplementation("org.mockito:mockito-inline:4.2.0")

  implementation(kotlin("reflect", version = Versions.kotlin))
}

tasks.test {
  useJUnitPlatform()
}

tasks.nativeImage {
  setGraalVmHome(System.getProperty("java.home"))
  buildType { buildTypeSelector ->
    buildTypeSelector.executable {
      main = application.mainClass.get()
    }
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
      ).joinToString(","),
    "--initialize-at-run-time=io.netty.util.internal.logging.Log4JLogger",
    "-H:+StaticExecutableWithDynamicLibC",
    "-H:+InlineBeforeAnalysis"
  )
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}
