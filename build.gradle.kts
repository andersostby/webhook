import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val gradleVersion = "7.3.3"
val javaVersion = JavaVersion.VERSION_17

val slf4jVersion = "1.7.35"
val logbackVersion = "1.2.10"
val ktorVersion = "1.6.7"
val jacksonVersion = "2.13.1"
val rabbitmqVersion = "5.14.1"

val assertjVersion = "3.22.0"
val mockkVersion = "1.12.2"
val junitJupiterVersion = "5.8.2"

val mainClass = "com.andersostby.webhook.MainKt"

plugins {
    kotlin("jvm") version "1.6.10"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation("com.rabbitmq:amqp-client:$rabbitmqVersion")

    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
}

tasks {
    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    withType<Wrapper> {
        gradleVersion = gradleVersion
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = javaVersion.toString()
    }

    named<KotlinCompile>("compileTestKotlin") {
        kotlinOptions.jvmTarget = javaVersion.toString()
    }

    named<Jar>("jar") {
        baseName = "app"

        manifest {
            attributes["Main-Class"] = mainClass
            attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
                it.name
            }
        }

        doLast {
            configurations.runtimeClasspath.get().forEach {
                val file = File("$buildDir/libs/${it.name}")
                if (!file.exists())
                    it.copyTo(file)
            }
        }
    }
}

java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}
