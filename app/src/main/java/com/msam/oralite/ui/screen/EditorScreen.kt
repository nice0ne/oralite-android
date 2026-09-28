package com.msam.oralite.ui.screen

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msam.oralite.data.model.ConnectionProfile
import com.msam.oralite.data.model.ConnectionType
import com.msam.oralite.data.model.QueryResult
import com.msam.oralite.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: MainViewModel,
    onOpenProfiles: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val activeProfile by viewModel.activeProfile.collectAsState()
    val sqlText by viewModel.sqlText.collectAsState()
    val queryResult by viewModel.queryResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    val isAutosaved by viewModel.isAutosaved.collectAsState()
    val profiles by viewModel.profiles.collectAsState()

    var isEditorCollapsed by remember { mutableStateOf(false) }
    var isUppercaseMode by remember { mutableStateOf(true) }
    var isSyntaxHighlightEnabled by remember { mutableStateOf(true) }
    var editorHeightDp by remember { mutableStateOf(220) }
    var selectedRow by remember { mutableStateOf<Map<String, String>?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showSavedQueriesDialog by remember { mutableStateOf(false) }
    var showProfileDropdown by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Palette Colors
    val editorBg = if (isDark) Color(0xFF0D1117) else Color(0xFFFFFFFF)
    val gutterBg = if (isDark) Color(0xFF080C10) else Color(0xFFF6F8FA)
    val gutterText = if (isDark) Color(0xFF6E7681) else Color(0xFF8C959F)
    val gutterBorder = if (isDark) Color(0xFF21262D) else Color(0xFFE1E4E8)
    val editorBorder = if (isDark) Color(0xFF30363D) else Color(0xFFD0D7DE)
    val editorText = if (isDark) Color(0xFFE6EDF3) else Color(0xFF1F2328)
    val cursorColor = if (isDark) Color(0xFF58A6FF) else Color(0xFF0969DA)
    val headerBg = if (isDark) Color(0xFF161B22) else Color(0xFFF6F8FA)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showProfileDropdown = true }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(if (activeProfile != null) Color(0xFF2EA043) else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = activeProfile?.name ?: "No Profile",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                activeProfile?.let { p ->
                                    Text(
                                        text = if (p.type == ConnectionType.DIRECT) "DIRECT TCP ${p.host}:${p.port}/${p.serviceOrSid}" else "REST API ${p.restUrl}",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = showProfileDropdown,
                            onDismissRequest = { showProfileDropdown = false }
                        ) {
                            profiles.forEach { p ->
                                val isSelected = p.id == activeProfile?.id
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    p.name,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 13.sp
                                                )
                                                if (isSelected) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        "ACTIVE",
                                                        color = Color(0xFF2EA043),
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                            Text(
                                                text = if (p.type == ConnectionType.DIRECT) "TCP ${p.host}:${p.port}/${p.serviceOrSid}" else p.restUrl,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) Color(0xFF2EA043) else Color.Gray)
                                        )
                                    },
                                    onClick = {
                                        viewModel.setActiveProfile(p)
                                        showProfileDropdown = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("+ Manage Profiles...", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    showProfileDropdown = false
                                    onOpenProfiles()
                                }
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showSavedQueriesDialog = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Bookmarks, contentDescription = "Saved Queries", modifier = Modifier.size(19.dp))
                    }
                    IconButton(onClick = { viewModel.toggleTheme() }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    IconButton(onClick = onOpenHistory, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.History, contentDescription = "History", modifier = Modifier.size(19.dp))
                    }
                    IconButton(onClick = onOpenProfiles, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = "Profiles", modifier = Modifier.size(19.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Collapsible SQL Editor Section
            if (!isEditorCollapsed) {
                val lineCount = (sqlText.count { it == '\n' } + 1).coerceAtLeast(1)
                val charCount = sqlText.length

                // Editor Box Container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(editorBg)
                        .border(1.dp, editorBorder, RoundedCornerShape(8.dp))
                ) {
                    // Integrated Top Sub-Header Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(headerBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Status & Line Info
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isAutosaved) {
                                Icon(
                                    Icons.Default.CloudDone,
                                    contentDescription = "Autosaved",
                                    modifier = Modifier.size(13.dp),
                                    tint = Color(0xFF2EA043)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            Text(
                                text = "${lineCount}L, ${charCount}C",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Right: Quick Actions
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Syntax Color Toggle
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSyntaxHighlightEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .height(24.dp)
                                    .clickable { isSyntaxHighlightEnabled = !isSyntaxHighlightEnabled }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Palette,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = if (isSyntaxHighlightEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        "COLOR",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSyntaxHighlightEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // CAPS Toggle
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isUppercaseMode) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .height(24.dp)
                                    .clickable {
                                        isUppercaseMode = !isUppercaseMode
                                        if (isUppercaseMode) viewModel.setSql(sqlText.uppercase())
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardCapslock,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = if (isUppercaseMode) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        "CAPS",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUppercaseMode) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Format SQL
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .height(24.dp)
                                    .clickable {
                                        val formatted = SqlFormatter.format(sqlText)
                                        viewModel.setSql(if (isUppercaseMode) formatted.uppercase() else formatted)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("FORMAT", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Save Query
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .height(24.dp)
                                    .clickable { showSaveDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.BookmarkAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        "SAVE",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            // Height Toggle
                            IconButton(
                                onClick = { editorHeightDp = if (editorHeightDp == 220) 340 else 220 },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (editorHeightDp == 220) Icons.Default.Height else Icons.Default.VerticalAlignTop,
                                    contentDescription = "Toggle Height",
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Clear
                            if (sqlText.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.setSql("") },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = editorBorder)

                    // Text Input Area with Pixel-Perfect Gutter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(editorHeightDp.dp)
                    ) {
                        // Gutter with Line Numbers
                        Column(
                            modifier = Modifier
                                .width(34.dp)
                                .fillMaxHeight()
                                .background(gutterBg)
                                .border(BorderStroke(0.5.dp, gutterBorder))
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            for (i in 1..lineCount.coerceAtMost(60)) {
                                Text(
                                    text = "$i ",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = gutterText,
                                    lineHeight = 20.sp
                                )
                            }
                        }

                        // BasicTextField with Matching LineHeight & Padding
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            BasicTextField(
                                value = sqlText,
                                onValueChange = { input ->
                                    val text = if (isUppercaseMode) input.uppercase() else input
                                    viewModel.setSql(text)
                                },
                                modifier = Modifier.fillMaxSize(),
                                visualTransformation = if (isSyntaxHighlightEnabled) SqlSyntaxVisualTransformation(isDark) else VisualTransformation.None,
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = editorText
                                ),
                                cursorBrush = SolidColor(cursorColor),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = if (isUppercaseMode) KeyboardCapitalization.Characters else KeyboardCapitalization.None,
                                    autoCorrectEnabled = false
                                ),
                                decorationBox = { innerTextField ->
                                    if (sqlText.isEmpty()) {
                                        Text(
                                            "SELECT * FROM DUAL",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp,
                                            lineHeight = 20.sp,
                                            color = if (isDark) Color(0xFF8B949E) else Color(0xFF6E7681)
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }
                }

                // Quick SQL Chips Bar (Padding prevents first chip from clipping)
                val chips = listOf(
                    "SELECT" to Color(0xFF0969DA),
                    "FROM" to Color(0xFF0969DA),
                    "WHERE" to Color(0xFF0969DA),
                    "AND" to Color(0xFF8250DF),
                    "OR" to Color(0xFF8250DF),
                    "ORDER BY" to Color(0xFF0969DA),
                    "GROUP BY" to Color(0xFF0969DA),
                    "ROWNUM <= 50" to Color(0xFF1A7F37),
                    "COUNT(*)" to Color(0xFF1A7F37),
                    "SYSDATE" to Color(0xFF1A7F37),
                    "DUAL" to Color(0xFF9A6700),
                    "*" to Color(0xFFCF222E),
                    "=" to Color(0xFFCF222E),
                    "<=" to Color(0xFFCF222E),
                    ";" to Color(0xFFCF222E),
                    "," to Color(0xFFCF222E),
                    "'" to Color(0xFFCF222E)
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(chips) { (chip, dotColor) ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            modifier = Modifier.clickable {
                                val kw = if (isUppercaseMode) chip.uppercase() else chip
                                viewModel.appendSql(kw)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = chip,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Execution Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // RUN Button
                Button(
                    onClick = { viewModel.runQuery() },
                    enabled = !isLoading && activeProfile != null,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(13.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RUNNING", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("RUN", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }

                // Status Badges & Collapse Toggle
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    activeProfile?.let { p ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (p.type == ConnectionType.DIRECT) Color(0xFF1F6FEB).copy(alpha = 0.10f) else Color(0xFF238636).copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, if (p.type == ConnectionType.DIRECT) Color(0xFF1F6FEB).copy(alpha = 0.35f) else Color(0xFF238636).copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = if (p.type == ConnectionType.DIRECT) "TCP:1521" else "REST API",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (p.type == ConnectionType.DIRECT) Color(0xFF1F6FEB) else Color(0xFF238636)
                            )
                        }
                    }

                    queryResult?.let { res ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (res.isSuccess) Color(0xFF2EA043).copy(alpha = 0.10f) else MaterialTheme.colorScheme.errorContainer,
                            border = BorderStroke(0.5.dp, if (res.isSuccess) Color(0xFF2EA043).copy(alpha = 0.35f) else MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
                        ) {
                            val rowUnit = if (res.rows.size == 1) "row" else "rows"
                            Text(
                                text = if (res.isSuccess) "${res.rows.size} $rowUnit • ${res.durationMs}ms" else "ERROR",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = if (res.isSuccess) Color(0xFF2EA043) else MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    IconButton(
                        onClick = { isEditorCollapsed = !isEditorCollapsed },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isEditorCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = "Collapse Editor",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            HorizontalDivider()

            // Result Area
            Box(modifier = Modifier.fillMaxSize()) {
                queryResult?.let { res ->
                    if (!res.isSuccess) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Oracle SQL Error",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 13.sp
                                    )
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(res.errorMessage ?: "Unknown error"))
                                            Toast.makeText(context, "Error copied", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Copy Error",
                                            modifier = Modifier.size(15.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    res.errorMessage ?: "Unknown error",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    } else if (res.columns.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "No records returned",
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    } else {
                        // Dynamic column width calculation to prevent header and cell wrapping
                        val colWidths = remember(res) {
                            res.columns.associateWith { col ->
                                val maxValLen = res.rows.take(50).maxOfOrNull { it[col]?.length ?: 0 } ?: 0
                                val charCount = maxOf(col.length, maxValLen)
                                (charCount * 9 + 32).coerceIn(100, 320).dp
                            }
                        }

                        val horizontalScroll = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .horizontalScroll(horizontalScroll)
                        ) {
                            // Header Row
                            Row(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "#",
                                    modifier = Modifier.width(44.dp).padding(horizontal = 6.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                res.columns.forEach { col ->
                                    val w = colWidths[col] ?: 140.dp
                                    Text(
                                        text = col,
                                        modifier = Modifier.width(w).padding(horizontal = 8.dp),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                            HorizontalDivider()

                            // Data Rows
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                itemsIndexed(res.rows) { idx, row ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedRow = row }
                                            .background(
                                                if (idx % 2 == 0) MaterialTheme.colorScheme.surface
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                            )
                                            .padding(vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "${idx + 1}",
                                            modifier = Modifier.width(44.dp).padding(horizontal = 6.dp),
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        res.columns.forEach { col ->
                                            val w = colWidths[col] ?: 140.dp
                                            val value = row[col] ?: "NULL"
                                            val isNull = row[col] == null
                                            Text(
                                                text = value,
                                                modifier = Modifier.width(w).padding(horizontal = 8.dp),
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 1,
                                                softWrap = false,
                                                color = if (isNull) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f) else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                }
                            }
                        }
                    }
                } ?: run {
                    // Empty State with Quick Starter Queries
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "SQL Editor Ready",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tap RUN or execute starter query:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val starters = listOf(
                            "SELECT 'HELLO ORACLE' AS MSG, SYSDATE FROM DUAL",
                            "SELECT TABLE_NAME FROM USER_TABLES WHERE ROWNUM <= 10",
                            "SELECT USER FROM DUAL"
                        )
                        starters.forEach { query ->
                            OutlinedButton(
                                onClick = {
                                    viewModel.setSql(query)
                                    viewModel.runQuery()
                                },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = query,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Save Query Dialog
    if (showSaveDialog) {
        SaveQueryDialog(
            viewModel = viewModel,
            onDismiss = { showSaveDialog = false }
        )
    }

    // Saved Queries Dialog
    if (showSavedQueriesDialog) {
        SavedQueriesDialog(
            viewModel = viewModel,
            onDismiss = { showSavedQueriesDialog = false }
        )
    }

    // Row Detail Bottom Sheet (Full Monospace Inspector)
    selectedRow?.let { row ->
        ModalBottomSheet(onDismissRequest = { selectedRow = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "ROW DETAIL",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    TextButton(onClick = {
                        val rowTsv = row.entries.joinToString("\n") { "${it.key}: ${it.value}" }
                        clipboardManager.setText(AnnotatedString(rowTsv))
                        Toast.makeText(context, "Row copied", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("COPY ALL", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    items(row.entries.toList()) { (k, v) ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(v))
                                    Toast.makeText(context, "$k copied", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 6.dp)
                        ) {
                            Text(k, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text(v, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            HorizontalDivider(modifier = Modifier.padding(top = 4.dp), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}
