dependencies {
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    implementation(project(":core"))

    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation(testFixtures(project(":core")))
    testImplementation(testFixtures(project(":api")))
}