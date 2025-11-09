import org.springframework.boot.gradle.tasks.bundling.BootJar

tasks.named<BootJar>("bootJar") {
    enabled = true
    mainClass.set("com.youtube-clone.api.CloneApplication")
}

/*
    다른 모듈의 테스트에서 api 모듈을 참조할 수 있도록 일반 JAR 생성을 활성화
    Spring Boot는 bootJar가 활성화되면 기본적으로 jar 태스크를 비활성화하지만,
    E2E 테스트에서 전체 애플리케이션 컨텍스트를 사용하려면 일반 JAR가 필요함
    예: live-streaming:interaction 모듈의 테스트가 testImplementation(project(":api"))로 의존

    다른 해결방안도 존재하지만 테스트 코드의 유연성을 위해 true로 설정함
 */
tasks.named<Jar>("jar") {
    enabled = true
}

val restAssuredVersion: String by project
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.security:spring-security-crypto")
    implementation("org.springframework:spring-tx")

    implementation(project(":core"))
    implementation(project(":live-streaming:interaction"))

    testImplementation(testFixtures(project(":core")))
}