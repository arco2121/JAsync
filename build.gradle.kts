plugins {
    id("java-library")
    id("maven-publish")
}

group = "com.arco2121.jasync"
version = "1.1.0"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    withSourcesJar()
    withJavadocJar()
}

dependencies {
    implementation("org.aspectj:aspectjrt:1.9.20.1")
    implementation("org.aspectj:aspectjweaver:1.9.20.1")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    test {
        useJUnitPlatform()
        if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_21)) {
            jvmArgs("--enable-preview")
        }
    }

    compileJava {
        options.encoding = "UTF-8"
        if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_21)) {
            options.compilerArgs.addAll(listOf("--enable-preview"))
        }
    }

    javadoc {
        options.encoding = "UTF-8"
        if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_21)) {
            (options as StandardJavadocDocletOptions).addStringOption("-enable-preview", "")
        }
    }

    jar {
        manifest {
            attributes(
                mapOf(
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version,
                    "Implementation-Vendor" to "arco2121",
                    "Multi-Release" to "true"
                )
            )
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.arco2121"
            artifactId = "jasync"
            version = "1.1.0"
            from(components["java"])
            pom {
                name.set("JAsync")
                description.set("A modern, async/await library for Java that brings JavaScript/Python-style asynchronous programming to the JVM")
                url.set("https://github.com/arco2121/JAsync")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("arco2121")
                        name.set("arco2121")
                        url.set("https://github.com/arco2121")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/arco2121/JAsync.git")
                    developerConnection.set("scm:git:ssh://github.com/arco2121/JAsync.git")
                    url.set("https://github.com/arco2121/JAsync")
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/arco2121/JAsync")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}