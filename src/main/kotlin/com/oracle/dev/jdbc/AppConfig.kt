package com.oracle.dev.jdbc

data class AppConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val poolName: String = "oracle-vector-exposed-sample",
    val initialPoolSize: Int = 1,
    val minPoolSize: Int = 1,
    val maxPoolSize: Int = 4,
) {
    companion object {
        fun fromEnvironment(): AppConfig {
            val jdbcUrl = requiredEnv("DB_URL")
            val username = requiredEnv("DB_USERNAME")
            val password = requiredEnv("DB_PASSWORD")

            return AppConfig(
                jdbcUrl = jdbcUrl,
                username = username,
                password = password,
            )
        }

        private fun requiredEnv(name: String): String =
            System.getenv(name)?.takeIf(String::isNotBlank)
                ?: error(
                    "Missing $name. In PowerShell, set it with: " +
                        "[Environment]::SetEnvironmentVariable(\"$name\", \"...\", \"User\")",
                )
    }
}
