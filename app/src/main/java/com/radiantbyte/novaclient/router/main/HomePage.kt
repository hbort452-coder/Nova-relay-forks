package com.radiantbyte.novaclient.router.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.OpenableColumns
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import com.radiantbyte.novaclient.ui.component.NovaGlassCard
import com.radiantbyte.novaclient.ui.component.NovaButton
import com.radiantbyte.novaclient.ui.component.NovaFloatingActionButton
import com.radiantbyte.novaclient.ui.theme.NovaColors
import com.radiantbyte.novaclient.ui.component.authId
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.radiantbyte.novaclient.R
import com.radiantbyte.novaclient.service.Services
import com.radiantbyte.novaclient.util.ServerCompatUtils
import com.radiantbyte.novaclient.util.LocalSnackbarHostState
import com.radiantbyte.novaclient.util.MinecraftUtils
import com.radiantbyte.novaclient.util.SnackbarHostStateScope
import com.radiantbyte.novaclient.viewmodel.MainScreenViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import androidx.core.net.toUri

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePageContent() {
    SnackbarHostStateScope {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val snackbarHostState = LocalSnackbarHostState.current
        val mainScreenViewModel: MainScreenViewModel = viewModel()
        val onPostPermissionResult: (Boolean) -> Unit = block@{ isGranted: Boolean ->
            if (!isGranted) {
                coroutineScope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.notification_permission_denied)
                    )
                }
                return@block
            }

            if (mainScreenViewModel.selectedGame.value === null) {
                coroutineScope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.select_game_first)
                    )
                }
                return@block
            }

            Services.toggle(context, mainScreenViewModel.captureModeModel.value)

        }
        val postNotificationPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted -> onPostPermissionResult(isGranted) }
        val overlayPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (!Settings.canDrawOverlays(context)) {
                coroutineScope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.overlay_permission_denied)
                    )
                }
                return@rememberLauncherForActivityResult
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                postNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return@rememberLauncherForActivityResult
            }
            onPostPermissionResult(true)
        }
        var isActiveBefore by rememberSaveable { mutableStateOf(Services.isActive) }
        var showConnectionDialog by remember { mutableStateOf(false) }
        LaunchedEffect(Services.isActive) {
            if (Services.isActive == isActiveBefore) {
                return@LaunchedEffect
            }

            isActiveBefore = Services.isActive
            if (Services.isActive) {
                showConnectionDialog = true
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = context.getString(R.string.backend_connected),
                    actionLabel = context.getString(R.string.start_game)
                )
                val selectedGame = mainScreenViewModel.selectedGame.value
                if (result == SnackbarResult.ActionPerformed && selectedGame != null) {
                    val intent = context.packageManager.getLaunchIntentForPackage(selectedGame)
                    if (intent != null) {
                        context.startActivity(intent)
                    } else {
                        snackbarHostState.showSnackbar(
                            message = context.getString(R.string.failed_to_launch_game),
                        )
                    }
                }
                return@LaunchedEffect
            }

            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = context.getString(R.string.backend_disconnected)
            )
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.home),
                                style = MaterialTheme.typography.headlineMedium,
                                color = NovaColors.OnSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NovaColors.Background,
                        titleContentColor = NovaColors.OnSurface
                    )
                )
            },
            bottomBar = {
                SnackbarHost(
                    snackbarHostState,
                    modifier = Modifier
                        .animateContentSize()
                )
            },
            containerColor = NovaColors.Background
        ) {
            Box(
                Modifier
                    .padding(it)
                    .fillMaxSize()
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Welcome Section
                    WelcomeCard()

                    // First Row: What's this and Game selection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            IntroductionCard()
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            GameCard()
                        }
                    }

                    // Second Row: Import data packs (full width)
                    TexturePackCard()

                    HomeLinksRow()
                }
                NovaFloatingActionButton(
                    onClick = {
                        if (!Settings.canDrawOverlays(context)) {
                            Toast.makeText(
                                context,
                                R.string.request_overlay_permission,
                                Toast.LENGTH_SHORT
                            ).show()

                            overlayPermissionLauncher.launch(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    "package: ${context.packageName}".toUri()
                                )
                            )
                            return@NovaFloatingActionButton
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            postNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            return@NovaFloatingActionButton
                        }

                        onPostPermissionResult(true)
                    },
                    modifier = Modifier
                        .padding(15.dp)
                        .align(Alignment.BottomEnd),
                    containerColor = if (Services.isActive) NovaColors.Error else NovaColors.Primary,
                    contentColor = NovaColors.OnPrimary
                ) {
                    AnimatedContent(Services.isActive, label = "") { isActive ->
                        if (!isActive) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = null
                            )
                        } else {
                            Icon(
                                Icons.Rounded.Pause,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
        if (showConnectionDialog) {
            val ipAddress = remember {
                runCatching {
                    NetworkInterface.getNetworkInterfaces().asSequence()
                        .flatMap { it.inetAddresses.asSequence() }
                        .filterIsInstance<Inet4Address>()
                        .firstOrNull { !it.isLoopbackAddress }
                        ?.hostAddress
                }.getOrNull() ?: "127.0.0.1"
            }

            AlertDialog(
                onDismissRequest = { showConnectionDialog = false },
                title = {
                    Text(
                        "Nova Relay Connected",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val currentModel = mainScreenViewModel.captureModeModel.value
                        if (currentModel.isProtectedServer()) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = NovaColors.Primary.copy(alpha = 0.1f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "🎯 Aternos Server Connected",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = NovaColors.Primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Server: ${currentModel.serverHostName}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        ServerCompatUtils.getStatusMessage(currentModel.serverConfigType),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NovaColors.OnSurfaceVariant
                                    )
                                }
                            }
                        }

                        Text(
                            "To join, go to Minecraft's Friends tab and join through LAN. If LAN doesn't show up, you can add a new server in the Servers tab by entering the IP address and port provided below, then press Play.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // Connection details card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "IP Address:",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        ipAddress,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(2f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Port:",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        "19132",
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(2f)
                                    )
                                }
                            }
                        }

                        if (currentModel.isProtectedServer()) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = NovaColors.Secondary.copy(alpha = 0.1f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "💡 Aternos Server Connection Tips:",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = NovaColors.Secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    ServerCompatUtils.getTroubleshootingTips().take(3).forEach { tip ->
                                        Text(
                                            tip,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NovaColors.OnSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showConnectionDialog = false }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
private fun WelcomeCard() {
    NovaGlassCard(
        modifier = Modifier.fillMaxWidth(),
        glowColor = NovaColors.Primary,
        glowIntensity = 0.3f
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Rounded.Home,
                    contentDescription = null,
                    tint = NovaColors.Primary,
                    modifier = Modifier
                        .background(NovaColors.Primary.copy(alpha = 0.2f), CircleShape)
                        .padding(12.dp)
                        .size(32.dp)
                )
                Column {
                    Text(
                        "Welcome to Nova Client",
                        style = MaterialTheme.typography.headlineMedium,
                        color = NovaColors.OnSurface
                    )
                    Text(
                        "Enhanced Minecraft Bedrock Experience",
                        style = MaterialTheme.typography.bodyLarge,
                        color = NovaColors.Primary
                    )
                }
            }
        }
    }
}

