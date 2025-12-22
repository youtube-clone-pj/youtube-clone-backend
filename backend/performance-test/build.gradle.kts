plugins {
    java
    id("io.gatling.gradle") version "3.14.9"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}