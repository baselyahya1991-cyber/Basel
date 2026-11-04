package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AlarmItem
import com.example.ui.components.AlarmCard
import com.example.ui.components.AlarmEditDialog
import com.example.ui.components.AlarmRingingOverlay
import com.example.ui.components.LiveClockHeader
import com.example.ui.components.QuickNapSection
import com.example.ui.viewmodel.AlarmViewModel
import kotlinx.coroutines.launch

@Composable
fun AlarmMainScreen(
    viewModel: AlarmViewModel,
    modifier: Modifier = Modifier
) {
    val alarms by viewModel.alarms.collectAsState()
    val formattedTime by viewModel.formattedCurrentTime.collectAsState()
    val amPm by viewModel.currentAmPm.collectAsState()
    val dateFormatted by viewModel.currentDateFormatted.collectAsState()
    val nextAlarmCountdown by viewModel.nextAlarmCountdown.collectAsState()
    val ringingState by viewModel.ringingState.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var selectedAlarmForEdit by remember { mutableStateOf<AlarmItem?>(null) }
    var alarmToDelete by remember { mutableStateOf<AlarmItem?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Request Notification permission on Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            scope.launch {
                snackbarHostState.showSnackbar("يرجى تفعيل الإشعارات لتصلك تنبيهات المنبه بدقة")
            }
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("alarm_main_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    selectedAlarmForEdit = null
                    showEditDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "إضافة منبه") },
                text = { Text("إضافة منبه", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_alarm")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top App Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "منبه بسيط",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "تنظيم نومك ويومك بسهولة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Live Digital Clock & Countdown Header
            item {
                LiveClockHeader(
                    timeString = formattedTime,
                    amPm = amPm,
                    dateString = dateFormatted,
                    nextAlarmText = nextAlarmCountdown
                )
            }

            // Quick One-Tap Naps
            item {
                QuickNapSection(
                    onNapSelected = { minutes ->
                        viewModel.addQuickNap(minutes)
                        scope.launch {
                            snackbarHostState.showSnackbar("تم تفعيل منبه قيلولة بعد $minutes دقيقة بنجاح")
                        }
                    }
                )
            }

            // Alarms List Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المنبهات المجدولة (${alarms.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Empty state if alarms list is empty
            if (alarms.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AlarmOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "لا يوجد أي منبه حالياً",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "اضغط على زر (إضافة منبه) في الأسفل لإنشاء منبهك الأول",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // Alarms List items
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        onToggle = { enabled ->
                            viewModel.toggleAlarm(alarm, enabled)
                            scope.launch {
                                val status = if (enabled) "تفعيل" else "تعطيل"
                                snackbarHostState.showSnackbar("تم $status منبه ${alarm.label}")
                            }
                        },
                        onClick = {
                            selectedAlarmForEdit = alarm
                            showEditDialog = true
                        },
                        onDelete = {
                            alarmToDelete = alarm
                        }
                    )
                }
            }
        }
    }

    // Add / Edit Alarm Dialog
    if (showEditDialog) {
        AlarmEditDialog(
            initialAlarm = selectedAlarmForEdit,
            onDismiss = {
                showEditDialog = false
                selectedAlarmForEdit = null
            },
            onSave = { savedAlarm ->
                viewModel.addOrUpdateAlarm(savedAlarm)
                showEditDialog = false
                selectedAlarmForEdit = null
                scope.launch {
                    snackbarHostState.showSnackbar("تم حفظ منبه ${savedAlarm.label} (${savedAlarm.formattedTime()} ${savedAlarm.amPmText()})")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (alarmToDelete != null) {
        AlertDialog(
            onDismissRequest = { alarmToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("حذف المنبه") },
            text = { Text("هل أنت متأكد من حذف منبه \"${alarmToDelete?.label}\" (${alarmToDelete?.formattedTime()})؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        alarmToDelete?.let { viewModel.deleteAlarm(it) }
                        alarmToDelete = null
                        scope.launch {
                            snackbarHostState.showSnackbar("تم حذف المنبه")
                        }
                    }
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { alarmToDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Alarm Ringing Fullscreen Alert
    if (ringingState.isRinging) {
        AlarmRingingOverlay(
            label = ringingState.label,
            onDismiss = {
                viewModel.dismissRingingAlarm()
                scope.launch {
                    snackbarHostState.showSnackbar("تم إيقاف المنبه")
                }
            },
            onSnooze = {
                viewModel.snoozeRingingAlarm(minutes = 5)
                scope.launch {
                    snackbarHostState.showSnackbar("تم تأجيل المنبه لمدة ٥ دقائق")
                }
            }
        )
    }
}
