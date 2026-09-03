plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("io.ktor.plugin") version "3.0.3"
    application
}

group = "com.ronydiaz.dockerlearn"
version = "0.0.1"

application {
    mainClass.set("com.ronydiaz.dockerlearn.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:3.0.3")
    implementation("io.ktor:ktor-server-netty-jvm:3.0.3")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.0.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.0.3")
    implementation("io.ktor:ktor-server-status-pages-jvm:3.0.3")
    implementation("io.ktor:ktor-server-cors-jvm:3.0.3")
    implementation("ch.qos.logback:logback-classic:1.5.16")

    testImplementation("io.ktor:ktor-server-test-host-jvm:3.0.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.1.0")
    implementation(kotlin("stdlib-jdk8"))
}

kotlin {
    jvmToolchain(17)
}
