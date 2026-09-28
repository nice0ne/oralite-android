package com.msam.oralite.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ConnectionType { DIRECT, REST }

@Entity(tableName = "connection_profiles")
data class ConnectionProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: ConnectionType = ConnectionType.DIRECT,
    val host: String = "localhost",
    val port: Int = 1521,
    val serviceOrSid: String = "ORCL",
    val username: String = "",
    val password: String = "",
    val restUrl: String = "",
    val isDefault: Boolean = false
)

@Entity(tableName = "saved_queries")
data class SavedQuery(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sqlText: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "query_history")
data class QueryHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sqlText: String,
    val durationMs: Long,
    val rowCount: Int,
    val status: String, // SUCCESS or ERROR
    val errorMessage: String? = null,
    val executedAt: Long = System.currentTimeMillis()
)

data class QueryResult(
    val columns: List<String> = emptyList(),
    val rows: List<Map<String, String>> = emptyList(),
    val durationMs: Long = 0,
    val errorMessage: String? = null,
    val isSuccess: Boolean = true
)
