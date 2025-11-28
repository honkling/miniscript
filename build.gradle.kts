plugins {
    kotlin("jvm") version "2.1.10"
}

group = "me.honkling"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}