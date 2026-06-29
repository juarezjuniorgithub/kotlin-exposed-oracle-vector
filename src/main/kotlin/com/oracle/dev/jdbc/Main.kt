package com.oracle.dev.jdbc

fun main() {
    val config = AppConfig.fromEnvironment()
    val pool = DatabaseFactory.connect(config)

    try {
        OracleVectorSample.recreateSchemaAndSeed()
        OracleVectorSample.updateVectorColumn()

        val databaseQuery = floatArrayOf(0.88f, 0.12f, 0.22f, 0.06f)

        println("Stored vectors")
        OracleVectorSample.readBackStoredArrays().forEach { stored ->
            println(
                "${stored.title}: " +
                    "embedding=${stored.embedding.contentToString()}, " +
                    "qualitySignals=${stored.qualitySignals.contentToString()}, " +
                    "preciseEmbedding=${stored.preciseEmbedding.contentToString()}",
            )
        }

        println()
        printMatches("Nearest by cosine", OracleVectorSample.nearestByCosine(databaseQuery))
        printMatches("Database rows within Euclidean threshold", OracleVectorSample.databaseDocumentsNear(databaseQuery))
        printMatches("Ranked by dot product distance", OracleVectorSample.rankedByDotProduct(databaseQuery))
    } finally {
        DatabaseFactory.destroyPool(pool)
    }
}

private fun printMatches(label: String, matches: List<VectorMatch>) {
    println(label)
    matches.forEach { match ->
        println(
            "#${match.id} ${match.title} [${match.category}] distance=${"%.6f".format(match.distance)}",
        )
    }
    println()
}
