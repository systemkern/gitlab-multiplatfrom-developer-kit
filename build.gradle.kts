plugins {
    kotlin("multiplatform") version "1.4.10"
    id("docker.plugin") version "1.0.34"
}
group = "com.systemkern"
version = "0.0-SNAPSHOT"


repositories {
    mavenCentral()
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }

    }
    /*
    js {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                    webpackConfig.cssSupport.enabled = true
                }
            }
        }
    }
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> macosX64("native")
        hostOs == "Linux" -> linuxX64("native")
        isMingwX64 -> mingwX64("native")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }
    */

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
                implementation("org.jetbrains.kotlin:kotlin-reflect")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.10")
                implementation("org.springframework.boot:spring-boot-devtools:2.3.1.RELEASE")
                implementation("org.springframework.boot:spring-boot-starter-web:2.3.1.RELEASE")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("org.testcontainers:testcontainers:1.12.5")
                implementation("org.springframework.boot:spring-boot-starter-test") {
                    exclude(group = "mockito")
                    exclude(group = "org.hamcrest")
                    exclude(group = "org.junit.vintage")
                }

                implementation("org.springframework.boot:spring-boot-devtools:2.3.1.RELEASE")
                implementation("org.springframework.boot:spring-boot-starter-oauth2-client:2.3.1.RELEASE")
                implementation("org.springframework.boot:spring-boot-starter-aop:2.3.1.RELEASE")
                implementation("org.springframework.boot:spring-boot-starter-actuator:2.3.1.RELEASE")
                implementation("org.springframework.boot:spring-boot-starter-validation:2.3.1.RELEASE")

                // Rest and HATEOAS
                implementation("org.springframework.boot:spring-boot-starter-hateoas:2.3.1.RELEASE")
                implementation("org.springframework.boot:spring-boot-starter-web:2.3.1.RELEASE")

                // Security
                implementation("org.springframework.boot:spring-boot-starter-security:2.3.1.RELEASE")
                implementation("org.springframework.session:spring-session-data-redis:2.3.1.RELEASE")

                // spring data, JPA, Repositories and DB migration
                implementation("org.springframework.boot:spring-boot-starter-data-jpa:2.3.1.RELEASE")

                //Email
                implementation("org.springframework.boot:spring-boot-starter-mail:2.3.1.RELEASE")
                implementation("org.springframework.boot:spring-boot-starter-thymeleaf:2.3.1.RELEASE")

                val jupiter = "5.6.2"
                val mockkVersion = "1.10.2"
                implementation("org.junit.jupiter:junit-jupiter:$jupiter")
                implementation("org.springframework.security:spring-security-test:2.3.1.RELEASE")
                implementation("org.springframework.restdocs:spring-restdocs-mockmvc:2.0.4.RELEASE")
                implementation("io.mockk:mockk:${mockkVersion}")
                implementation("com.ninja-squad:springmockk:2.0.1")

                // testcontainers
                implementation("org.testcontainers:testcontainers:1.12.5")
                implementation("org.testcontainers:postgresql:1.12.5")

                //ebedded servers
                implementation("com.github.kstyrc:embedded-redis:0.6")
                implementation("org.eclipse.jgit:org.eclipse.jgit:5.8.1.202007141445-r")
            }
        }
        /**
        val jsMain by getting
        val jsTest by getting {
        dependencies {
        implementation(kotlin("test-js"))
        }
        }
        val nativeMain by getting
        val nativeTest by getting
         */
    }
}

