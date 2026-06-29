package com.oracle.dev.jdbc

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.VectorFormat

object Documents : Table("EXPOSED_VECTOR_DOCUMENTS") {
    val id = integer("ID").autoIncrement()
    val title = varchar("TITLE", length = 120)
    val category = varchar("CATEGORY", length = 40)
    val body = varchar("BODY", length = 1000)
    val embedding = vector("EMBEDDING", dimensions = 4)
    val qualitySignals = vector<IntArray>("QUALITY_SIGNALS", dimensions = 4)
    val preciseEmbedding = vector<FloatArray>(
        name = "PRECISE_EMBEDDING",
        dimensions = 4,
        format = VectorFormat.FLOAT64,
    )

    override val primaryKey = PrimaryKey(id, name = "PK_EXPOSED_VECTOR_DOCUMENTS")
}
