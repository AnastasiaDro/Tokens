package com.cerebus.tokens.presentation.tokens_screen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cerebus.tokens.data.TokensRepositoryImpl
import com.cerebus.tokens.domain.usecases.tokens.ChangeTokensNumberUseCase
import com.cerebus.tokens.domain.usecases.tokens.CheckTokenUseCase
import com.cerebus.tokens.domain.usecases.tokens.CheckTokensAreGrappedUseCase
import com.cerebus.tokens.domain.usecases.tokens.ClearAllTokensUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetAllTokensUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetCheckedColorUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetMaxTokensNumberUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetMinTokensNumberUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetTokensNumberUseCase
import com.cerebus.tokens.domain.usecases.tokens.UncheckTokenUseCase
import com.cerebus.tokens.presentation.settings_screen.SettingsViewModelFactory

class TokensViewModelFactory(context: Context) : ViewModelProvider.Factory {

    private val repository = TokensRepositoryImpl(context.getSharedPreferences(
        SettingsViewModelFactory.TOKENS_PREFERENCES, Context.MODE_PRIVATE))

    private val getCheckedColorUseCase by lazy(LazyThreadSafetyMode.NONE) {
        GetCheckedColorUseCase(repository)
    }

    private val changeTokensNumberUseCase by lazy(LazyThreadSafetyMode.NONE) {
        ChangeTokensNumberUseCase(repository)
    }

    private val clearAllTokensUseCase by lazy(LazyThreadSafetyMode.NONE) {
        ClearAllTokensUseCase(repository)
    }

    private val checkTokenUseCase by lazy(LazyThreadSafetyMode.NONE) {
        CheckTokenUseCase(repository)
    }

    private val uncheckTokenUseCase by lazy(LazyThreadSafetyMode.NONE) {
        UncheckTokenUseCase(repository)
    }

    private val getTokensNumberUseCase by lazy(LazyThreadSafetyMode.NONE) {
        GetTokensNumberUseCase(repository)
    }

    private val checkTokensAreGrappedUseCase by lazy(LazyThreadSafetyMode.NONE) {
        CheckTokensAreGrappedUseCase(repository)
    }

    private val getAllTokensUseCase by lazy(LazyThreadSafetyMode.NONE) {
        GetAllTokensUseCase(repository)
    }

    private val getMinTokensNumberUseCase by lazy(LazyThreadSafetyMode.NONE) {
        GetMinTokensNumberUseCase(repository)
    }

    private val getMaxTokensNumberUseCase by lazy(LazyThreadSafetyMode.NONE) {
        GetMaxTokensNumberUseCase(repository)
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TokensViewModel(
            getCheckedColorUseCase = getCheckedColorUseCase,
            changeTokensNumberUseCase = changeTokensNumberUseCase,
            clearAllTokensUseCase = clearAllTokensUseCase,
            checkTokenUseCase = checkTokenUseCase,
            uncheckTokenUseCase = uncheckTokenUseCase,
            getTokensNumberUseCase = getTokensNumberUseCase,
            checkTokensAreGrappedUseCase = checkTokensAreGrappedUseCase,
            getAllTokensUseCase = getAllTokensUseCase,
            getMinTokensNumberUseCase = getMinTokensNumberUseCase,
            getMaxTokensNumberUseCase = getMaxTokensNumberUseCase
        ) as T
    }
}
