plugins { kotlin("jvm") }

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testImplementation("org.apache.kafka:kafka-clients:3.7.0")
    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.13")
}

tasks.test {
    useJUnitPlatform()
    systemProperty("junit.jupiter.execution.timeout.default", "5m")
}
