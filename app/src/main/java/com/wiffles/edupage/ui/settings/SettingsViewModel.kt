package com.wiffles.edupage.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.edupage.api.Edupage
import com.edupage.api.model.people.EduAccount
import com.wiffles.edupage.R
import com.wiffles.edupage.data.AccountProfile
import com.wiffles.edupage.data.AccountProfileStore
import com.wiffles.edupage.data.AppLanguage
import com.wiffles.edupage.data.AppPreferences
import com.wiffles.edupage.data.AppFontScale
import com.wiffles.edupage.data.AccentColor
import com.wiffles.edupage.data.BackendMode
import com.wiffles.edupage.data.BreakVisibility
import com.wiffles.edupage.data.CancelledLessonStyle
import com.wiffles.edupage.data.HapticIntensity
import com.wiffles.edupage.data.LessonGrouping
import com.wiffles.edupage.data.MotionBlurScope
import com.wiffles.edupage.data.MotionBlurStrength
import com.wiffles.edupage.data.CredentialStore
import com.wiffles.edupage.data.DarkModePreference
import com.wiffles.edupage.data.DataExporter
import com.wiffles.edupage.data.LockStore
import com.wiffles.edupage.data.AiCredentialsStore
import com.wiffles.edupage.data.GradesCache
import com.wiffles.edupage.data.MealsCache
import com.wiffles.edupage.data.SubjectStyle
import com.wiffles.edupage.data.SubjectStyleStore
import com.wiffles.edupage.data.TimetableCache
import com.wiffles.edupage.data.TimelineCache
import com.wiffles.edupage.network.BackendRegistrationManager
import com.wiffles.edupage.network.AiConfig
import com.wiffles.edupage.network.AiProvider
import com.wiffles.edupage.network.AiService
import com.wiffles.edupage.network.UpdateCenter
import dagger.hilt.android.qualifiers.ApplicationContext
import com.wiffles.edupage.notification.NotificationScheduler
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val edupage: Edupage,    private val credentialStore: CredentialStore,
    private val accountProfileStore: AccountProfileStore,
    private val appPreferences: AppPreferences,
    private val lockStore: LockStore,
    private val notificationScheduler: NotificationScheduler,
    private val timetableCache: TimetableCache,
    private val gradesCache: GradesCache,
    private val timelineCache: TimelineCache,
    private val mealsCache: MealsCache,
    private val subjectStyleStore: SubjectStyleStore,
    private val backendRegistrationManager: BackendRegistrationManager,
    private val dataExporter: DataExporter,
    private val updateChecker: com.wiffles.edupage.network.UpdateChecker,
    private val aiService: AiService,
    private val aiCredentialsStore: AiCredentialsStore,
    @ApplicationContext private val context: android.content.Context,
) : ViewModel() {

    sealed interface UpdateCheckState {
        data object Idle : UpdateCheckState
        data object Checking : UpdateCheckState
        data object UpToDate : UpdateCheckState
        data class Available(val info: com.wiffles.edupage.network.AppUpdateInfo) : UpdateCheckState
        data class Failed(val message: String?) : UpdateCheckState
    }

    private val _updateState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateState: StateFlow<UpdateCheckState> = _updateState.asStateFlow()

    fun checkForUpdates() {
        if (_updateState.value is UpdateCheckState.Checking) return
        viewModelScope.launch {
            _updateState.value = UpdateCheckState.Checking
            val current = runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrNull().orEmpty()
            val info = updateChecker.check(current, appPreferences.prereleaseUpdates)
            _updateState.value = if (info != null) {
                UpdateCheckState.Available(info)
            } else {
                UpdateCheckState.UpToDate
            }
        }
    }

    fun consumeUpdateState() {
        _updateState.value = UpdateCheckState.Idle
    }

    private val _downloadProgress = MutableStateFlow<Int?>(null)
    val downloadProgress: StateFlow<Int?> = _downloadProgress.asStateFlow()

    fun downloadUpdate(info: com.wiffles.edupage.network.AppUpdateInfo) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                _downloadProgress.value = 0
                val cleanVersion = info.versionName.replace(Regex("[^a-zA-Z0-9]"), "_")
                val dir = java.io.File(context.cacheDir, "updates").apply { mkdirs() }
                val file = java.io.File(dir, "Edupage2-$cleanVersion.apk")
                val url = java.net.URL(info.downloadUrl)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connect()
                if (connection.responseCode != java.net.HttpURLConnection.HTTP_OK) {
                    throw IllegalStateException("Server returned ${connection.responseCode}")
                }
                val fileLength = connection.contentLength
                connection.inputStream.use { input ->
                    file.outputStream().use { output ->
                        val data = ByteArray(8192)
                        var total = 0L
                        while (true) {
                            val count = input.read(data)
                            if (count == -1) break
                            output.write(data, 0, count)
                            total += count
                            if (fileLength > 0) {
                                _downloadProgress.value = ((total * 100) / fileLength).toInt().coerceIn(0, 100)
                            }
                        }
                    }
                }
                _downloadProgress.value = 100
                installApk(file)
            } catch (e: Exception) {
                Log.e(TAG, "update download failed", e)
                _downloadProgress.value = null
            }
        }
    }

    private suspend fun installApk(file: java.io.File) {
        withContext(kotlinx.coroutines.Dispatchers.Main) {
            _downloadProgress.value = null
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }.onFailure {
                Log.e(TAG, "could not launch installer", it)
            }
        }
    }

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    private val _breakVisibility = MutableStateFlow(appPreferences.breakVisibility)
    val breakVisibility: StateFlow<BreakVisibility> = _breakVisibility.asStateFlow()

    private val _showWeekends = MutableStateFlow(appPreferences.showWeekends)
    val showWeekends: StateFlow<Boolean> = _showWeekends.asStateFlow()

    private val _showSeconds = MutableStateFlow(appPreferences.showSeconds)
    val showSeconds: StateFlow<Boolean> = _showSeconds.asStateFlow()

    private val _mealsEnabled = MutableStateFlow(appPreferences.mealsEnabled)
    val mealsEnabled: StateFlow<Boolean> = _mealsEnabled.asStateFlow()

    private val _compactTimetable = MutableStateFlow(appPreferences.compactTimetable)
    val compactTimetable: StateFlow<Boolean> = _compactTimetable.asStateFlow()

    private val _autoRefreshIntervalMinutes = MutableStateFlow(appPreferences.autoRefreshIntervalMinutes)
    val autoRefreshIntervalMinutes: StateFlow<Int> = _autoRefreshIntervalMinutes.asStateFlow()

    private val _keepScreenAwake = MutableStateFlow(appPreferences.keepScreenAwake)
    val keepScreenAwake: StateFlow<Boolean> = _keepScreenAwake.asStateFlow()

    private val _messagesPriority = MutableStateFlow(appPreferences.messagesPriority)
    val messagesPriority: StateFlow<Boolean> = _messagesPriority.asStateFlow()

    private val _messagesNewOnTop = MutableStateFlow(appPreferences.messagesNewOnTop)
    val messagesNewOnTop: StateFlow<Boolean> = _messagesNewOnTop.asStateFlow()

    private val _subjectIconsEnabled = MutableStateFlow(appPreferences.subjectIconsEnabled)
    val subjectIconsEnabled: StateFlow<Boolean> = _subjectIconsEnabled.asStateFlow()

    val subjectStyles: StateFlow<Map<String, SubjectStyle>> = subjectStyleStore.styles

    private val _aiQuizEnabled = MutableStateFlow(appPreferences.aiQuizEnabled)
    val aiQuizEnabled: StateFlow<Boolean> = _aiQuizEnabled.asStateFlow()

    val aiConfig: StateFlow<AiConfig> = aiCredentialsStore.config

    sealed interface AiModelsState {
        data object Idle : AiModelsState
        data object Loading : AiModelsState
        data class Loaded(val models: List<String>) : AiModelsState
        data class Failed(val message: String) : AiModelsState
    }

    private val _aiModels = MutableStateFlow<AiModelsState>(AiModelsState.Idle)
    val aiModels: StateFlow<AiModelsState> = _aiModels.asStateFlow()

    sealed interface AiTestState {
        data object Idle : AiTestState
        data object Testing : AiTestState
        data object Ok : AiTestState
        data class Failed(val message: String) : AiTestState
    }

    private val _aiTestState = MutableStateFlow<AiTestState>(AiTestState.Idle)
    val aiTestState: StateFlow<AiTestState> = _aiTestState.asStateFlow()

    private val _cloudEnabled = MutableStateFlow(appPreferences.cloudEnabled)
    val cloudEnabled: StateFlow<Boolean> = _cloudEnabled.asStateFlow()

    private val _enhancedAppearanceEnabled = MutableStateFlow(appPreferences.enhancedAppearanceEnabled)
    val enhancedAppearanceEnabled: StateFlow<Boolean> = _enhancedAppearanceEnabled.asStateFlow()

    private val _customAccentArgb = MutableStateFlow(appPreferences.customAccentArgb)
    val customAccentArgb: StateFlow<Int?> = _customAccentArgb.asStateFlow()

    private val _fontScale = MutableStateFlow(appPreferences.fontScale)
    val fontScale: StateFlow<AppFontScale> = _fontScale.asStateFlow()

    private val _defaultTab = MutableStateFlow(appPreferences.defaultTab)
    val defaultTab: StateFlow<Int> = _defaultTab.asStateFlow()

    private val _firstDayOfWeek = MutableStateFlow(appPreferences.firstDayOfWeek)
    val firstDayOfWeek: StateFlow<Int> = _firstDayOfWeek.asStateFlow()

    private val _cancelledLessonStyle = MutableStateFlow(appPreferences.cancelledLessonStyle)
    val cancelledLessonStyle: StateFlow<CancelledLessonStyle> = _cancelledLessonStyle.asStateFlow()

    private val _lessonGrouping = MutableStateFlow(appPreferences.lessonGrouping)
    val lessonGrouping: StateFlow<LessonGrouping> = _lessonGrouping.asStateFlow()

    private val _liveClassNotif = MutableStateFlow(appPreferences.liveClassNotif)
    val liveClassNotif: StateFlow<Boolean> = _liveClassNotif.asStateFlow()

    private val _hapticIntensity = MutableStateFlow(appPreferences.hapticIntensity)
    val hapticIntensity: StateFlow<HapticIntensity> = _hapticIntensity.asStateFlow()

    private val _motionBlurEnabled = MutableStateFlow(appPreferences.motionBlurEnabled)
    val motionBlurEnabled: StateFlow<Boolean> = _motionBlurEnabled.asStateFlow()

    private val _motionBlurScope = MutableStateFlow(appPreferences.motionBlurScope)
    val motionBlurScope: StateFlow<MotionBlurScope> = _motionBlurScope.asStateFlow()

    private val _motionBlurStrength = MutableStateFlow(appPreferences.motionBlurStrength)
    val motionBlurStrength: StateFlow<MotionBlurStrength> = _motionBlurStrength.asStateFlow()

    private val _liveClassShowSubject = MutableStateFlow(appPreferences.liveClassShowSubject)
    val liveClassShowSubject: StateFlow<Boolean> = _liveClassShowSubject.asStateFlow()

    private val _liveClassShortSubject = MutableStateFlow(appPreferences.liveClassShortSubject)
    val liveClassShortSubject: StateFlow<Boolean> = _liveClassShortSubject.asStateFlow()

    private val _liveClassShowRoom = MutableStateFlow(appPreferences.liveClassShowRoom)
    val liveClassShowRoom: StateFlow<Boolean> = _liveClassShowRoom.asStateFlow()

    private val _liveClassShowTeacher = MutableStateFlow(appPreferences.liveClassShowTeacher)
    val liveClassShowTeacher: StateFlow<Boolean> = _liveClassShowTeacher.asStateFlow()

    private val _liveClassShowProgress = MutableStateFlow(appPreferences.liveClassShowProgress)
    val liveClassShowProgress: StateFlow<Boolean> = _liveClassShowProgress.asStateFlow()

    private val _autoCheckUpdates = MutableStateFlow(appPreferences.autoCheckUpdates)
    val autoCheckUpdates: StateFlow<Boolean> = _autoCheckUpdates.asStateFlow()

    private val _prereleaseUpdates = MutableStateFlow(appPreferences.prereleaseUpdates)
    val prereleaseUpdates: StateFlow<Boolean> = _prereleaseUpdates.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(appPreferences.notificationsEnabled)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _notifGradesEnabled = MutableStateFlow(appPreferences.notifGradesEnabled)
    val notifGradesEnabled: StateFlow<Boolean> = _notifGradesEnabled.asStateFlow()

    private val _notifMessagesEnabled = MutableStateFlow(appPreferences.notifMessagesEnabled)
    val notifMessagesEnabled: StateFlow<Boolean> = _notifMessagesEnabled.asStateFlow()

    private val _notifSubstitutionsEnabled = MutableStateFlow(appPreferences.notifSubstitutionsEnabled)
    val notifSubstitutionsEnabled: StateFlow<Boolean> = _notifSubstitutionsEnabled.asStateFlow()

    private val _notifCheckIntervalMinutes = MutableStateFlow(appPreferences.notifCheckIntervalMinutes)
    val notifCheckIntervalMinutes: StateFlow<Int> = _notifCheckIntervalMinutes.asStateFlow()

    private val _darkMode = MutableStateFlow(appPreferences.darkMode)
    val darkMode: StateFlow<DarkModePreference> = _darkMode.asStateFlow()

    private val _useAmoled = MutableStateFlow(appPreferences.useAmoled)
    val useAmoled: StateFlow<Boolean> = _useAmoled.asStateFlow()

    private val _accentColor = MutableStateFlow(appPreferences.accentColor)
    val accentColor: StateFlow<AccentColor> = _accentColor.asStateFlow()

    private val _dynamicColor = MutableStateFlow(appPreferences.dynamicColor)
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _forceHighRefreshRate = MutableStateFlow(appPreferences.forceHighRefreshRate)
    val forceHighRefreshRate: StateFlow<Boolean> = _forceHighRefreshRate.asStateFlow()

    val lockEnabled: StateFlow<Boolean> = lockStore.isEnabledFlow
    val isBiometricEnabled: StateFlow<Boolean> = lockStore.isBiometricEnabledFlow

    private val _appLanguage = MutableStateFlow(appPreferences.appLanguage)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    private val _backendMode = MutableStateFlow(appPreferences.backendMode)
    val backendMode: StateFlow<BackendMode> = _backendMode.asStateFlow()

    private val _backendCustomUrl = MutableStateFlow(appPreferences.backendCustomUrl)
    val backendCustomUrl: StateFlow<String> = _backendCustomUrl.asStateFlow()

    private val _backendCustomKey = MutableStateFlow(appPreferences.backendCustomKey)
    val backendCustomKey: StateFlow<String> = _backendCustomKey.asStateFlow()

    private val _backendRegisterStatus = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val backendRegisterStatus: SharedFlow<Boolean> = _backendRegisterStatus.asSharedFlow()

    private val _backendSyncStatus = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val backendSyncStatus: SharedFlow<Boolean> = _backendSyncStatus.asSharedFlow()

    private val _backendDeleteStatus = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val backendDeleteStatus: SharedFlow<Boolean> = _backendDeleteStatus.asSharedFlow()

    private val _recreateActivity = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val recreateActivity: SharedFlow<Unit> = _recreateActivity.asSharedFlow()

    private val _children = MutableStateFlow<List<EduAccount>?>(null)
    val children: StateFlow<List<EduAccount>?> = _children.asStateFlow()

    private val _currentChild = MutableStateFlow<EduAccount?>(null)
    val currentChild: StateFlow<EduAccount?> = _currentChild.asStateFlow()

    val isParent: Boolean
        get() = edupage.isParent

    private val _parentSwitchEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val parentSwitchEvent: SharedFlow<Unit> = _parentSwitchEvent.asSharedFlow()

    private val _profiles = MutableStateFlow<List<AccountProfile>>(accountProfileStore.loadProfiles())
    val profiles: StateFlow<List<AccountProfile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow<AccountProfile?>(accountProfileStore.activeProfile())
    val activeProfile: StateFlow<AccountProfile?> = _activeProfile.asStateFlow()

    private val _switchAccountEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val switchAccountEvent: SharedFlow<Unit> = _switchAccountEvent.asSharedFlow()

    init {
        loadChildren()
    }

    fun refreshAccounts() {
        _profiles.value = accountProfileStore.loadProfiles()
        _activeProfile.value = accountProfileStore.activeProfile()
    }

    fun removeAccount(id: String) {
        accountProfileStore.removeProfile(id)
        refreshAccounts()
    }

    fun switchToAccount(id: String) {
        val profile = accountProfileStore.loadProfiles().find { it.id == id } ?: return
        viewModelScope.launch {
            credentialStore.save(profile.username, profile.password, profile.subdomain, profile.sessionId)
            accountProfileStore.setActiveProfile(id)
            refreshAccounts()
            clearAllCaches()
            _switchAccountEvent.emit(Unit)
            Log.i(TAG, "Switching to account profile ${profile.username}@${profile.subdomain}")
        }
    }

    private fun loadChildren() {
        val childrenList = edupage.children
        _children.value = childrenList
        if (childrenList != null) {
            val selectedId = appPreferences.selectedChildId
            _currentChild.value = if (selectedId > 0) {
                childrenList.find { it.personId == selectedId }
            } else {
                null
            }
        }
    }

    fun switchToChild(child: EduAccount) {
        viewModelScope.launch {
            try {
                edupage.switchToChild(child)
                appPreferences.selectedChildId = child.personId
                _currentChild.value = child
                clearAllCaches()
                _parentSwitchEvent.emit(Unit)
                Log.i(TAG, "Switched to child: ${child.name} (${child.personId})")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to switch to child: ${e.message}", e)
            }
        }
    }

    fun switchToParent() {
        viewModelScope.launch {
            try {
                edupage.switchToParent()
                appPreferences.selectedChildId = -1
                _currentChild.value = null
                clearAllCaches()
                _parentSwitchEvent.emit(Unit)
                Log.i(TAG, "Switched back to parent view")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to switch to parent: ${e.message}", e)
            }
        }
    }

    private fun clearAllCaches() {
        gradesCache.clear()
        timelineCache.clear()
        timetableCache.clear()
        mealsCache.clear()
        appPreferences.clearNotifiedIds()
        loadChildren()
    }

    fun setBreakVisibility(value: BreakVisibility) {
        appPreferences.breakVisibility = value
        _breakVisibility.value = value
    }

    fun setShowWeekends(value: Boolean) {
        appPreferences.showWeekends = value
        _showWeekends.value = value
    }

    fun setShowSeconds(value: Boolean) {
        appPreferences.showSeconds = value
        _showSeconds.value = value
    }

    fun setMealsEnabled(value: Boolean) {
        appPreferences.mealsEnabled = value
        _mealsEnabled.value = value
    }

    fun setCompactTimetable(value: Boolean) {
        appPreferences.compactTimetable = value
        _compactTimetable.value = value
    }

    fun setAutoRefreshIntervalMinutes(value: Int) {
        val clamped = value.coerceIn(0, 1440)
        appPreferences.autoRefreshIntervalMinutes = clamped
        _autoRefreshIntervalMinutes.value = clamped
    }

    fun setKeepScreenAwake(value: Boolean) {
        appPreferences.keepScreenAwake = value
        _keepScreenAwake.value = value
    }

    fun setMessagesPriority(value: Boolean) {
        appPreferences.messagesPriority = value
        _messagesPriority.value = value
    }

    fun setMessagesNewOnTop(value: Boolean) {
        appPreferences.messagesNewOnTop = value
        _messagesNewOnTop.value = value
    }

    fun setSubjectIconsEnabled(value: Boolean) {
        appPreferences.subjectIconsEnabled = value
        _subjectIconsEnabled.value = value
    }

    fun setAiQuizEnabled(value: Boolean) {
        appPreferences.aiQuizEnabled = value
        _aiQuizEnabled.value = value
    }

    fun setAiProvider(provider: AiProvider) {
        aiCredentialsStore.setProvider(provider)
        _aiModels.value = AiModelsState.Idle
        _aiTestState.value = AiTestState.Idle
    }

    fun aiConfigFor(provider: AiProvider): AiConfig = aiCredentialsStore.read(provider)

    fun setAiApiKey(provider: AiProvider, value: String) {
        aiCredentialsStore.setApiKey(provider, value)
    }

    fun setAiModel(provider: AiProvider, value: String) {
        aiCredentialsStore.setModel(provider, value)
    }

    fun setAiCustomBaseUrl(value: String) {
        aiCredentialsStore.setCustomBaseUrl(value)
    }

    fun clearAiApiKey(provider: AiProvider) {
        aiCredentialsStore.clearApiKey(provider)
    }

    fun loadAiModels() {
        val config = aiCredentialsStore.current()
        if (config.apiKey.isBlank()) {
            _aiModels.value = AiModelsState.Failed(context.getString(R.string.settings_ai_key_empty))
            return
        }
        if (_aiModels.value is AiModelsState.Loading) return
        viewModelScope.launch {
            _aiModels.value = AiModelsState.Loading
            val result = aiService.listModels(config)
            _aiModels.value = result.fold(
                onSuccess = { AiModelsState.Loaded(it) },
                onFailure = { AiModelsState.Failed(it.message ?: "Unknown error") },
            )
        }
    }

    fun testAiConnection() {
        val config = aiCredentialsStore.current()
        if (config.apiKey.isBlank()) {
            _aiTestState.value = AiTestState.Failed(context.getString(R.string.settings_ai_key_empty))
            return
        }
        if (_aiTestState.value is AiTestState.Testing) return
        viewModelScope.launch {
            _aiTestState.value = AiTestState.Testing
            val result = aiService.testConnection(config)
            _aiTestState.value = result.fold(
                onSuccess = { AiTestState.Ok },
                onFailure = { AiTestState.Failed(it.message ?: "Unknown error") },
            )
        }
    }

    fun consumeAiTestState() {
        if (_aiTestState.value !is AiTestState.Testing) _aiTestState.value = AiTestState.Idle
    }

    fun setCloudEnabled(value: Boolean) {
        appPreferences.cloudEnabled = value
        _cloudEnabled.value = value
    }

    fun setEnhancedAppearanceEnabled(value: Boolean) {
        appPreferences.enhancedAppearanceEnabled = value
        _enhancedAppearanceEnabled.value = value
    }

    fun setCustomAccentArgb(value: Int?) {
        appPreferences.customAccentArgb = value
        _customAccentArgb.value = value
    }

    fun setFontScale(value: AppFontScale) {
        appPreferences.fontScale = value
        _fontScale.value = value
    }

    fun setDynamicColor(value: Boolean) {
        appPreferences.dynamicColor = value
        _dynamicColor.value = value
    }

    fun setForceHighRefreshRate(value: Boolean) {
        appPreferences.forceHighRefreshRate = value
        _forceHighRefreshRate.value = value
    }

    fun setDefaultTab(value: Int) {
        val clamped = value.coerceIn(0, 4)
        appPreferences.defaultTab = clamped
        _defaultTab.value = clamped
    }

    fun setFirstDayOfWeek(value: Int) {
        val clamped = value.coerceIn(0, 1)
        appPreferences.firstDayOfWeek = clamped
        _firstDayOfWeek.value = clamped
    }

    fun clearCaches() {
        viewModelScope.launch {
            timetableCache.clear()
            gradesCache.clear()
            timelineCache.clear()
            mealsCache.clear()
            Log.i(TAG, "Cleared timetable/grades/timeline/meals caches")
        }
    }

    fun setBiometricEnabled(value: Boolean) {
        lockStore.setBiometricEnabled(value)
    }

    fun setCancelledLessonStyle(value: CancelledLessonStyle) {
        appPreferences.cancelledLessonStyle = value
        _cancelledLessonStyle.value = value
    }

    fun setLessonGrouping(value: LessonGrouping) {
        appPreferences.lessonGrouping = value
        _lessonGrouping.value = value
    }

    fun setLiveClassNotif(value: Boolean) {
        appPreferences.liveClassNotif = value
        _liveClassNotif.value = value
    }

    fun setHapticIntensity(value: HapticIntensity) {
        if (value == _hapticIntensity.value) return
        appPreferences.hapticIntensity = value
        _hapticIntensity.value = value
        com.wiffles.edupage.ui.util.HapticGate.intensity = value
    }

    fun setMotionBlurEnabled(value: Boolean) {
        appPreferences.motionBlurEnabled = value
        _motionBlurEnabled.value = value
        com.wiffles.edupage.ui.modifiers.MotionBlurGate.enabled = value
    }

    fun setMotionBlurScope(value: MotionBlurScope) {
        appPreferences.motionBlurScope = value
        _motionBlurScope.value = value
        com.wiffles.edupage.ui.modifiers.MotionBlurGate.scope = value
    }

    fun setMotionBlurStrength(value: MotionBlurStrength) {
        appPreferences.motionBlurStrength = value
        _motionBlurStrength.value = value
        com.wiffles.edupage.ui.modifiers.MotionBlurGate.scale = value.scale
    }

    fun setLiveClassShowSubject(value: Boolean) {
        appPreferences.liveClassShowSubject = value
        _liveClassShowSubject.value = value
    }

    fun setLiveClassShortSubject(value: Boolean) {
        appPreferences.liveClassShortSubject = value
        _liveClassShortSubject.value = value
    }

    fun setLiveClassShowRoom(value: Boolean) {
        appPreferences.liveClassShowRoom = value
        _liveClassShowRoom.value = value
    }

    fun setLiveClassShowTeacher(value: Boolean) {
        appPreferences.liveClassShowTeacher = value
        _liveClassShowTeacher.value = value
    }

    fun setLiveClassShowProgress(value: Boolean) {
        appPreferences.liveClassShowProgress = value
        _liveClassShowProgress.value = value
    }

    fun setAutoCheckUpdates(value: Boolean) {
        appPreferences.autoCheckUpdates = value
        _autoCheckUpdates.value = value
    }

    fun setPrereleaseUpdates(value: Boolean) {
        appPreferences.prereleaseUpdates = value
        _prereleaseUpdates.value = value
    }

    fun skipUpdate(versionName: String) {
        appPreferences.skippedUpdateVersion = versionName
        UpdateCenter.dismiss()
        _updateState.value = UpdateCheckState.Idle
    }

    fun shouldAutoCheckUpdates(): Boolean = appPreferences.autoCheckUpdates

    fun setNotifGradesEnabled(value: Boolean) {
        appPreferences.notifGradesEnabled = value
        _notifGradesEnabled.value = value
        updateWorker()
    }

    fun setNotifMessagesEnabled(value: Boolean) {
        appPreferences.notifMessagesEnabled = value
        _notifMessagesEnabled.value = value
        updateWorker()
    }

    fun setNotifSubstitutionsEnabled(value: Boolean) {
        appPreferences.notifSubstitutionsEnabled = value
        _notifSubstitutionsEnabled.value = value
        updateWorker()
    }

    fun setNotifCheckIntervalMinutes(value: Int) {
        appPreferences.notifCheckIntervalMinutes = value
        _notifCheckIntervalMinutes.value = value
        appPreferences.lastNotificationFetchTimestamp = 0L
        updateWorker()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        appPreferences.notificationsEnabled = enabled
        _notificationsEnabled.value = enabled
        updateWorker()
        if (!enabled) {
            appPreferences.lastNotificationFetchTimestamp = 0L
            appPreferences.lastTimelineId = -1
        }
    }

    private fun updateWorker() {
        val enabled = appPreferences.notificationsEnabled
        val anyTypeEnabled = appPreferences.notifGradesEnabled ||
                           appPreferences.notifMessagesEnabled ||
                           appPreferences.notifSubstitutionsEnabled

        if (enabled && anyTypeEnabled) {
            notificationScheduler.scheduleGradeMessageCheck()
        } else {
            notificationScheduler.cancelGradeMessageCheck()
        }
    }

    fun setDarkMode(value: DarkModePreference) {
        if (value == _darkMode.value) return
        appPreferences.darkMode = value
        _darkMode.value = value
    }

    fun setUseAmoled(value: Boolean) {
        if (value == _useAmoled.value) return
        appPreferences.useAmoled = value
        _useAmoled.value = value
    }

    fun setAccentColor(value: AccentColor) {
        // A preset selection must override any previously chosen custom accent,
        // otherwise the theme keeps using the custom color and appears stuck.
        val hasCustom = _customAccentArgb.value != null
        if (value == _accentColor.value && !hasCustom) return
        appPreferences.accentColor = value
        _accentColor.value = value
        if (hasCustom) {
            appPreferences.customAccentArgb = null
            _customAccentArgb.value = null
        }
    }

    fun setAppLanguage(value: AppLanguage) {
        if (value == _appLanguage.value) return
        appPreferences.appLanguage = value
        _appLanguage.value = value
        viewModelScope.launch { _recreateActivity.emit(Unit) }
    }

    fun setBackendBaseUrl(value: String) {
        appPreferences.backendCustomUrl = value
        _backendCustomUrl.value = value
    }

    fun setBackendApiKey(value: String) {
        appPreferences.backendCustomKey = value
        _backendCustomKey.value = value
    }

    fun setBackendMode(value: BackendMode) {
        appPreferences.backendMode = value
        _backendMode.value = value
    }

    fun registerDevice() {
        val pushReady = runCatching { com.google.firebase.FirebaseApp.getInstance() }.isSuccess
        if (!pushReady) {
            Log.w(TAG, "FCM not configured, cannot register device")
            return
        }
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            viewModelScope.launch {
                val ok = backendRegistrationManager.registerIfPossible(token)
                _backendRegisterStatus.emit(ok)
            }
        }
    }

    fun syncReadNow() {
        viewModelScope.launch {
            val localSeen = appPreferences.getSeenTimelineIds()
            val result = backendRegistrationManager.syncReadState(localSeen)
            if (result.ok && result.ids.isNotEmpty()) {
                appPreferences.markTimelineIdsSeen(result.ids)
            }
            _backendSyncStatus.emit(result.ok)
        }
    }

    fun deleteAllBackendData() {
        viewModelScope.launch {
            val ok = backendRegistrationManager.deleteAllData()
            _backendDeleteStatus.emit(ok)
        }
    }

    fun logout() {
        notificationScheduler.cancelGradeMessageCheck()
        com.wiffles.edupage.notification.ClassLiveController.stop(context)
        viewModelScope.launch {
            timetableCache.clear()
            gradesCache.clear()
            timelineCache.clear()
            mealsCache.clear()
        }
        credentialStore.clear()
        edupage.session.cookieJar.clear()
        edupage.session.isLoggedIn = false
        edupage.session.data = null
        edupage.session.gsecHash = null
    }

    fun exportDataJson(): String = dataExporter.exportAsJson()

    fun appVersionName(): String = appPreferences.appVersionName

    fun enableLock(pin: String) {
        lockStore.enable(pin)
    }

    fun disableLock() {
        lockStore.disable()
    }

    fun verifyPin(pin: String): Boolean = lockStore.verifyPin(pin)

    fun changePin(currentPin: String, newPin: String): Boolean = lockStore.changePin(currentPin, newPin)
}

