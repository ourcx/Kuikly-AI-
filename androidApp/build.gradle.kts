plugins {
    id("com.android.application")
    kotlin("android")
}

fun String.toBuildConfigStringLiteral(): String = buildString {
    append('"')
    this@toBuildConfigStringLiteral.forEach { character ->
        when (character) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\b' -> append("\\b")
            '\t' -> append("\\t")
            '\n' -> append("\\n")
            '\u000C' -> append("\\f")
            '\r' -> append("\\r")
            else -> {
                if (character.code < 0x20 || character.code == 0x7F) {
                    append("\\u%04x".format(character.code))
                } else {
                    append(character)
                }
            }
        }
    }
    append('"')
}

android {
    namespace = "com.ourcx.kuiklystock"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.ourcx.kuiklystock"
        minSdk = 21
        targetSdk = 30
        versionCode = 1
        versionName = "1.0"
        buildConfigField(
            "String",
            "OPENAI_PROXY_URL",
            System.getenv("OPENAI_PROXY_URL").orEmpty().toBuildConfigStringLiteral(),
        )
        buildConfigField(
            "String",
            "OPENAI_MODEL",
            (System.getenv("OPENAI_MODEL") ?: "gpt-5.6").toBuildConfigStringLiteral(),
        )
    }
    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("androidx.core:core-ktx:1.6.0")
    implementation("androidx.dynamicanimation:dynamicanimation:1.0.0")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")
}
