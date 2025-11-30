val querydslVersion: String by project
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.querydsl:querydsl-jpa:${querydslVersion}:jakarta")
    annotationProcessor("com.querydsl:querydsl-apt:${querydslVersion}:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    implementation(project(":core"))

    testImplementation(testFixtures(project(":core")))
    testImplementation(testFixtures(project(":api")))

    testFixturesImplementation(project(":core"))
}