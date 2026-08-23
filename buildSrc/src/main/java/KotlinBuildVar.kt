object Version {

    private const val KUIKLY_VERSION = "2.16.0"
    private const val KOTLIN_VERSION = "2.1.21"

    /**
     * Get Kuikly version string: ${kuiklyVersion}-${kotlinVersion}
     */
    fun getKuiklyVersion(): String {
        return "$KUIKLY_VERSION-$KOTLIN_VERSION"
    }
}

object BuildPlugin {
    val kuikly by lazy {
        "com.tencent.kuikly-open:core-gradle-plugin:${Version.getKuiklyVersion()}"
    }
}
