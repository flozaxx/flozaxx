package com.flozaxx.applock.ui.setup

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.flozaxx.applock.R
import com.flozaxx.applock.graph
import com.flozaxx.applock.lock.PinDots
import com.flozaxx.applock.lock.PinPad
import com.flozaxx.applock.storage.PinVault
import com.flozaxx.applock.ui.components.ScreenScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class CredentialSetupMode(val asksForPin: Boolean, val issuesRecoveryCode: Boolean) {
    FIRST_TIME(asksForPin = true, issuesRecoveryCode = true),
    CHANGE_PIN(asksForPin = true, issuesRecoveryCode = false),
    NEW_RECOVERY_CODE(asksForPin = false, issuesRecoveryCode = true),

    /** After the old recovery code was used: it is spent, so a new PIN comes with a new code. */
    AFTER_RECOVERY(asksForPin = true, issuesRecoveryCode = true),
}

private const val MIN_PIN_LENGTH = 4
private const val MAX_PIN_LENGTH = 8

/** Choosing a PIN (entered twice) and, where the mode asks for it, showing a fresh recovery code. */
@Composable
fun CredentialSetup(mode: CredentialSetupMode, onFinished: () -> Unit, onBack: (() -> Unit)?) {
    val graph = LocalContext.current.graph
    val scope = rememberCoroutineScope()
    var pinChosen by remember { mutableStateOf(!mode.asksForPin) }
    var saving by remember { mutableStateOf(false) }
    // Kept in memory only: the code is shown once and must not end up in saved instance state.
    var recoveryCode by remember { mutableStateOf<String?>(null) }

    suspend fun issueRecoveryCode() {
        val code = PinVault.newRecoveryCode()
        val credential = withContext(Dispatchers.Default) { graph.vault.create(PinVault.normalizeRecoveryCode(code)) }
        graph.store.update { it.copy(recoveryCode = credential) }
        recoveryCode = code
    }

    val code = recoveryCode
    when {
        !pinChosen -> PinChoiceScreen(
            title = stringResource(if (mode == CredentialSetupMode.FIRST_TIME) R.string.pin_create_title else R.string.pin_change_title),
            saving = saving,
            onBack = onBack,
            onPinChosen = { pin ->
                saving = true
                scope.launch {
                    val credential = withContext(Dispatchers.Default) { graph.vault.create(pin) }
                    graph.store.update { it.copy(pin = credential, failedAttempts = 0, lockoutUntilMillis = 0) }
                    if (mode.issuesRecoveryCode) issueRecoveryCode()
                    pinChosen = true
                    saving = false
                    if (!mode.issuesRecoveryCode) onFinished()
                }
            },
        )

        code != null -> RecoveryCodeScreen(code, onDone = onFinished)

        else -> {
            LaunchedEffect(Unit) {
                if (mode.issuesRecoveryCode) issueRecoveryCode()
            }
            Surface(Modifier.fillMaxSize()) {
                Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
        }
    }
}

@Composable
private fun PinChoiceScreen(title: String, saving: Boolean, onBack: (() -> Unit)?, onPinChosen: (String) -> Unit) {
    // Security: plain remember on purpose, so the PIN being typed never lands in saved instance state.
    var first by remember { mutableStateOf<String?>(null) }
    var entry by remember { mutableStateOf("") }
    var mismatch by remember { mutableStateOf(false) }
    val confirming = first != null

    BackHandler(enabled = confirming) {
        first = null
        entry = ""
    }

    ScreenScaffold(title = title, onBack = onBack) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(if (confirming) R.string.pin_repeat else R.string.pin_choose),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(if (mismatch) R.string.pin_mismatch else R.string.pin_length_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = if (mismatch) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Box(Modifier.height(16.dp), contentAlignment = Alignment.Center) {
                PinDots(
                    total = entry.length,
                    filled = entry.length,
                    color = MaterialTheme.colorScheme.primary,
                    dotSize = 14.dp,
                    gap = 14.dp,
                )
            }
            Spacer(Modifier.weight(1f))
            PinPad(
                keySize = 72.dp,
                columnGap = 24.dp,
                rowGap = 12.dp,
                bottomEnd = {
                    Surface(
                        onClick = { entry = entry.dropLast(1) },
                        shape = CircleShape,
                        modifier = Modifier.size(72.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(painterResource(R.drawable.ic_backspace), stringResource(R.string.lock_delete))
                        }
                    }
                },
            ) { digit ->
                Surface(
                    onClick = {
                        if (entry.length < MAX_PIN_LENGTH) entry += digit
                        mismatch = false
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(72.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) { Text(digit.toString(), fontSize = 26.sp) }
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                enabled = entry.length >= MIN_PIN_LENGTH && !saving,
                onClick = {
                    val chosen = first
                    when {
                        chosen == null -> {
                            first = entry
                            entry = ""
                        }
                        chosen == entry -> onPinChosen(entry)
                        else -> {
                            first = null
                            entry = ""
                            mismatch = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(if (confirming) R.string.pin_save else R.string.next))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RecoveryCodeScreen(code: String, onDone: () -> Unit) {
    val context = LocalContext.current
    val graph = context.graph
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf(graph.store.settings.value.recoveryEmail) }
    var noMailApp by remember { mutableStateOf(false) }
    val subject = stringResource(R.string.recovery_mail_subject)
    val body = stringResource(R.string.recovery_mail_body, code)

    // Leaving without having stored the code would make a forgotten PIN unrecoverable.
    BackHandler {}

    ScreenScaffold(title = stringResource(R.string.recovery_title), onBack = null) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        ) {
            Text(
                stringResource(R.string.recovery_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Card(Modifier.fillMaxWidth()) {
                Text(
                    code,
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                label = { Text(stringResource(R.string.recovery_email_label)) },
                supportingText = {
                    Text(stringResource(if (noMailApp) R.string.recovery_no_mail_app else R.string.recovery_email_hint))
                },
                isError = noMailApp,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                enabled = '@' in email,
                onClick = {
                    scope.launch { graph.store.update { it.copy(recoveryEmail = email) } }
                    val mail = Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
                        .putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                        .putExtra(Intent.EXTRA_SUBJECT, subject)
                        .putExtra(Intent.EXTRA_TEXT, body)
                    try {
                        context.startActivity(mail)
                    } catch (_: ActivityNotFoundException) {
                        noMailApp = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(painterResource(R.drawable.ic_mail), null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.recovery_send_mail))
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.recovery_saved))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun ForgotPinScreen(onRecovered: () -> Unit, onBack: () -> Unit) {
    val graph = LocalContext.current.graph
    val scope = rememberCoroutineScope()
    var entry by rememberSaveable { mutableStateOf("") }
    var checking by remember { mutableStateOf(false) }
    var wrong by remember { mutableStateOf(false) }
    var accepted by rememberSaveable { mutableStateOf(false) }

    if (accepted) {
        CredentialSetup(CredentialSetupMode.AFTER_RECOVERY, onFinished = onRecovered, onBack = null)
        return
    }

    BackHandler(onBack = onBack)
    ScreenScaffold(title = stringResource(R.string.forgot_title), onBack = onBack) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 24.dp)) {
            Text(
                stringResource(R.string.forgot_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = entry,
                onValueChange = {
                    entry = it.uppercase()
                    wrong = false
                },
                label = { Text(stringResource(R.string.forgot_code_label)) },
                supportingText = { if (wrong) Text(stringResource(R.string.forgot_wrong_code)) },
                isError = wrong,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Button(
                enabled = entry.isNotBlank() && !checking,
                onClick = {
                    checking = true
                    scope.launch {
                        val stored = graph.store.settings.value.recoveryCode
                        val matches = stored != null && withContext(Dispatchers.Default) {
                            graph.vault.matches(PinVault.normalizeRecoveryCode(entry), stored)
                        }
                        checking = false
                        if (matches) accepted = true else wrong = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.forgot_check))
            }
        }
    }
}
