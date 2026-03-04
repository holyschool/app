package com.enderplusbayzuiship.edupage2.ui.settings

import androidx.lifecycle.ViewModel
import com.edupage.api.Edupage
import com.enderplusbayzuiship.edupage2.data.CredentialStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val edupage: Edupage,
    private val credentialStore: CredentialStore
) : ViewModel() {

    /** Clears saved credentials and resets the in-memory session. */
    fun logout() {
        credentialStore.clear()
        edupage.session.isLoggedIn = false
        edupage.session.data = null
        edupage.session.gsecHash = null
    }
}
