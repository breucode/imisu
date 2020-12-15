import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import io.gitlab.arturbosch.detekt.Detekt
import org.apache.tools.ant.filters.ReplaceTokens
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version Versions.kotlin
  kotlin("kapt") version Versions.kotlin
  id("jacoco")
  id("com.diffplug.spotless") version "5.8.2"
  id("io.gitlab.arturbosch.detekt") version "1.14.2"
  id("com.github.ben-manes.versions") version "0.36.0"
  id("application")
  id("org.mikeneck.graalvm-native-image") version "0.9.1"
}

group = "de.breuco"
version = "0.6.5"

tasks.wrapper {
  distributionType = Wrapper.DistributionType.ALL
}

application.mainClass.set("de.breuco.imisu.ApplicationKt")

tasks.jacocoTestReport {
  dependsOn(tasks.test)

  reports {
    csv.isEnabled = true
  }
}

if (project.property("generateNativeImageConfig").toString().toBoolean()) {
  application {
    applicationDefaultJvmArgs = listOf("-agentlib:native-image-agent=config-merge-dir=src/main/resources/META-INF/native-image/")
  }
}

val swaggerUiVersion = "3.38.0"

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
  val ktlintVersion = "0.40.0"
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

    prettier("2.2.0")
  }
}

val javaVersion = JavaVersion.VERSION_11

tasks {
  withType<Detekt> {
    this.jvmTarget = javaVersion.toString()
  }
}

detekt {
  failFast = true
  buildUponDefaultConfig = true
  config = files("$projectDir/detekt.yaml")
  baseline = file("$projectDir/detekt-baseline.xml")
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
  implementation(platform("org.http4k:http4k-bom:3.283.1"))
  implementation("org.http4k:http4k-core")
  implementation("org.http4k:http4k-server-netty")
  implementation("org.http4k:http4k-contract")
  implementation("org.http4k:http4k-format-jackson")
  implementation("org.http4k:http4k-client-okhttp")
  testImplementation("org.http4k:http4k-testing-kotest")

  runtimeOnly("org.webjars:swagger-ui:$swaggerUiVersion")

  implementation("com.github.ajalt:clikt:2.8.0")

  implementation("org.minidns:minidns-hla:1.0.0")

  val koinVersion = "2.2.1"
  implementation("org.koin:koin-core:$koinVersion")
  implementation("org.koin:koin-core-ext:$koinVersion")
  testImplementation("org.koin:koin-test:$koinVersion")

  implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.9")

  val hopliteVersion = "1.3.10"
  implementation("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
  implementation("com.sksamuel.hoplite:hoplite-hocon:$hopliteVersion")
  implementation("com.sksamuel.hoplite:hoplite-props:1.0.8")

  runtimeOnly("org.slf4j:slf4j-simple:1.7.30")
  implementation("io.github.microutils:kotlin-logging:2.0.3")

  val kotestVersion = "4.3.2"
  testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion") {
    exclude("junit")
    exclude("org.junit.vintage")
  }
  testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")

  testImplementation("io.mockk:mockk:1.10.0")
}

tasks.test {
  useJUnitPlatform()
}

tasks.nativeImage {
  setGraalVmHome(System.getProperty("java.home"))
  mainClass = application.mainClass.get()
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
    "-H:+StaticExecutableWithDynamicLibC"
  )
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = javaVersion.toString()

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions.jvmTarget = javaVersion.toString()

java {
  sourceCompatibility = javaVersion
  targetCompatibility = javaVersion
}
