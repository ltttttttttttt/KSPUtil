group "com.lt"
version "1.0"

allprojects {
    repositories {
        maven("https://mirrors.tencent.com/nexus/repository/maven-public/")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
        mavenCentral()
    }
}

plugins {
    kotlin("jvm") apply false
}