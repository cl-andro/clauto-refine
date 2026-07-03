task("clean", type = Delete::class) {
    delete(layout.buildDirectory)
}

subprojects {
    group = "com.clauto.tools.refine"
    val rawSuiteVersion = if (project.hasProperty("suiteVersion")) project.property("suiteVersion").toString() else ""
    val cleanSuiteVersion = if (rawSuiteVersion.startsWith("v")) rawSuiteVersion.substring(1) else rawSuiteVersion
    version = if (cleanSuiteVersion.isNotEmpty()) cleanSuiteVersion else "4.4.0"

    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11

            withSourcesJar()
            withJavadocJar()
        }
    }
    plugins.withId("maven-publish") {
        extensions.configure<PublishingExtension> {
            publications {
                withType(MavenPublication::class) {
                    version = project.version.toString()
                    group = project.group.toString()

                    pom {
                        name.set("HiddenApiRefine")
                        description.set("A Gradle plugin that improves the experience when developing Android apps, especially system tools, that use hidden APIs.")
                        url.set("https://github.com/ClautoW/HiddenApiRefinePlugin")
                        licenses {
                            license {
                                name.set("MIT License")
                                url.set("https://github.com/ClautoW/HiddenApiRefinePlugin/blob/main/LICENSE")
                            }
                        }
                        developers {
                            developer {
                                name.set("Kr328 & ClautoW")
                            }
                        }
                        scm {
                            connection.set("scm:git:https://github.com/ClautoW/HiddenApiRefinePlugin.git")
                            url.set("https://github.com/ClautoW/HiddenApiRefinePlugin")
                        }
                    }
                }
            }
            repositories {
                mavenLocal()
                maven {
                    name = "ossrh"
                    url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2")
                    credentials(PasswordCredentials::class.java)
                }
                maven {
                    name = "github"
                    url = uri("https://maven.pkg.github.com/cl-andro/clauto-refine")
                    credentials {
                        username = System.getenv("GITHUB_ACTOR")
                        password = System.getenv("GITHUB_TOKEN")
                    }
                }
            }
        }
        plugins.withId("signing") {
            extensions.configure<SigningExtension> {
                val signingKey = findProperty("signingKey") as? String
                val signingPassword = findProperty("signingPassword") as? String
                val secretKeyRingFile = findProperty("signing.secretKeyRingFile") as? String
                val gnuPgDir = java.io.File(System.getProperty("user.home") + "/.gnupg/private-keys-v1.d")

                val hasGpg = gnuPgDir.isDirectory && (gnuPgDir.list()?.size ?: 0) > 0
                val hasKeyRing = secretKeyRingFile != null && java.io.File(secretKeyRingFile).exists()
                val hasInMemory = !signingKey.isNullOrEmpty()

                if (hasGpg || hasKeyRing || hasInMemory) {
                    if (hasGpg) {
                        useGpgCmd()
                    } else if (hasInMemory) {
                        useInMemoryPgpKeys(signingKey, signingPassword)
                    }

                    plugins.withId("maven-publish") {
                        extensions.configure<PublishingExtension> {
                            publications {
                                withType(MavenPublication::class) {
                                    val signingTasks = sign(this)
                                    tasks.withType(AbstractPublishToMaven::class).matching { it.publication == this }.all {
                                        dependsOn(signingTasks)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
