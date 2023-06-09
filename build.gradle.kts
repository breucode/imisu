import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
  kotlin("jvm") version Versions.kotlin
  kotlin("kapt") version Versions.kotlin
  id("jacoco")
  id("com.diffplug.spotless") version "6.19.0"
  id("io.gitlab.arturbosch.detekt") version "1.23.0"
  id("com.github.ben-manes.versions") version "0.47.0"
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
        "-agentlib:native-image-agent=config-merge-dir=src/main/resources/META-INF/native-image/",
      )
  }
}

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

repositories {
  mavenCentral()
  val github = ivy {
    url = uri("https://github.com/")
    patternLayout { artifact("/[organisation]/[module]/archive/refs/tags/v[revision].[ext]") }
    metadataSources { artifact() }
  }
  exclusiveContent {
    forRepositories(github)
    filter { includeModule("swagger-api", "swagger-ui") }
  }
}

val swaggerRuntime: Configuration by configurations.creating

dependencies {
  implementation(platform("org.http4k:http4k-bom:4.47.2.0"))
  implementation("org.http4k:http4k-core")
  implementation("org.http4k:http4k-server-netty")
  implementation("org.http4k:http4k-contract")
  implementation("org.http4k:http4k-format-jackson")
  implementation("org.http4k:http4k-client-okhttp")
  testImplementation("org.http4k:http4k-testing-kotest")

  swaggerRuntime("swagger-api:swagger-ui:4.12.0@zip")

  implementation("org.minidns:minidns-hla:1.0.4")

  val koinVersion = "3.4.1"
  implementation("io.insert-koin:koin-core:$koinVersion")
  testImplementation("io.insert-koin:koin-test:$koinVersion")

  val hopliteVersion = "2.7.4"
  implementation("com.sksamuel.hoplite:hoplite-core:$hopliteVersion")
  implementation("com.sksamuel.hoplite:hoplite-hocon:$hopliteVersion")
  implementation("com.sksamuel.hoplite:hoplite-props:1.0.8")

  runtimeOnly("org.slf4j:slf4j-simple:2.0.7")
  implementation("io.github.microutils:kotlin-logging:3.0.5")

  val kotestVersion = "5.6.2"
  testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion") {
    exclude("junit")
    exclude("org.junit.vintage")
  }
  testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")

  testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
  testImplementation("org.mockito:mockito-inline:5.2.0")
}

val createVersionProperties by
  tasks.registering(WriteProperties::class) {
    property("applicationVersion", version)
    outputFile = File("$buildDir/resources/main/versions.properties")
  }

val unzipSwagger by
  tasks.registering(Copy::class) {
    from(zipTree(swaggerRuntime.singleFile)) {
      include("*/dist/**")
      exclude("**/*.map")
      exclude("**/swagger-ui-es*.js")
      exclude("**/swagger-ui.js")
      exclude("**/oauth2-redirect.html")

      includeEmptyDirs = false

      eachFile {
        this.relativePath = RelativePath(true, *this.relativePath.segments.drop(2).toTypedArray())
      }
    }

    into("${sourceSets.main.get().output.resourcesDir!!.path}/swagger-ui")
  }

tasks.processResources {
  dependsOn(unzipSwagger)
  dependsOn(createVersionProperties)
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
    "--enable-http",
    "--enable-https",
    "--initialize-at-build-time=" +
      listOf(
          "org.slf4j.simple.SimpleLogger",
          "org.slf4j.LoggerFactory",
          "org.slf4j.impl.StaticLoggerBinder",
          "org.minidns",
        )
        .joinToString(","),
    "--initialize-at-run-time=io.netty.util.internal.logging.Log4JLogger",
    "-H:+StaticExecutableWithDynamicLibC",
    "-H:+InlineBeforeAnalysis",
  )
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(11)) }
