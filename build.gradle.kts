import com.google.cloud.tools.gradle.appengine.appyaml.AppEngineAppYamlExtension

val kotlin_version: String by project
val logback_version: String by project
val ktor_version = "2.3.7"
val coroutines_version = "1.7.3"
val poi_version = "5.2.5"
val firestore_version = "3.19.1"

plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "1.9.0"
    id("io.ktor.plugin") version "3.1.1"
    id("com.gradleup.shadow") version "8.3.1"
    id("com.google.cloud.tools.appengine") version "2.8.0"
    application
}

group = "uz.mobiledv"
version = "0.0.1"

configure<AppEngineAppYamlExtension> {
    stage {
        setArtifact("build/libs/${project.name}-all.jar")
    }
    deploy {
        version = "GCLOUD_CONFIG"
        projectId = "GCLOUD_CONFIG"
    }
}

application {
    mainClass = "io.ktor.server.netty.EngineMain"

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
    google()
}

dependencies {
    // Ktor Core
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging:$ktor_version")
    implementation("io.ktor:ktor-server-cors:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    implementation("io.ktor:ktor-server-config-yaml:$ktor_version")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("org.slf4j:slf4j-api:2.0.9")

    // Excel processing
    implementation("org.apache.poi:poi:$poi_version")
    implementation("org.apache.poi:poi-ooxml:$poi_version")

    // Google Cloud Firestore
    implementation("com.google.cloud:google-cloud-firestore:$firestore_version")
    implementation("com.google.cloud:google-cloud-datastore:2.2.0")

    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutines_version")

    // Testing
    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")
}

tasks {
    shadowJar {
        archiveBaseName.set("uniplan-backend")
        archiveClassifier.set("all")
        archiveVersion.set("")
    }
}