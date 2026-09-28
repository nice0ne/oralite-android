package com.msam.oralite.data.engine

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.msam.oralite.data.model.ConnectionProfile
import com.msam.oralite.data.model.QueryResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.concurrent.TimeUnit

interface DatabaseEngine {
    suspend fun executeQuery(sql: String, limit: Int = 100): QueryResult
    suspend fun testConnection(): Pair<Boolean, String>
}

object SqlHelper {
    fun injectSafetyLimit(sql: String, limit: Int = 100): String {
        // ponytail: regex boundary check; upgrade to SQL AST parser if complex CTE/subqueries used
        val clean = sql.trim().trimEnd(';')
        val upper = clean.uppercase()
        if (!upper.startsWith("SELECT")) return sql
        if (upper.contains("ROWNUM") || upper.contains("FETCH FIRST") || upper.contains("FETCH NEXT")) {
            return sql
        }
        return "SELECT * FROM ($clean) WHERE ROWNUM <= $limit"
    }
}

class DirectJdbcEngine(private val profile: ConnectionProfile) : DatabaseEngine {
    init {
        try {
            Class.forName("oracle.jdbc.OracleDriver")
        } catch (_: Throwable) {
            try {
                DriverManager.registerDriver(oracle.jdbc.OracleDriver())
            } catch (_: Throwable) {}
        }
    }

    private fun openConnection(): Connection {
        val props = java.util.Properties().apply {
            put("user", profile.username)
            put("password", profile.password)
        }
        val cleanService = profile.serviceOrSid.trimStart('/', ':')
        val urls = listOf(
            "jdbc:oracle:thin:@${profile.host}:${profile.port}/$cleanService",
            "jdbc:oracle:thin:@${profile.host}:${profile.port}:$cleanService",
            "jdbc:oracle:thin:@//${profile.host}:${profile.port}/$cleanService"
        )

        var lastError: Throwable? = null
        for (url in urls) {
            try {
                return DriverManager.getConnection(url, profile.username, profile.password)
            } catch (e: Throwable) {
                lastError = e
                try {
                    val driver = oracle.jdbc.OracleDriver()
                    val conn = driver.connect(url, props)
                    if (conn != null) return conn
                } catch (dE: Throwable) {
                    lastError = dE
                }
            }
        }
        throw (lastError ?: SQLException("Failed to connect to Oracle at ${profile.host}:${profile.port}"))
    }

    override suspend fun testConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            DriverManager.setLoginTimeout(8)
            openConnection().use { conn ->
                Pair(conn.isValid(5), "Connected to Oracle")
            }
        } catch (e: Throwable) {
            val msg = if (e is NoClassDefFoundError || e is ClassNotFoundException) {
                "Oracle Direct JDBC tidak didukung runtime Android (${e.javaClass.simpleName}: ${e.message}). Silakan gunakan koneksi mode REST API."
            } else {
                e.localizedMessage ?: "Connection failed"
            }
            Pair(false, msg)
        }
    }

    override suspend fun executeQuery(sql: String, limit: Int): QueryResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val safeSql = SqlHelper.injectSafetyLimit(sql, limit)

        try {
            DriverManager.setLoginTimeout(10)
            openConnection().use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.queryTimeout = 15
                    val isResultSet = stmt.execute(safeSql)

                    if (isResultSet) {
                        val rs = stmt.resultSet
                        val meta = rs.metaData
                        val colCount = meta.columnCount
                        val columns = (1..colCount).map { meta.getColumnLabel(it) ?: meta.getColumnName(it) }

                        val rows = mutableListOf<Map<String, String>>()
                        while (rs.next()) {
                            val row = mutableMapOf<String, String>()
                            for (i in 1..colCount) {
                                val col = columns[i - 1]
                                row[col] = rs.getString(i) ?: "NULL"
                            }
                            rows.add(row)
                        }

                        val duration = System.currentTimeMillis() - start
                        QueryResult(columns = columns, rows = rows, durationMs = duration, isSuccess = true)
                    } else {
                        val duration = System.currentTimeMillis() - start
                        val affected = stmt.updateCount
                        QueryResult(
                            columns = listOf("AFFECTED_ROWS"),
                            rows = listOf(mapOf("AFFECTED_ROWS" to affected.toString())),
                            durationMs = duration,
                            isSuccess = true
                        )
                    }
                }
            }
        } catch (e: Throwable) {
            val duration = System.currentTimeMillis() - start
            val errMsg = if (e is NoClassDefFoundError || e is ClassNotFoundException) {
                "Oracle Direct JDBC tidak didukung runtime Android (${e.javaClass.simpleName}: ${e.message}). Gunakan mode koneksi REST API Middleware."
            } else {
                e.localizedMessage ?: "Unknown SQL error"
            }
            QueryResult(
                durationMs = duration,
                errorMessage = errMsg,
                isSuccess = false
            )
        }
    }
}

class RestApiEngine(private val profile: ConnectionProfile) : DatabaseEngine {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    override suspend fun testConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(profile.restUrl).head().build()
            client.newCall(req).execute().use { resp ->
                Pair(resp.isSuccessful, "HTTP ${resp.code}")
            }
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "REST ping failed")
        }
    }

    override suspend fun executeQuery(sql: String, limit: Int): QueryResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val safeSql = SqlHelper.injectSafetyLimit(sql, limit)

        try {
            val payload = gson.toJson(mapOf("sql" to safeSql))
            val req = Request.Builder()
                .url(profile.restUrl)
                .post(payload.toRequestBody(jsonType))
                .build()

            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string() ?: ""
                val duration = System.currentTimeMillis() - start
                if (!resp.isSuccessful) {
                    return@withContext QueryResult(
                        durationMs = duration,
                        errorMessage = "HTTP ${resp.code}: $body",
                        isSuccess = false
                    )
                }

                // Parser payload: [{col1: val1, ...}]
                val listType = object : TypeToken<List<Map<String, Any?>>>() {}.type
                val rawRows: List<Map<String, Any?>> = gson.fromJson(body, listType) ?: emptyList()
                val columns = rawRows.firstOrNull()?.keys?.toList() ?: emptyList()
                val rows = rawRows.map { r -> r.mapValues { it.value?.toString() ?: "NULL" } }

                QueryResult(columns = columns, rows = rows, durationMs = duration, isSuccess = true)
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - start
            QueryResult(
                durationMs = duration,
                errorMessage = e.localizedMessage ?: "REST execution failed",
                isSuccess = false
            )
        }
    }
}
