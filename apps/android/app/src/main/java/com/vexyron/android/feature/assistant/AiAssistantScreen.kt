package com.vexyron.android.feature.assistant

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.vexyron.android.core.components.VexyronStatusBadge
import com.vexyron.android.core.designsystem.VexyronBg
import com.vexyron.android.core.designsystem.VexyronBorder
import com.vexyron.android.core.designsystem.VexyronCardBg
import com.vexyron.android.core.designsystem.VexyronCyan
import com.vexyron.android.core.designsystem.VexyronGradientPrimary
import com.vexyron.android.core.designsystem.VexyronGreen
import com.vexyron.android.core.designsystem.VexyronPurple
import com.vexyron.android.core.designsystem.VexyronSurfaceElevated
import com.vexyron.android.core.designsystem.VexyronTextMuted
import com.vexyron.android.core.designsystem.VexyronTextPrimary
import com.vexyron.android.core.designsystem.VexyronTextSecondary
import com.vexyron.android.core.voice.VoiceManager
import com.vexyron.android.domain.model.DeviceState
import com.vexyron.android.service.network.ConnectionManager
import com.vexyron.android.service.terminal.TerminalManager
import com.vexyron.android.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val time: String = "Just now"
)

sealed class ActionResolution {
    data class ImmediateReply(val message: String) : ActionResolution()
    data class LaunchApp(val command: String, val appName: String) : ActionResolution()
    data class SystemControl(val command: String, val message: String) : ActionResolution()
    data class ExecuteCommand(val command: String) : ActionResolution()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiAssistantScreen(
    navController: NavController,
    vm: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val hostOs by ConnectionManager.hostOs.collectAsState()
    val hostName by ConnectionManager.hostName.collectAsState()
    val isConnected = ConnectionManager.isConnected && vm.state == DeviceState.CONNECTED

    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val isKeyboardOpen = WindowInsets.isImeVisible || imeBottom > 0

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                "1",
                "Hey! I am Vex. I can control your laptop, open applications like WhatsApp and Brave, run commands, and check system status. How can I help you?",
                isUser = false
            )
        )
    }

    fun resolveAction(rawText: String, isWindows: Boolean): ActionResolution {
        val trimmed = rawText.trim()
        val lower = trimmed.lowercase()

        // 1. GREETINGS & INTRO
        if (lower in listOf("hi", "hello", "hey", "hey vex", "hello vex", "hi vex", "yo", "sup") ||
            lower.startsWith("who are you") || lower.startsWith("what are you")
        ) {
            return ActionResolution.ImmediateReply(
                "Hey! I am Vex. I can control your laptop, open apps and shortcuts (like WhatsApp in Brave), execute shell commands, adjust volume, and check device status. What would you like me to do?"
            )
        }

        if (lower == "help" || lower == "what can you do" || lower == "commands") {
            return ActionResolution.ImmediateReply(
                "🤖 Hey! I am Vex! Here is what I can do:\n" +
                "• Open apps & shortcuts: 'open whatsapp in brave from brave shortcuts', 'open brave', 'open chrome', 'open vscode', 'open spotify'\n" +
                "• Open websites: 'open youtube in brave', 'open chatgpt', 'open github'\n" +
                "• System controls: 'lock laptop', 'sleep', 'mute', 'volume up', 'take screenshot', 'close brave'\n" +
                "• Diagnostics: 'whoami', 'my ip', 'disk space', 'ram usage', 'uptime'\n" +
                "• Run any command: 'ls', 'dir', 'ifconfig', 'git status', 'ping google.com'"
            )
        }

        // 2. SMARTPHONE BATTERY
        if (lower in listOf("battery", "phone battery", "check battery", "check my phone battery", "my battery")) {
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                context.registerReceiver(null, filter)
            }
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 85
            return ActionResolution.ImmediateReply("🔋 Your smartphone battery is at $level%.")
        }

        // 3. CONNECTION & SPEED
        if (lower in listOf("status", "is laptop connected", "is laptop connected?", "connection", "connection status", "check status")) {
            val osName = if (hostOs != "Unknown") hostOs else "Laptop"
            val targetName = if (hostName.isNotBlank() && hostName != "Laptop") hostName else osName
            return if (isConnected) {
                ActionResolution.ImmediateReply("🟢 Connected to $osName laptop ($targetName) on port 45876. Ready to execute commands and open applications.")
            } else {
                ActionResolution.ImmediateReply("🔴 Laptop is currently disconnected. Go to the Home screen and tap 'Connect' to pair via Wi-Fi.")
            }
        }

        if (lower in listOf("speed", "wifi speed", "network speed", "latency")) {
            return ActionResolution.ImmediateReply("⚡ Local Wi-Fi channel operating at ultra-low latency with transfer throughput up to ~25 MB/s.")
        }

        // 4. CLOSING / KILLING APPS
        val isCloseIntent = lower.startsWith("close ") || lower.startsWith("kill ") || lower.startsWith("quit ") || lower.startsWith("exit ") || lower.startsWith("stop ")
        if (isCloseIntent) {
            val appToClose = lower
                .removePrefix("close ")
                .removePrefix("kill ")
                .removePrefix("quit ")
                .removePrefix("exit ")
                .removePrefix("stop ")
                .replace(" on laptop", "")
                .replace(" in laptop", "")
                .trim()
            val targetProc = when (appToClose) {
                "brave", "brave browser" -> "brave"
                "chrome", "google chrome" -> "chrome"
                "firefox" -> "firefox"
                "code", "vscode", "vs code" -> "code"
                "spotify" -> "spotify"
                "discord" -> "discord"
                "vlc" -> "vlc"
                "steam" -> "steam"
                else -> appToClose
            }
            val cmd = if (isWindows) {
                "Stop-Process -Name \"$targetProc\" -Force -ErrorAction SilentlyContinue; taskkill /IM \"$targetProc.exe\" /F 2>nul"
            } else {
                "pkill -f \"$targetProc\""
            }
            return ActionResolution.SystemControl(cmd, "🛑 Closed $appToClose on your laptop.")
        }

        // 5. SYSTEM CONTROLS (VOLUME, POWER, SCREEN, PLAYBACK)
        if (lower in listOf("volume up", "increase volume", "louder", "sound up")) {
            val cmd = if (isWindows) {
                "(New-Object -ComObject WScript.Shell).SendKeys([char]175)"
            } else {
                "pactl set-sink-volume @DEFAULT_SINK@ +10% 2>/dev/null || amixer -q sset Master 10%+ 2>/dev/null"
            }
            return ActionResolution.SystemControl(cmd, "🔊 Increased volume on your laptop.")
        }

        if (lower in listOf("volume down", "decrease volume", "quieter", "sound down", "lower volume")) {
            val cmd = if (isWindows) {
                "(New-Object -ComObject WScript.Shell).SendKeys([char]174)"
            } else {
                "pactl set-sink-volume @DEFAULT_SINK@ -10% 2>/dev/null || amixer -q sset Master 10%- 2>/dev/null"
            }
            return ActionResolution.SystemControl(cmd, "🔉 Decreased volume on your laptop.")
        }

        if (lower in listOf("mute", "unmute", "toggle mute", "volume mute", "sound mute")) {
            val cmd = if (isWindows) {
                "(New-Object -ComObject WScript.Shell).SendKeys([char]173)"
            } else {
                "pactl set-sink-mute @DEFAULT_SINK@ toggle 2>/dev/null || amixer -q sset Master toggle 2>/dev/null"
            }
            return ActionResolution.SystemControl(cmd, "🔇 Toggled audio mute on your laptop.")
        }

        if (lower in listOf("play", "pause", "play pause", "play/pause", "resume")) {
            val cmd = if (isWindows) {
                "(New-Object -ComObject WScript.Shell).SendKeys([char]179)"
            } else {
                "playerctl play-pause 2>/dev/null"
            }
            return ActionResolution.SystemControl(cmd, "⏯️ Toggled media playback on your laptop.")
        }

        if (lower in listOf("next track", "next song", "next")) {
            val cmd = if (isWindows) {
                "(New-Object -ComObject WScript.Shell).SendKeys([char]176)"
            } else {
                "playerctl next 2>/dev/null"
            }
            return ActionResolution.SystemControl(cmd, "⏭️ Skipped to next track.")
        }

        if (lower in listOf("previous track", "previous song", "prev track", "previous")) {
            val cmd = if (isWindows) {
                "(New-Object -ComObject WScript.Shell).SendKeys([char]177)"
            } else {
                "playerctl previous 2>/dev/null"
            }
            return ActionResolution.SystemControl(cmd, "⏮️ Skipped to previous track.")
        }

        if (lower in listOf("lock", "lock laptop", "lock pc", "lock screen")) {
            val cmd = if (isWindows) {
                "rundll32.exe user32.dll,LockWorkStation"
            } else {
                "(loginctl lock-session 2>/dev/null) || (xdg-screensaver lock 2>/dev/null) || (gnome-screensaver-command -l 2>/dev/null) || (xflock4 2>/dev/null)"
            }
            return ActionResolution.SystemControl(cmd, "🔒 Locked your laptop screen.")
        }

        if (lower in listOf("sleep", "suspend", "sleep laptop", "put laptop to sleep")) {
            val cmd = if (isWindows) {
                "rundll32.exe powrprof.dll,SetSuspendState 0,1,0"
            } else {
                "systemctl suspend"
            }
            return ActionResolution.SystemControl(cmd, "💤 Putting your laptop to sleep...")
        }

        if (lower in listOf("restart", "reboot", "restart laptop", "reboot laptop")) {
            val cmd = if (isWindows) {
                "shutdown /r /t 10"
            } else {
                "reboot || shutdown -r now"
            }
            return ActionResolution.SystemControl(cmd, "🔄 Restarting your laptop...")
        }

        if (lower in listOf("shutdown", "power off", "turn off laptop", "turn off pc")) {
            val cmd = if (isWindows) {
                "shutdown /s /t 10"
            } else {
                "shutdown -h now || poweroff"
            }
            return ActionResolution.SystemControl(cmd, "🔌 Shutting down your laptop...")
        }

        if (lower in listOf("screenshot", "take screenshot", "capture screen", "take a screenshot")) {
            val cmd = if (isWindows) {
                "Start-Process SnippingTool.exe"
            } else {
                "(gnome-screenshot 2>/dev/null) || (spectacle 2>/dev/null) || (scrot ~/Pictures/screenshot_%Y%m%d_%H%M%S.png 2>/dev/null) || (import -window root ~/Pictures/screenshot.png 2>/dev/null)"
            }
            return ActionResolution.SystemControl(cmd, "📸 Captured screenshot on your laptop.")
        }

        // 6. WHATSAPP & BRAVE SHORTCUTS (SPECIALIZED RESOLVER)
        val hasWhatsApp = lower.contains("whatsapp")
        val hasBrave = lower.contains("brave")
        val hasShortcut = lower.contains("shotcut") || lower.contains("shortcut") || lower.contains("short cut") || lower.contains("pwa") || lower.contains("web app")

        if (hasWhatsApp) {
            val waUrl = "https://web.whatsapp.com"
            val cmd = if (isWindows) {
                if (hasBrave || hasShortcut) {
                    "try { Start-Process brave -ArgumentList '--app=$waUrl' } catch { try { Start-Process brave -ArgumentList '$waUrl' } catch { Start-Process '$waUrl' } }"
                } else {
                    "try { Start-Process whatsapp: } catch { try { Start-Process brave -ArgumentList '--app=$waUrl' } catch { try { Start-Process chrome -ArgumentList '$waUrl' } catch { Start-Process '$waUrl' } } }"
                }
            } else {
                if (hasBrave || hasShortcut) {
                    "(nohup brave-browser --app=\"$waUrl\" >/dev/null 2>&1 &) || (nohup brave --app=\"$waUrl\" >/dev/null 2>&1 &) || (nohup /snap/bin/brave --app=\"$waUrl\" >/dev/null 2>&1 &) || (nohup flatpak run com.brave.Browser --app=\"$waUrl\" >/dev/null 2>&1 &) || (nohup brave-browser \"$waUrl\" >/dev/null 2>&1 &) || (nohup brave \"$waUrl\" >/dev/null 2>&1 &) || (xdg-open \"$waUrl\" >/dev/null 2>&1 &)"
                } else {
                    "(gtk-launch whatsapp >/dev/null 2>&1 &) || (gtk-launch whatsapp-for-linux >/dev/null 2>&1 &) || (nohup brave-browser --app=\"$waUrl\" >/dev/null 2>&1 &) || (nohup google-chrome --app=\"$waUrl\" >/dev/null 2>&1 &) || (nohup brave \"$waUrl\" >/dev/null 2>&1 &) || (xdg-open \"$waUrl\" >/dev/null 2>&1 &)"
                }
            }
            val label = if (hasBrave && hasShortcut) "WhatsApp in Brave (from shortcuts)" else if (hasBrave) "WhatsApp in Brave" else "WhatsApp"
            return ActionResolution.LaunchApp(cmd, label)
        }

        if (hasShortcut && (hasBrave || lower == "brave shortcuts" || lower == "open brave shortcuts" || lower == "open brave shotcuts")) {
            val cmd = if (isWindows) {
                "try { Start-Process brave -ArgumentList 'brave://apps' } catch { Start-Process brave }"
            } else {
                "(nohup brave-browser \"brave://apps\" >/dev/null 2>&1 &) || (nohup brave \"brave://apps\" >/dev/null 2>&1 &) || (nohup /snap/bin/brave \"brave://apps\" >/dev/null 2>&1 &)"
            }
            return ActionResolution.LaunchApp(cmd, "Brave Shortcuts")
        }

        // 7. WEBSITES & SERVICES IN SPECIFIC BROWSERS
        val webSites = listOf(
            "youtube" to ("https://www.youtube.com" to "YouTube"),
            "yt" to ("https://www.youtube.com" to "YouTube"),
            "chatgpt" to ("https://chatgpt.com" to "ChatGPT"),
            "gpt" to ("https://chatgpt.com" to "ChatGPT"),
            "openai" to ("https://chatgpt.com" to "ChatGPT"),
            "gmail" to ("https://mail.google.com" to "Gmail"),
            "github" to ("https://github.com" to "GitHub"),
            "google" to ("https://www.google.com" to "Google"),
            "twitter" to ("https://x.com" to "Twitter / X"),
            " x " to ("https://x.com" to "Twitter / X"),
            "reddit" to ("https://reddit.com" to "Reddit"),
            "instagram" to ("https://instagram.com" to "Instagram"),
            "netflix" to ("https://netflix.com" to "Netflix"),
            "discord" to ("https://discord.com/app" to "Discord Web"),
            "telegram" to ("https://web.telegram.org" to "Telegram Web"),
            "spotify" to ("https://open.spotify.com" to "Spotify Web"),
            "linkedin" to ("https://linkedin.com" to "LinkedIn"),
            "facebook" to ("https://facebook.com" to "Facebook"),
            "amazon" to ("https://amazon.com" to "Amazon"),
            "notion" to ("https://notion.so" to "Notion")
        )

        for ((siteKey, siteData) in webSites) {
            val (url, siteName) = siteData
            if (lower.contains(siteKey)) {
                if (hasBrave) {
                    val cmd = if (isWindows) {
                        if (hasShortcut) "try { Start-Process brave -ArgumentList '--app=$url' } catch { Start-Process brave -ArgumentList '$url' }"
                        else "try { Start-Process brave -ArgumentList '$url' } catch { Start-Process '$url' }"
                    } else {
                        if (hasShortcut) "(nohup brave-browser --app=\"$url\" >/dev/null 2>&1 &) || (nohup brave --app=\"$url\" >/dev/null 2>&1 &) || (xdg-open \"$url\" >/dev/null 2>&1 &)"
                        else "(nohup brave-browser \"$url\" >/dev/null 2>&1 &) || (nohup brave \"$url\" >/dev/null 2>&1 &) || (xdg-open \"$url\" >/dev/null 2>&1 &)"
                    }
                    return ActionResolution.LaunchApp(cmd, "$siteName in Brave")
                } else if (lower.contains("chrome")) {
                    val cmd = if (isWindows) {
                        "try { Start-Process chrome -ArgumentList '$url' } catch { Start-Process '$url' }"
                    } else {
                        "(nohup google-chrome \"$url\" >/dev/null 2>&1 &) || (nohup chromium \"$url\" >/dev/null 2>&1 &) || (xdg-open \"$url\" >/dev/null 2>&1 &)"
                    }
                    return ActionResolution.LaunchApp(cmd, "$siteName in Chrome")
                } else if (lower.contains("firefox")) {
                    val cmd = if (isWindows) {
                        "try { Start-Process firefox -ArgumentList '$url' } catch { Start-Process '$url' }"
                    } else {
                        "(nohup firefox \"$url\" >/dev/null 2>&1 &) || (xdg-open \"$url\" >/dev/null 2>&1 &)"
                    }
                    return ActionResolution.LaunchApp(cmd, "$siteName in Firefox")
                } else if (lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("start ")) {
                    val cmd = if (isWindows) {
                        "Start-Process '$url'"
                    } else {
                        "xdg-open \"$url\" >/dev/null 2>&1 &"
                    }
                    return ActionResolution.LaunchApp(cmd, siteName)
                }
            }
        }

        // 8. DESKTOP APPLICATIONS LAUNCH
        val cleanedApp = lower
            .removePrefix("open ")
            .removePrefix("launch ")
            .removePrefix("start ")
            .removePrefix("run ")
            .replace(" in laptop", "")
            .replace(" on laptop", "")
            .replace(" in desktop", "")
            .replace(" on desktop", "")
            .trim()

        val isLaunchPrefix = lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("start ")

        if (cleanedApp in listOf("brave", "brave browser")) {
            val cmd = if (isWindows) {
                "try { Start-Process brave } catch { start brave }"
            } else {
                "(nohup brave-browser >/dev/null 2>&1 &) || (nohup brave >/dev/null 2>&1 &) || (nohup /snap/bin/brave >/dev/null 2>&1 &) || (gtk-launch brave-browser >/dev/null 2>&1 &)"
            }
            return ActionResolution.LaunchApp(cmd, "Brave Browser")
        }

        if (cleanedApp in listOf("chrome", "google chrome")) {
            val cmd = if (isWindows) {
                "try { Start-Process chrome } catch { start chrome }"
            } else {
                "(nohup google-chrome-stable >/dev/null 2>&1 &) || (nohup google-chrome >/dev/null 2>&1 &) || (nohup chromium >/dev/null 2>&1 &) || (gtk-launch google-chrome >/dev/null 2>&1 &)"
            }
            return ActionResolution.LaunchApp(cmd, "Google Chrome")
        }

        if (cleanedApp in listOf("firefox", "mozilla firefox")) {
            val cmd = if (isWindows) {
                "try { Start-Process firefox } catch { start firefox }"
            } else {
                "(nohup firefox >/dev/null 2>&1 &) || (gtk-launch firefox >/dev/null 2>&1 &)"
            }
            return ActionResolution.LaunchApp(cmd, "Firefox")
        }

        if (cleanedApp in listOf("code", "vscode", "vs code", "visual studio code")) {
            val cmd = if (isWindows) {
                "try { Start-Process code } catch { start code }"
            } else {
                "(nohup code >/dev/null 2>&1 &) || (gtk-launch code >/dev/null 2>&1 &)"
            }
            return ActionResolution.LaunchApp(cmd, "VS Code")
        }

        if (cleanedApp in listOf("spotify")) {
            val cmd = if (isWindows) {
                "try { Start-Process spotify } catch { start spotify }"
            } else {
                "(nohup spotify >/dev/null 2>&1 &) || (nohup /snap/bin/spotify >/dev/null 2>&1 &) || (gtk-launch spotify >/dev/null 2>&1 &)"
            }
            return ActionResolution.LaunchApp(cmd, "Spotify")
        }

        if (cleanedApp in listOf("calc", "calculator")) {
            val cmd = if (isWindows) {
                "start calc"
            } else {
                "(nohup gnome-calculator >/dev/null 2>&1 &) || (nohup kcalc >/dev/null 2>&1 &) || (nohup galculator >/dev/null 2>&1 &)"
            }
            return ActionResolution.LaunchApp(cmd, "Calculator")
        }

        if (cleanedApp in listOf("notepad", "text editor", "editor")) {
            val cmd = if (isWindows) {
                "start notepad"
            } else {
                "(nohup gedit >/dev/null 2>&1 &) || (nohup kate >/dev/null 2>&1 &) || (nohup mousepad >/dev/null 2>&1 &)"
            }
            return ActionResolution.LaunchApp(cmd, "Text Editor")
        }

        if (cleanedApp in listOf("files", "file explorer", "explorer", "file manager")) {
            val cmd = if (isWindows) {
                "start explorer"
            } else {
                "(xdg-open ~ >/dev/null 2>&1 &) || (nohup nautilus >/dev/null 2>&1 &) || (nohup thunar >/dev/null 2>&1 &)"
            }
            return ActionResolution.LaunchApp(cmd, "File Explorer")
        }

        if (cleanedApp in listOf("downloads", "downloads folder")) {
            val cmd = if (isWindows) {
                "Start-Process explorer \"\$env:USERPROFILE\\Downloads\""
            } else {
                "xdg-open ~/Downloads >/dev/null 2>&1 &"
            }
            return ActionResolution.LaunchApp(cmd, "Downloads")
        }

        if (cleanedApp in listOf("settings", "system settings")) {
            val cmd = if (isWindows) {
                "start ms-settings:"
            } else {
                "(nohup gnome-control-center >/dev/null 2>&1 &) || (nohup systemsettings >/dev/null 2>&1 &)"
            }
            return ActionResolution.LaunchApp(cmd, "Settings")
        }

        if (cleanedApp in listOf("terminal", "powershell", "cmd", "bash", "shell")) {
            val cmd = if (isWindows) {
                "start powershell"
            } else {
                "(nohup gnome-terminal >/dev/null 2>&1 &) || (nohup xterm >/dev/null 2>&1 &) || (nohup konsole >/dev/null 2>&1 &)"
            }
            return ActionResolution.LaunchApp(cmd, "Terminal")
        }

        if (cleanedApp in listOf("task manager", "taskmgr", "system monitor")) {
            val cmd = if (isWindows) {
                "start taskmgr"
            } else {
                "(nohup gnome-system-monitor >/dev/null 2>&1 &) || (nohup ksysguard >/dev/null 2>&1 &)"
            }
            return ActionResolution.LaunchApp(cmd, "Task Manager")
        }

        if (cleanedApp.startsWith("http://") || cleanedApp.startsWith("https://")) {
            val cmd = if (isWindows) "Start-Process '$cleanedApp'" else "xdg-open \"$cleanedApp\" >/dev/null 2>&1 &"
            return ActionResolution.LaunchApp(cmd, cleanedApp)
        }

        // 9. SYSTEM DIAGNOSTIC SHORTCUT COMMANDS
        if (lower in listOf("whoami", "user", "username")) {
            return ActionResolution.ExecuteCommand("whoami")
        }
        if (lower in listOf("ip", "my ip", "ip address", "ipconfig", "ifconfig", "ip a")) {
            val cmd = if (isWindows) "ipconfig" else "ip -brief address show 2>/dev/null || ifconfig 2>/dev/null || hostname -I"
            return ActionResolution.ExecuteCommand(cmd)
        }
        if (lower in listOf("uptime", "system uptime")) {
            val cmd = if (isWindows) "(Get-Date) - (gcim Win32_OperatingSystem).LastBootUpTime" else "uptime -p 2>/dev/null || uptime"
            return ActionResolution.ExecuteCommand(cmd)
        }
        if (lower in listOf("ram", "memory", "ram usage", "free ram", "free memory")) {
            val cmd = if (isWindows) "Get-CimInstance Win32_OperatingSystem | Select-Object @{N=\"FreeRAM_GB\";E={[math]::Round(\$_.FreePhysicalMemory/1MB,2)}}, @{N=\"TotalRAM_GB\";E={[math]::Round(\$_.TotalVisibleMemorySize/1MB,2)}}" else "free -h"
            return ActionResolution.ExecuteCommand(cmd)
        }
        if (lower in listOf("disk", "disk space", "storage", "free disk", "df", "df -h")) {
            val cmd = if (isWindows) "Get-PSDrive -PSProvider FileSystem | Select-Object Name, @{N=\"Free_GB\";E={[math]::Round(\$_.Free/1GB,2)}}" else "df -h /"
            return ActionResolution.ExecuteCommand(cmd)
        }

        // 10. GENERAL / ARBITRARY COMMANDS OR PREFIXED COMMANDS
        val strippedCmd = trimmed
            .removePrefix("run ")
            .removePrefix("exec ")
            .removePrefix("execute ")
            .removePrefix("terminal ")
            .removePrefix("cmd ")
            .removePrefix("bash ")
            .removePrefix("powershell ")
            .trim()

        if (isLaunchPrefix) {
            val cmd = if (isWindows) {
                "start $cleanedApp"
            } else {
                "(gtk-launch $cleanedApp >/dev/null 2>&1 &) || (nohup $cleanedApp >/dev/null 2>&1 &) || (xdg-open $cleanedApp >/dev/null 2>&1 &)"
            }
            return ActionResolution.LaunchApp(cmd, cleanedApp.replaceFirstChar { it.uppercase() })
        }

        // Direct shell/terminal command
        return ActionResolution.ExecuteCommand(strippedCmd)
    }

    fun handleSend(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        messages.add(ChatMessage(System.currentTimeMillis().toString(), trimmed, isUser = true))
        inputText = ""

        scope.launch {
            delay(100)
            listState.animateScrollToItem(messages.size - 1)

            val isWindows = hostOs.equals("Windows", ignoreCase = true)
            val osName = if (hostOs != "Unknown") hostOs else "Laptop"
            val targetName = if (hostName.isNotBlank() && hostName != "Laptop") hostName else osName

            val action = resolveAction(trimmed, isWindows)

            val reply = when (action) {
                is ActionResolution.ImmediateReply -> {
                    action.message
                }
                is ActionResolution.LaunchApp -> {
                    if (!isConnected) {
                        "⚠️ Your laptop is currently disconnected. Tap 'Connect' on the Home screen to link via Wi-Fi, then I will open applications for you."
                    } else {
                        ConnectionManager.sendTerminalCommand(action.command, "~")
                        "🚀 Opening ${action.appName} on your $osName laptop ($targetName)..."
                    }
                }
                is ActionResolution.SystemControl -> {
                    if (!isConnected) {
                        "⚠️ Your laptop is currently disconnected. Tap 'Connect' on the Home screen to link via Wi-Fi."
                    } else {
                        ConnectionManager.sendTerminalCommand(action.command, "~")
                        action.message
                    }
                }
                is ActionResolution.ExecuteCommand -> {
                    if (!isConnected) {
                        "⚠️ Your laptop is currently disconnected. Tap 'Connect' on the Home screen to link via Wi-Fi, then I can run commands on your laptop."
                    } else {
                        val result = TerminalManager.executeCommandWithOutput(action.command, timeoutMs = 3500)
                        if (result != null) {
                            when {
                                result.stdout.isNotBlank() -> {
                                    "💻 Output from $targetName:\n${result.stdout.trim()}"
                                }
                                result.stderr.isNotBlank() -> {
                                    "⚠️ Output from $targetName:\n${result.stderr.trim()}"
                                }
                                result.exitCode == 0 -> {
                                    "✅ Executed on your laptop: ${action.command}"
                                }
                                else -> {
                                    "⚠️ Command exited with code ${result.exitCode} on your laptop."
                                }
                            }
                        } else {
                            "⚡ Dispatched command to your $osName laptop ($targetName): ${action.command}"
                        }
                    }
                }
            }

            delay(200)
            messages.add(ChatMessage((System.currentTimeMillis() + 1).toString(), reply, isUser = false))
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val voiceManager = remember {
        VoiceManager(
            context = context,
            onPartial = { partial ->
                inputText = partial
            },
            onFinalResult = { result ->
                inputText = result
                handleSend(result)
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceManager.destroy()
        }
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceManager.startListening()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice commands", Toast.LENGTH_SHORT).show()
        }
    }

    fun onMicClick() {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPerm) {
            voiceManager.startListening()
        } else {
            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(isKeyboardOpen) {
        if (isKeyboardOpen && messages.isNotEmpty()) {
            delay(150)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val suggestedPrompts = listOf(
        "Open WhatsApp in Brave from Brave shortcuts",
        "Open Brave shortcuts",
        "Open Chrome on laptop",
        "Open VS Code",
        "Check battery",
        "Laptop status"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VexyronBg)
            .statusBarsPadding()
            .padding(top = 16.dp)
            .imePadding()
            .navigationBarsPadding()
            .padding(bottom = if (isKeyboardOpen) 8.dp else 92.dp)
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(VexyronGradientPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Vex AI",
                        tint = VexyronTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Vex AI",
                        color = VexyronTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isConnected) "Connected • ${if (hostOs != "Unknown") hostOs else "Laptop"}" else "Device Ecosystem Assistant",
                        color = if (isConnected) VexyronGreen else VexyronTextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            VexyronStatusBadge(
                text = if (isConnected) "Online" else "Ready",
                indicatorColor = if (isConnected) VexyronGreen else VexyronCyan
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CHAT MESSAGES LIST
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message = message)
            }
        }

        // SUGGESTED PROMPT CHIPS (Hide when typing for maximum chat visibility)
        if (!isKeyboardOpen) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suggestedPrompts) { prompt ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(VexyronSurfaceElevated)
                            .border(1.dp, VexyronBorder, RoundedCornerShape(20.dp))
                            .clickable { handleSend(prompt) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = prompt,
                            color = VexyronTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // MESSAGE INPUT BOX
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        text = if (voiceManager.isListening) "Listening... speak now" else "Ask Vex (e.g. open WhatsApp in Brave)...",
                        color = if (voiceManager.isListening) VexyronGreen else VexyronTextMuted,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { handleSend(inputText) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (voiceManager.isListening) VexyronGreen else VexyronCyan,
                    unfocusedBorderColor = VexyronBorder,
                    focusedTextColor = VexyronTextPrimary,
                    unfocusedTextColor = VexyronTextPrimary,
                    focusedContainerColor = VexyronCardBg,
                    unfocusedContainerColor = VexyronCardBg
                ),
                shape = RoundedCornerShape(22.dp)
            )

            // Mic Icon Button
            val isListening = voiceManager.isListening
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isListening) VexyronCyan.copy(alpha = 0.2f) else VexyronSurfaceElevated)
                    .border(
                        width = if (isListening) 2.dp else 1.dp,
                        color = if (isListening) VexyronGreen else VexyronBorder,
                        shape = CircleShape
                    )
                    .clickable { onMicClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = if (isListening) "Listening" else "Voice Input",
                    tint = if (isListening) VexyronGreen else VexyronTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Send Icon Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (inputText.isNotBlank()) VexyronCyan else VexyronSurfaceElevated)
                    .clickable(enabled = inputText.isNotBlank()) { handleSend(inputText) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (inputText.isNotBlank()) VexyronBg else VexyronTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val isOutput = message.text.startsWith("💻 Output") ||
            message.text.startsWith("⚠️ Output") ||
            (message.text.contains("\n") && !message.isUser)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 16.dp
                    )
                )
                .background(if (message.isUser) VexyronCyan.copy(alpha = 0.2f) else VexyronCardBg)
                .border(
                    width = 1.dp,
                    color = if (message.isUser) VexyronCyan.copy(alpha = 0.4f) else VexyronBorder,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            SelectionContainer {
                Text(
                    text = message.text,
                    color = if (message.isUser) VexyronTextPrimary else VexyronTextSecondary,
                    fontSize = 13.sp,
                    fontFamily = if (isOutput) FontFamily.Monospace else FontFamily.Default,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
