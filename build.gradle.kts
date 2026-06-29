plugins {
    kotlin("jvm") version "2.4.0"
    application
}

group = "com.oracle.dev.jdbc"
version = "0.0.1-SNAPSHOT"

description = "Kotlin Exposed sample for Oracle AI Database 26ai VECTOR columns and similarity search"

val javaVersion = JavaVersion.VERSION_23
val jdbcVersion = "23.26.2.0.0"
val exposedVersion = "1.3.0"

java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}


dependencies {
    implementation(kotlin("stdlib"))

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    implementation("com.oracle.database.jdbc:ojdbc17:$jdbcVersion")
    implementation("com.oracle.database.jdbc:ucp17:$jdbcVersion")

    runtimeOnly("org.slf4j:slf4j-simple:2.0.17")

    testImplementation(kotlin("test"))
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(23)
    options.encoding = "UTF-8"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_23)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

application {
    mainClass.set("com.oracle.dev.jdbc.MainKt")
}


