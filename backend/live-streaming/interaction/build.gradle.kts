val querydslVersion: String by project
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.querydsl:querydsl-jpa:${querydslVersion}:jakarta")
    annotationProcessor("com.querydsl:querydsl-apt:${querydslVersion}:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    implementation(project(":core"))

    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation(testFixtures(project(":core")))
    testImplementation(testFixtures(project(":api")))

    testFixturesImplementation(project(":core"))
}