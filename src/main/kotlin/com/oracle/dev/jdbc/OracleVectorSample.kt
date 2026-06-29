package com.oracle.dev.jdbc

import org.jetbrains.exposed.v1.core.QueryParameter
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.functions.vector.VectorDistance
import org.jetbrains.exposed.v1.core.functions.vector.VectorDistanceMetric
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

object OracleVectorSample {
    private val seedDocuments = listOf(
        DocumentSeed(
            title = "Oracle AI Database 26ai",
            category = "database",
            body = "Oracle AI Database stores vector embeddings next to relational data.",
            embedding = floatArrayOf(0.92f, 0.10f, 0.18f, 0.05f),
            qualitySignals = intArrayOf(9, 2, 2, 1),
            preciseEmbedding = floatArrayOf(0.9201f, 0.1002f, 0.1803f, 0.0504f),
        ),
        DocumentSeed(
            title = "Kotlin Exposed",
            category = "kotlin",
            body = "Exposed maps Kotlin tables and SQL expressions with a type-safe DSL.",
            embedding = floatArrayOf(0.08f, 0.94f, 0.20f, 0.11f),
            qualitySignals = intArrayOf(1, 9, 3, 2),
            preciseEmbedding = floatArrayOf(0.0801f, 0.9402f, 0.2003f, 0.1104f),
        ),
        DocumentSeed(
            title = "Vector Search",
            category = "database",
            body = "Similarity search compares embeddings using cosine, Euclidean, or dot product distance.",
            embedding = floatArrayOf(0.86f, 0.16f, 0.35f, 0.08f),
            qualitySignals = intArrayOf(8, 2, 5, 1),
            preciseEmbedding = floatArrayOf(0.8601f, 0.1602f, 0.3503f, 0.0804f),
        ),
        DocumentSeed(
            title = "Connection Pooling",
            category = "jdbc",
            body = "Oracle UCP gives JDBC applications a production-ready connection pool.",
            embedding = floatArrayOf(0.13f, 0.30f, 0.12f, 0.93f),
            qualitySignals = intArrayOf(2, 5, 2, 9),
            preciseEmbedding = floatArrayOf(0.1301f, 0.3002f, 0.1203f, 0.9304f),
        ),
    )

    fun recreateSchemaAndSeed() = transaction {
        SchemaUtils.drop(Documents)
        SchemaUtils.create(Documents)

        Documents.batchInsert(seedDocuments) { document ->
            this[Documents.title] = document.title
            this[Documents.category] = document.category
            this[Documents.body] = document.body
            this[Documents.embedding] = document.embedding
            this[Documents.qualitySignals] = document.qualitySignals
            this[Documents.preciseEmbedding] = document.preciseEmbedding
        }

        try {
            exec(
                """
                CREATE VECTOR INDEX EXPOSED_DOCS_EMBEDDING_HNSW_IDX
                ON EXPOSED_VECTOR_DOCUMENTS (EMBEDDING)
                ORGANIZATION INMEMORY NEIGHBOR GRAPH
                DISTANCE COSINE
                """.trimIndent(),
            )
        } catch (exception: ExposedSQLException) {
            println(
                "Skipping optional HNSW vector index creation: " +
                    (exception.cause?.message ?: exception.message),
            )
        }
    }

    fun updateVectorColumn() = transaction {
        Documents.update({ Documents.title eq "Connection Pooling" }) {
            it[embedding] = floatArrayOf(0.20f, 0.35f, 0.14f, 0.90f)
            it[qualitySignals] = intArrayOf(3, 5, 2, 9)
            it[preciseEmbedding] = floatArrayOf(0.2001f, 0.3502f, 0.1403f, 0.9004f)
        }
    }

    fun nearestByCosine(queryEmbedding: FloatArray): List<VectorMatch> = transaction {
        val distance = vectorDistance(queryEmbedding, VectorDistanceMetric.COSINE).alias("COSINE_DISTANCE")

        Documents
            .select(Documents.id, Documents.title, Documents.category, distance)
            .orderBy(distance to SortOrder.ASC)
            .limit(3)
            .map {
                VectorMatch(
                    id = it[Documents.id],
                    title = it[Documents.title],
                    category = it[Documents.category],
                    distance = it[distance],
                )
            }
    }

    fun databaseDocumentsNear(queryEmbedding: FloatArray): List<VectorMatch> = transaction {
        val distanceExpression = vectorDistance(queryEmbedding, VectorDistanceMetric.EUCLIDEAN)
        val distance = distanceExpression.alias("EUCLIDEAN_DISTANCE")

        Documents
            .select(Documents.id, Documents.title, Documents.category, distance)
            .where {
                (Documents.category eq "database") and (distanceExpression less 0.35)
            }
            .orderBy(distance to SortOrder.ASC)
            .map {
                VectorMatch(
                    id = it[Documents.id],
                    title = it[Documents.title],
                    category = it[Documents.category],
                    distance = it[distance],
                )
            }
    }

    fun rankedByDotProduct(queryEmbedding: FloatArray): List<VectorMatch> = transaction {
        val distanceExpression = vectorDistance(queryEmbedding, VectorDistanceMetric.DOT)
        val distance = distanceExpression.alias("DOT_PRODUCT_DISTANCE")

        Documents
            .select(Documents.id, Documents.title, Documents.category, distance)
            .where { distanceExpression greater -1.0 }
            .orderBy(distance to SortOrder.ASC)
            .limit(3)
            .map {
                VectorMatch(
                    id = it[Documents.id],
                    title = it[Documents.title],
                    category = it[Documents.category],
                    distance = it[distance],
                )
            }
    }

    fun readBackStoredArrays(): List<StoredVector> = transaction {
        Documents
            .selectAll()
            .orderBy(Documents.id to SortOrder.ASC)
            .map {
                StoredVector(
                    title = it[Documents.title],
                    embedding = it[Documents.embedding],
                    qualitySignals = it[Documents.qualitySignals],
                    preciseEmbedding = it[Documents.preciseEmbedding],
                )
            }
    }

    private fun vectorDistance(
        queryEmbedding: FloatArray,
        metric: VectorDistanceMetric,
    ): VectorDistance<FloatArray> = VectorDistance(
        expression = Documents.embedding,
        targetExpression = QueryParameter(queryEmbedding, Documents.embedding.columnType),
        metric = metric,
    )
}

data class DocumentSeed(
    val title: String,
    val category: String,
    val body: String,
    val embedding: FloatArray,
    val qualitySignals: IntArray,
    val preciseEmbedding: FloatArray,
)

data class VectorMatch(
    val id: Int,
    val title: String,
    val category: String,
    val distance: Double,
)

data class StoredVector(
    val title: String,
    val embedding: FloatArray,
    val qualitySignals: IntArray,
    val preciseEmbedding: FloatArray,
)



