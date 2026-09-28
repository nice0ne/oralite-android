package com.msam.oralite.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msam.oralite.data.model.ConnectionProfile
import com.msam.oralite.data.model.ConnectionType
import com.msam.oralite.data.model.SavedQuery
import com.msam.oralite.ui.viewmodel.MainViewModel

@Composable
fun ProfileDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val profiles by viewModel.profiles.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    var isAdding by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ConnectionType.DIRECT) }
    var host by remember { mutableStateOf("192.168.1.1") }
    var port by remember { mutableStateOf("1521") }
    var service by remember { mutableStateOf("ORCL") }
    var user by remember { mutableStateOf("system") }
    var pass by remember { mutableStateOf("") }
    var restUrl by remember { mutableStateOf("http://192.168.1.1:8192/api/query") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isAdding) "New Connection" else "Select Connection Profile") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                if (isAdding) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Profile Name") }, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        FilterChip(selected = type == ConnectionType.DIRECT, onClick = { type = ConnectionType.DIRECT }, label = { Text("Direct 1521") })
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(selected = type == ConnectionType.REST, onClick = { type = ConnectionType.REST }, label = { Text("REST API") })
                    }
                    if (type == ConnectionType.DIRECT) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Host") }, modifier = Modifier.weight(2f))
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("Port") }, modifier = Modifier.weight(1f))
                        }
                        OutlinedTextField(value = service, onValueChange = { service = it }, label = { Text("SID / Service") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = pass, onValueChange = { pass = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth())
                    } else {
                        OutlinedTextField(value = restUrl, onValueChange = { restUrl = it }, label = { Text("REST Middleware URL") }, modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(profiles) { p ->
                            val isSelected = p.id == activeProfile?.id
                            ListItem(
                                headlineContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(p.name, fontWeight = FontWeight.Bold)
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "ACTIVE",
                                                color = Color(0xFF2EA043),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                },
                                supportingContent = { Text(if (p.type == ConnectionType.DIRECT) "${p.host}:${p.port}/${p.serviceOrSid}" else p.restUrl) },
                                trailingContent = {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = "Active", tint = Color(0xFF2EA043))
                                    }
                                },
                                modifier = Modifier.clickable {
                                    viewModel.setActiveProfile(p)
                                    onDismiss()
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isAdding) {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        viewModel.saveProfile(
                            ConnectionProfile(
                                name = name,
                                type = type,
                                host = host,
                                port = port.toIntOrNull() ?: 1521,
                                serviceOrSid = service,
                                username = user,
                                password = pass,
                                restUrl = restUrl
                            )
                        )
                        isAdding = false
                    }
                }) { Text("Save") }
            } else {
                TextButton(onClick = { isAdding = true }) { Text("+ Add Profile") }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (isAdding) isAdding = false else onDismiss() }) { Text("Close") }
        }
    )
}

@Composable
fun HistoryDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val history by viewModel.history.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Query History") },
        text = {
            if (history.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No query history yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                    items(history) { h ->
                        ListItem(
                            headlineContent = { Text(h.sqlText, fontFamily = FontFamily.Monospace, fontSize = 12.sp, maxLines = 2) },
                            supportingContent = { Text("${h.status} • ${h.durationMs}ms • ${h.rowCount} rows", fontSize = 11.sp) },
                            modifier = Modifier.clickable {
                                viewModel.setSql(h.sqlText)
                                onDismiss()
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun SaveQueryDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val currentSql by viewModel.sqlText.collectAsState()
    var title by remember {
        mutableStateOf(
            currentSql.trim().take(30).replace("\n", " ").ifEmpty { "My Query" }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Query", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Save current SQL for quick access:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Query Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = currentSql.take(150),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(8.dp),
                        maxLines = 4,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                viewModel.saveCurrentQuery(title)
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SavedQueriesDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val savedQueries by viewModel.savedQueries.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Saved Queries", fontWeight = FontWeight.Bold) },
        text = {
            if (savedQueries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No saved queries yet.\nTap 'SAVE' in the editor to bookmark queries.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                    items(savedQueries) { q ->
                        ListItem(
                            headlineContent = { Text(q.title, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                            supportingContent = {
                                Text(
                                    q.sqlText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = { viewModel.deleteSavedQuery(q) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            modifier = Modifier.clickable {
                                viewModel.setSql(q.sqlText)
                                onDismiss()
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
