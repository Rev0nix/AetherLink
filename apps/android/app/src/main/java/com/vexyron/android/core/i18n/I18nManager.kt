package com.vexyron.android.core.i18n

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class LanguageOption(
    val code: String,
    val name: String,
    val nativeName: String
)

object I18nManager {
    private const val PREFS_NAME = "vexyron_prefs"
    private const val KEY_LANG = "app_language"

    val supportedLanguages = listOf(
        LanguageOption("en", "English", "English"),
        LanguageOption("es", "Spanish", "Español"),
        LanguageOption("de", "German", "Deutsch"),
        LanguageOption("fr", "French", "Français"),
        LanguageOption("hi", "Hindi", "हिन्दी"),
        LanguageOption("zh", "Chinese", "简体中文"),
        LanguageOption("ja", "Japanese", "日本語"),
        LanguageOption("pt", "Portuguese", "Português")
    )

    var currentLanguageCode by mutableStateOf("en")
        private set

    val currentLanguage: LanguageOption
        get() = supportedLanguages.find { it.code == currentLanguageCode } ?: supportedLanguages[0]

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentLanguageCode = prefs.getString(KEY_LANG, "en") ?: "en"
    }

    fun setLanguage(code: String, context: Context? = null) {
        if (supportedLanguages.any { it.code == code }) {
            currentLanguageCode = code
            context?.let {
                it.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_LANG, code)
                    .apply()
            }
        }
    }

    private val translations = mapOf(
        "en" to mapOf(
            "settings.title" to "Settings",
            "settings.general" to "General",
            "settings.appearance" to "Appearance",
            "settings.language" to "Language",
            "settings.startup" to "Startup Listener",
            "settings.devices" to "Devices & Permissions",
            "settings.network" to "Network & Discovery",
            "settings.ai" to "Vex AI",
            "settings.about" to "About & Updates",
            "settings.check_updates" to "Check for Updates",
            "settings.clear_cache" to "Clear Connection Cache",
            "update.available" to "Update Available",
            "update.download" to "Download & Install Update",
            "update.dismiss" to "Later",
            "update.uptodate" to "VEXYRON is up to date"
        ),
        "es" to mapOf(
            "settings.title" to "Configuración",
            "settings.general" to "General",
            "settings.appearance" to "Apariencia",
            "settings.language" to "Idioma",
            "settings.startup" to "Servicio de inicio",
            "settings.devices" to "Dispositivos y permisos",
            "settings.network" to "Red y detección",
            "settings.ai" to "Vex IA",
            "settings.about" to "Acerca de y actualizaciones",
            "settings.check_updates" to "Buscar actualizaciones",
            "settings.clear_cache" to "Borrar caché de conexión",
            "update.available" to "Actualización disponible",
            "update.download" to "Descargar e instalar",
            "update.dismiss" to "Más tarde",
            "update.uptodate" to "VEXYRON está actualizado"
        ),
        "de" to mapOf(
            "settings.title" to "Einstellungen",
            "settings.general" to "Allgemein",
            "settings.appearance" to "Erscheinungsbild",
            "settings.language" to "Sprache",
            "settings.startup" to "Autostart-Dienst",
            "settings.devices" to "Geräte & Berechtigungen",
            "settings.network" to "Netzwerk & Erkennung",
            "settings.ai" to "Vex KI",
            "settings.about" to "Über & Aktualisierungen",
            "settings.check_updates" to "Nach Updates suchen",
            "settings.clear_cache" to "Verbindungscache leeren",
            "update.available" to "Update verfügbar",
            "update.download" to "Herunterladen & Installieren",
            "update.dismiss" to "Später",
            "update.uptodate" to "VEXYRON ist auf dem neuesten Stand"
        ),
        "fr" to mapOf(
            "settings.title" to "Paramètres",
            "settings.general" to "Général",
            "settings.appearance" to "Apparence",
            "settings.language" to "Langue",
            "settings.startup" to "Service au démarrage",
            "settings.devices" to "Appareils et autorisations",
            "settings.network" to "Réseau et découverte",
            "settings.ai" to "IA Vex",
            "settings.about" to "À propos et mises à jour",
            "settings.check_updates" to "Rechercher des mises à jour",
            "settings.clear_cache" to "Vider le cache de connexion",
            "update.available" to "Mise à jour disponible",
            "update.download" to "Télécharger et installer",
            "update.dismiss" to "Plus tard",
            "update.uptodate" to "VEXYRON est à jour"
        ),
        "hi" to mapOf(
            "settings.title" to "सेटिंग्स",
            "settings.general" to "सामान्य",
            "settings.appearance" to "थीम और दिखावट",
            "settings.language" to "भाषा",
            "settings.startup" to "स्टार्टअप सर्विस",
            "settings.devices" to "डिवाइस और अनुमतियाँ",
            "settings.network" to "नेटवर्क और खोज",
            "settings.ai" to "ईथर एआई",
            "settings.about" to "के बारे में और अपडेट",
            "settings.check_updates" to "अपडेट की जांच करें",
            "settings.clear_cache" to "कनेक्शन कैश साफ़ करें",
            "update.available" to "नया अपडेट उपलब्ध है",
            "update.download" to "डाउनलोड और इंस्टॉल करें",
            "update.dismiss" to "बाद में",
            "update.uptodate" to "ईथरलिंक अप-टू-डेट है"
        ),
        "zh" to mapOf(
            "settings.title" to "设置",
            "settings.general" to "通用",
            "settings.appearance" to "外观主题",
            "settings.language" to "语言",
            "settings.startup" to "自启监听",
            "settings.devices" to "设备与权限",
            "settings.network" to "网络与发现",
            "settings.ai" to "Vex AI 助手",
            "settings.about" to "关于与更新",
            "settings.check_updates" to "检查更新",
            "settings.clear_cache" to "清除连接缓存",
            "update.available" to "有可用更新",
            "update.download" to "下载并安装更新",
            "update.dismiss" to "稍后",
            "update.uptodate" to "VEXYRON 已是最新版本"
        ),
        "ja" to mapOf(
            "settings.title" to "設定",
            "settings.general" to "一般",
            "settings.appearance" to "外観テーマ",
            "settings.language" to "言語",
            "settings.startup" to "起動リスナー",
            "settings.devices" to "デバイスと権限",
            "settings.network" to "ネットワークと検出",
            "settings.ai" to "Vex AI",
            "settings.about" to "情報と更新",
            "settings.check_updates" to "更新を確認",
            "settings.clear_cache" to "接続キャッシュを消去",
            "update.available" to "アップデートがあります",
            "update.download" to "ダウンロードしてインストール",
            "update.dismiss" to "後で",
            "update.uptodate" to "最新バージョンです"
        ),
        "pt" to mapOf(
            "settings.title" to "Configurações",
            "settings.general" to "Geral",
            "settings.appearance" to "Aparência",
            "settings.language" to "Idioma",
            "settings.startup" to "Serviço de Inicialização",
            "settings.devices" to "Dispositivos e Permissões",
            "settings.network" to "Rede e Descoberta",
            "settings.ai" to "Vex IA",
            "settings.about" to "Sobre e Atualizações",
            "settings.check_updates" to "Verificar Atualizações",
            "settings.clear_cache" to "Limpar Cache de Conexão",
            "update.available" to "Atualização Disponível",
            "update.download" to "Baixar e Instalar",
            "update.dismiss" to "Depois",
            "update.uptodate" to "VEXYRON está atualizado"
        )
    )

    fun t(key: String): String {
        return translations[currentLanguageCode]?.get(key)
            ?: translations["en"]?.get(key)
            ?: key
    }
}
