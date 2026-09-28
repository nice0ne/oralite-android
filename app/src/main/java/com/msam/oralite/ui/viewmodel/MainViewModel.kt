package com.msam.oralite.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.msam.oralite.data.engine.DirectJdbcEngine
import com.msam.oralite.data.engine.RestApiEngine
import com.msam.oralite.data.local.AppDatabase
import com.msam.oralite.data.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("oralite_prefs", Context.MODE_PRIVATE)
    private val db = AppDatabase.getDatabase(application)
    private val profileDao = db.profileDao()
    private val historyDao = db.historyDao()
    private val savedQueryDao = db.savedQueryDao()

    val profiles = profileDao.getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val history = historyDao.getRecent().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val savedQueries = savedQueryDao.getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProfile = MutableStateFlow<ConnectionProfile?>(null)
    val sqlText = MutableStateFlow(
        prefs.getString("last_sql", null) ?: "SELECT 'Hello Oracle' AS message, SYSDATE FROM dual"
    )
    val queryResult = MutableStateFlow<QueryResult?>(null)
    val isLoading = MutableStateFlow(false)
    val isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_theme", false))
    val isAutosaved = MutableStateFlow(true)

    init {
        viewModelScope.launch {
            // Auto-migrate legacy IP preset if present in existing DB
            profileDao.updateHostIp("172.20.35.103", "192.168.1.1")
            profileDao.updateRestUrl("172.20.35.103", "192.168.1.1")

            if (profileDao.count() == 0) {
                // Seed default profiles: REST (recommended) + Direct TCP
                val restDemo = ConnectionProfile(
                    name = "Marketing REST (Middleware)",
                    type = ConnectionType.REST,
                    restUrl = "http://192.168.1.1:8192/api/query",
                    isDefault = true
                )
                val directDemo = ConnectionProfile(
                    name = "Direct Oracle (1521 TCP)",
                    type = ConnectionType.DIRECT,
                    host = "192.168.1.1",
                    port = 1521,
                    serviceOrSid = "ORCL",
                    username = "scott",
                    password = "tiger",
                    isDefault = false
                )
                val id1 = profileDao.insert(restDemo)
                profileDao.insert(directDemo)
                if (prefs.getLong("last_profile_id", -1L) == -1L) {
                    prefs.edit().putLong("last_profile_id", id1).apply()
                }
            }

            profiles.collect { list ->
                if (list.isNotEmpty()) {
                    val lastProfileId = prefs.getLong("last_profile_id", -1L)
                    val matched = if (lastProfileId != -1L) list.firstOrNull { it.id == lastProfileId } else null
                    activeProfile.value = matched ?: list.firstOrNull { it.isDefault } ?: list.first()
                }
            }
        }
    }

    fun setSql(text: String) {
        sqlText.value = text
        prefs.edit().putString("last_sql", text).apply()
        isAutosaved.value = true
    }

    fun appendSql(keyword: String) {
        val current = sqlText.value
        val updated = if (current.isEmpty()) keyword else "$current $keyword"
        setSql(updated)
    }

    fun setActiveProfile(profile: ConnectionProfile) {
        activeProfile.value = profile
        prefs.edit().putLong("last_profile_id", profile.id).apply()
    }

    fun toggleTheme() {
        val next = !isDarkTheme.value
        isDarkTheme.value = next
        prefs.edit().putBoolean("is_dark_theme", next).apply()
    }

    fun saveCurrentQuery(title: String) {
        val current = sqlText.value.trim()
        if (current.isEmpty()) return
        val finalTitle = title.trim().ifEmpty {
            current.take(30).replace("\n", " ")
        }
        viewModelScope.launch {
            savedQueryDao.insert(SavedQuery(title = finalTitle, sqlText = current))
        }
    }

    fun deleteSavedQuery(query: SavedQuery) {
        viewModelScope.launch {
            savedQueryDao.delete(query)
        }
    }

    fun saveProfile(profile: ConnectionProfile) {
        viewModelScope.launch {
            val id = profileDao.insert(profile)
            val updated = profile.copy(id = id)
            setActiveProfile(updated)
        }
    }

    fun runQuery() {
        val profile = activeProfile.value ?: return
        val sql = sqlText.value.trim()
        if (sql.isEmpty()) return

        isLoading.value = true
        viewModelScope.launch {
            try {
                val engine = when (profile.type) {
                    ConnectionType.DIRECT -> DirectJdbcEngine(profile)
                    ConnectionType.REST -> RestApiEngine(profile)
                }

                val result = engine.executeQuery(sql)
                queryResult.value = result

                // Record to history
                historyDao.insert(
                    QueryHistory(
                        sqlText = sql,
                        durationMs = result.durationMs,
                        rowCount = result.rows.size,
                        status = if (result.isSuccess) "SUCCESS" else "ERROR",
                        errorMessage = result.errorMessage
                    )
                )
            } catch (t: Throwable) {
                queryResult.value = QueryResult(
                    durationMs = 0,
                    errorMessage = t.localizedMessage ?: t.javaClass.simpleName,
                    isSuccess = false
                )
            } finally {
                isLoading.value = false
            }
        }
    }
}
