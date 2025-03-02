import dev.s7a.gradle.minecraft.server.tasks.LaunchMinecraftServerTask

plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.14"
    id("dev.s7a.gradle.minecraft.server") version "3.2.1"
    id("com.gradleup.shadow") version "9.0.0-beta9"
}

group = "io.github.nopeless"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "spigotmc-repo"
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
    maven {
        name = "papermc"
        url = uri("https://papermc.io/repo/repository/maven-public/")
    }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.6-R0.1-SNAPSHOT")
    implementation("com.github.mwiede:jsch:0.2.23")

    paperweight.paperDevBundle("1.20.6-R0.1-SNAPSHOT")
}

tasks.register<LaunchMinecraftServerTask>("LaunchMinecraftServer") {
    dependsOn("shadowJar")

    doFirst {
        copy {
            from("build/libs/${project.name}-${version}-all.jar")
            into("build/MinecraftServer/plugins")
        }
    }

    jarUrl.set(LaunchMinecraftServerTask.JarUrl.Paper("1.20.6"))
    agreeEula.set(true)
}

val targetJavaVersion = 21
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"

    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible()) {
        options.release.set(targetJavaVersion)
    }
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
