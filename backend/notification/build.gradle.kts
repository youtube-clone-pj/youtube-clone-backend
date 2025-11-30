val querydslVersion: String by project
val martijndwarsWebPush: String by project
val bouncycastleBcprovJdk18on: String by project
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.querydsl:querydsl-jpa:${querydslVersion}:jakarta")
    annotationProcessor("com.querydsl:querydsl-apt:${querydslVersion}:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    implementation("nl.martijndwars:web-push:${martijndwarsWebPush}")
    implementation("org.bouncycastle:bcprov-jdk18on:${bouncycastleBcprovJdk18on}")

    implementation(project(":core"))
    implementation(project(":live-streaming:interaction"))
    implementation(project(":common"))

    testFixturesImplementation(project(":core"))

    testImplementation(testFixtures(project(":core")))
}
