import org.gradle.kotlin.dsl.support.listFilesOrdered
import java.util.Base64
import java.nio.file.Files

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
}

val encodeLibs = tasks.register("encodeLibs") {
    dependsOn("externalNativeBuildDebug", "externalNativeBuildRelease")

    doLast {
        val abiDirs = fileTree("$buildDir/intermediates/cmake")
            .matching {
                include("**/obj/**")
            }
            .files
            .map { it.parentFile }
            .distinct()

        val abiMap = mutableMapOf<String, MutableMap<String, String>>()

        abiDirs.forEach { abiDir ->
            val abiName = abiDir.name
            abiMap[abiName] = mutableMapOf()

            val soFiles = abiDir.listFiles { _, name -> name.endsWith(".so") } ?: arrayOf()
            soFiles.forEach { soFile ->
                val libName = soFile.name
                val fileContent = Files.readAllBytes(soFile.toPath())
                val base64Content = Base64.getEncoder().encodeToString(fileContent)

                abiMap[abiName]?.put(libName, base64Content)
            }
        }

        val packageName = "com.skiy.sf.data"
        val className = "EncodedLibs"
        val generatedSrcDir = file("$buildDir/generated/source/encodedLibs")
        val packageDir = File(generatedSrcDir, packageName.replace('.', '/'))
        packageDir.mkdirs()

        val classContent = buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("object $className {")
            abiMap.forEach { (abi, libsMap) ->
                val abiConstantName = abi.replace("-", "_").uppercase()
                appendLine("    val ${abiConstantName}_LIBS = mapOf(")
                libsMap.forEach { (libName, base64Content) ->
                    appendLine("        \"$libName\" to \"$base64Content\",")
                }
                appendLine("    )")
                appendLine()
            }
            appendLine("}")
        }

        val classFile = File(packageDir, "$className.kt")
        classFile.writeText(classContent)
    }
}

android {
    ndkVersion = sdkDirectory.resolve("ndk").listFilesOrdered().last().name
    namespace = "com.skiy.sf.nmhooklib"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_STL=c++_shared"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.28.0+"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        buildConfig = false
        prefab = true
    }

    sourceSets["main"].java.srcDir("$buildDir/generated/source/encodedLibs")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    dependsOn(encodeLibs)
}

tasks.whenTaskAdded {
    if (name.contains("JavaCompile") || name.contains("KotlinCompile")) {
        dependsOn(encodeLibs)
    }
}

dependencies {
    implementation("org.lsposed.lsplant:lsplant-standalone:6.4")
    implementation("io.github.vvb2060.ndk:dobby:1.2")
}

publishing {
    publications {
        register<MavenPublication>("gpr") {
            afterEvaluate {
                from(components["release"])
            }
            groupId = "com.skiy.sf"
            artifactId = "hook"
            version = "1.0"

            pom {
                name.set("Shadow Fiend Hook")
                description.set("A hooking system for Android applications.")
                url.set("https://github.com/Matrix-Username/Shadow-Fiend-Hook")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("Matrix-Username")
                        name.set("MatriX")
                        email.set("andreypolkovenko@gmail.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/Matrix-Username/Shadow-Fiend-Hook.git")
                    developerConnection.set("scm:git:ssh://github.com/Matrix-Username/Shadow-Fiend-Hook.git")
                    url.set("https://github.com/Matrix-Username/Shadow-Fiend-Hook")
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Matrix-Username/Shadow-Fiend-Hook")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
            }
        }
    }
}