@Composable
private fun IntroductionCard() {
    NovaGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        glowColor = NovaColors.Secondary,
        glowIntensity = 0.3f
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.what_is_this),
                style = MaterialTheme.typography.titleMedium,
                color = NovaColors.Secondary
            )
            Text(
                stringResource(R.string.introduction),
                style = MaterialTheme.typography.bodyMedium,
                color = NovaColors.OnSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun GameCard() {
    val context = LocalContext.current
    val mainScreenViewModel: MainScreenViewModel = viewModel()
    val captureModeModel by mainScreenViewModel.captureModeModel.collectAsStateWithLifecycle()
    var showGameSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var serverHostName by rememberSaveable(showGameSettingsDialog) { mutableStateOf(captureModeModel.serverHostName) }
    var serverPort by rememberSaveable(showGameSettingsDialog) { mutableStateOf(captureModeModel.serverPort.toString()) }
    var showGameSelectorDialog by remember { mutableStateOf(false) }
    val packageInfos by mainScreenViewModel.packageInfos.collectAsStateWithLifecycle()
    val packageInfoState by mainScreenViewModel.packageInfoState.collectAsStateWithLifecycle()
    val selectedGame by mainScreenViewModel.selectedGame.collectAsStateWithLifecycle()

    NovaGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        glowColor = NovaColors.Accent,
        glowIntensity = 0.25f,
        onClick = {
            showGameSettingsDialog = true
        }
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.mipmap.minecraft_icon),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.minecraft),
                        style = MaterialTheme.typography.titleMedium,
                        color = NovaColors.OnSurface
                    )
                    Text(
                        stringResource(
                            R.string.recommended_version,
                            MinecraftUtils.RECOMMENDED_VERSION
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaColors.OnSurfaceVariant
                    )
                }
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = NovaColors.Accent,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Game Features Section
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Game Features",
                    style = MaterialTheme.typography.labelMedium,
                    color = NovaColors.Accent
                )

                GameFeatureItem(
                    icon = Icons.Rounded.Security,
                    title = "Enhanced Security",
                    description = "MITM-based packet interception"
                )

                GameFeatureItem(
                    icon = Icons.Rounded.Speed,
                    title = "Performance Boost",
                    description = "Optimized network handling"
                )

                GameFeatureItem(
                    icon = Icons.Rounded.Extension,
                    title = "Module Support",
                    description = "Custom gameplay modifications"
                )
            }
        }
    }

    if (showGameSelectorDialog) {
        LifecycleEventEffect(Lifecycle.Event.ON_START) {
            mainScreenViewModel.fetchPackageInfos()
        }

        BasicAlertDialog(
            onDismissRequest = {
                showGameSelectorDialog = false
            },
            modifier = Modifier
                .padding(vertical = 24.dp),
            content = {
                Surface(
                    shape = AlertDialogDefaults.shape,
                    tonalElevation = AlertDialogDefaults.TonalElevation
                ) {
                    Column(
                        Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            stringResource(R.string.game_selector),
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        LazyColumn(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            item {
                                if (packageInfoState === MainScreenViewModel.PackageInfoState.Loading) {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .padding(vertical = 16.dp)
                                            .fillMaxWidth()
                                    )
                                }
                            }
                            items(packageInfos.size) {
                                val packageInfo = packageInfos[it]
                                val applicationInfo = packageInfo.applicationInfo!!
                                val packageManager = context.packageManager
                                val icon = remember {
                                    applicationInfo.loadIcon(packageManager).toBitmap()
                                        .asImageBitmap()
                                }
                                val name = remember {
                                    applicationInfo.loadLabel(packageManager).toString()
                                }
                                val packageName = packageInfo.packageName
                                val versionName = packageInfo.versionName ?: "0.0.0"
                                Card(
                                    onClick = {
                                        mainScreenViewModel.selectGame(packageName)
                                        showGameSelectorDialog = false
                                    },
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Icon(
                                            bitmap = icon,
                                            contentDescription = null,
                                            tint = Color.Unspecified,
                                            modifier = Modifier
                                                .size(24.dp)
                                        )
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                packageName,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                versionName,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    if (showGameSettingsDialog) {
        BasicAlertDialog(
            onDismissRequest = {
                showGameSettingsDialog = false
            },
            modifier = Modifier
                .padding(vertical = 24.dp),
            content = {
                Surface(
                    shape = AlertDialogDefaults.shape,
                    tonalElevation = AlertDialogDefaults.TonalElevation
                ) {
                    Column(
                        Modifier
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            stringResource(R.string.game_settings),
                            modifier = Modifier
                                .align(Alignment.Start),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                        ) {
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()
                            if (isPressed) {
                                SideEffect {
                                    showGameSelectorDialog = true
                                }
                            }
                            TextField(
                                value = selectedGame ?: "",
                                onValueChange = {},
                                readOnly = true,
                                maxLines = 1,
                                label = {
                                    Text(stringResource(R.string.select_game))
                                },
                                placeholder = {
                                    Text(stringResource(R.string.no_game_selected))
                                },
                                interactionSource = interactionSource,
                                enabled = !Services.isActive
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                TextField(
                                    value = serverHostName,
                                    label = {
                                        Text(stringResource(R.string.server_host_name))
                                    },
                                    onValueChange = {
                                        serverHostName = it

                                        if (it.isEmpty()) return@TextField
                                        val updatedModel = captureModeModel.copy(serverHostName = it)
                                            .withAutoDetectedServerConfig()
                                        mainScreenViewModel.selectCaptureModeModel(updatedModel)
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Next
                                    ),
                                    singleLine = true,
                                    enabled = !Services.isActive
                                )
                                TextField(
                                    value = serverPort,
                                    label = {
                                        Text(stringResource(R.string.server_port))
                                    },
                                    onValueChange = {
                                        serverPort = it

                                        if (it.isEmpty()) return@TextField
                                        val port = it.toIntOrNull() ?: return@TextField
                                        if (port < 0 || port > 65535) return@TextField

                                        mainScreenViewModel.selectCaptureModeModel(
                                            captureModeModel.copy(serverPort = port)
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Done
                                    ),
                                    singleLine = true,
                                    enabled = !Services.isActive
                                )
                            }

                            if (ServerCompatUtils.isProtectedServer(serverHostName) && !Services.isActive) {
                                Card(
                                    modifier = Modifier
                                        .width(TextFieldDefaults.MinWidth),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(
                                        containerColor = NovaColors.Primary.copy(alpha = 0.1f),
                                        contentColor = NovaColors.Primary
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.Info,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = NovaColors.Primary
                                            )
                                            Text(
                                                "Aternos Server Detected",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        val serverInfo = ServerCompatUtils.extractServerInfo(serverHostName)
                                        if (serverInfo != null) {
                                            Text(
                                                "Server: ${serverInfo.serverId}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }

                                        Text(
                                            ServerCompatUtils.getStatusMessage(captureModeModel.serverConfigType),
                                            style = MaterialTheme.typography.bodySmall
                                        )

                                        Text(
                                            ServerCompatUtils.getConfigDescription(captureModeModel.serverConfigType),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NovaColors.OnSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (Services.isActive) {
                                Card(
                                    modifier = Modifier
                                        .width(TextFieldDefaults.MinWidth),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Info,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                stringResource(R.string.tips),
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                stringResource(R.string.change_game_settings_tip),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun TexturePackCard() {
    val context = LocalContext.current
    rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            try {
                val fileName = context.contentResolver.query(selectedUri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    cursor.getString(nameIndex)
                } ?: return@let

                if (!fileName.endsWith(".mcpack") &&
                    !fileName.endsWith(".mcaddon") &&
                    !fileName.endsWith(".mcworld")) {
                    Toast.makeText(context, "Invalid file type. Only .mcpack, .mcaddon and .mcworld files are supported", Toast.LENGTH_LONG).show()
                    return@let
                }

                val tempFile = File(context.cacheDir, fileName)
                context.contentResolver.openInputStream(selectedUri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val fileUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    tempFile
                )

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(fileUri, when {
                        fileName.endsWith(".mcworld") -> "application/x-world"
                        fileName.endsWith(".mcpack") -> "application/x-minecraft-resourcepack"
                        fileName.endsWith(".mcaddon") -> "application/x-minecraft-addon"
                        else -> "application/octet-stream"
                    })
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    `package` = "com.mojang.minecraftpe"
                }

                try {
                    context.startActivity(intent)
                    Toast.makeText(context, "Data pack sent to Minecraft!", Toast.LENGTH_SHORT).show()
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context,
                        "Failed to open Minecraft. Error: ${e.message}",
                        Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Toast.makeText(context, "Failed to import data pack: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    NovaGlassCard(
        modifier = Modifier.fillMaxWidth(),
        glowColor = NovaColors.Secondary,
        glowIntensity = 0.2f
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.AddPhotoAlternate,
                    contentDescription = null,
                    tint = NovaColors.Secondary,
                    modifier = Modifier.size(28.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        "Minecraft Data Packs",
                        style = MaterialTheme.typography.titleMedium,
                        color = NovaColors.OnSurface
                    )
                    Text(
                        "Import texture packs, skins, add-ons and worlds",
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaColors.OnSurfaceVariant
                    )
                }
            }

            NovaButton(
                onClick = {
                    filePickerLauncher.launch("*/*")
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NovaColors.Secondary,
                    contentColor = NovaColors.OnSecondary
                )
            ) {
                Icon(
                    Icons.Rounded.Upload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Import Data Packs")
            }
        }
    }
}

@Composable
private fun GameFeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NovaColors.Secondary,
            modifier = Modifier.size(16.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = NovaColors.OnSurface,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = NovaColors.OnSurfaceVariant
            )
        }
    }
}

@Composable
private fun HomeLinksRow() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        NovaGlassCard(
            onClick = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    "https://www.youtube.com/channel/$authId".toUri()
                )
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            glowColor = NovaColors.Primary,
            glowIntensity = 0.18f
        ) {
            Row(
                Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = NovaColors.Primary,
                    modifier = Modifier.size(28.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        "Updates & News",
                        style = MaterialTheme.typography.titleMedium,
                        color = NovaColors.OnSurface
                    )
                    Text(
                        "Follow for latest updates and release notes",
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaColors.OnSurfaceVariant
                    )
                }
            }
        }

        NovaGlassCard(
            onClick = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    "https://discord.gg/WAe8wT7EkG".toUri()
                )
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            glowColor = NovaColors.Accent,
            glowIntensity = 0.18f
        ) {
            Row(
                Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    tint = NovaColors.Accent,
                    modifier = Modifier.size(28.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        "Join Discord",
                        style = MaterialTheme.typography.titleMedium,
                        color = NovaColors.OnSurface
                    )
                    Text(
                        "Community support & discussions",
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaColors.OnSurfaceVariant
                    )
                }
            }
        }
    }
}
