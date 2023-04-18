plugins {
    java
    id("org.jetbrains.intellij") version "1.8.0"
}

group = "com.mao.graphqlgenerator"
version = "1.0.0-SNAPSHOT"

//repositories {
//    mavenCentral()
//}

buildscript {
    repositories {
        mavenLocal()
        maven { url = uri("https://maven.aliyun.com/repository/public/") }
        mavenCentral()
        maven { url = uri("https://plugins.gradle.org/m2/") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/releases/") }
        maven { url = uri("https://dl.bintray.com/jetbrains/intellij-plugin-service") }
        maven { url = uri("https://dl.bintray.com/jetbrains/intellij-third-party-dependencies/") }
    }
    dependencies {
        classpath("org.jetbrains.intellij.plugins:gradle-intellij-plugin:1.8.0")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2022.2.2")
    type.set("IU") // Target IDE Platform

    plugins.set(listOf("java",
            "DatabaseTools"))
}


repositories {
    mavenLocal()
    jcenter()
    mavenCentral()
}

dependencies {
    implementation("com.graphql-java:graphql-java:20.2")
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}
