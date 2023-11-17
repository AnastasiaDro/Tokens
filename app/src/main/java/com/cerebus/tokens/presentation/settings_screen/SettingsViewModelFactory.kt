package com.cerebus.tokens.presentation.settings_screen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cerebus.tokens.data.TokensRepositoryImpl
import com.cerebus.tokens.domain.repository.TokensRepository
import com.cerebus.tokens.domain.usecases.tokens.ChangeCheckedColorUseCase
import com.cerebus.tokens.domain.usecases.tokens.ChangeTokensNumberUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetCheckedColorUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetMaxTokensNumberUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetMinTokensNumberUseCase
import com.cerebus.tokens.domain.usecases.tokens.GetTokensNumberUseCase

class SettingsViewModelFactory(context: Context): ViewModelProvider.Factory {

    private val repository: TokensRepository = TokensRepositoryImpl(context.getSharedPreferences(TOKENS_PREFERENCES, Context.MODE_PRIVATE))

    private val changeTokensNumberUseCase by lazy(LazyThreadSafetyMode.NONE) {
        ChangeTokensNumberUseCase(repository)
    }

    private val getTokensNumberUseCase by lazy(LazyThreadSafetyMode.NONE) {
        GetTokensNumberUseCase(repository)
    }

    private val getMinTokensNumberUseCase by lazy(LazyThreadSafetyMode.NONE) {
        GetMinTokensNumberUseCase(repository)
    }

    private val getMaxTokensNumberUseCase by lazy(LazyThreadSafetyMode.NONE) {
        GetMaxTokensNumberUseCase(repository)
    }

    private val getChangeCheckedColorUseCase by lazy(LazyThreadSafetyMode.NONE) {
        ChangeCheckedColorUseCase(repository)
    }

    private val getCheckedColorUseCase by lazy(LazyThreadSafetyMode.NONE) {
        GetCheckedColorUseCase(repository)
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(
            changeTokensNumberUseCase = changeTokensNumberUseCase,
            getTokensNumberUseCase = getTokensNumberUseCase,
            getMinTokensNumberUseCase = getMinTokensNumberUseCase,
            getMaxTokensNumberUseCase = getMaxTokensNumberUseCase,
            getChangeCheckedColorUseCase = getChangeCheckedColorUseCase,
            getCheckedColorUseCase = getCheckedColorUseCase
        ) as T
    }

    companion object {
        const val TOKENS_PREFERENCES = "TokensPreferences"
    }
}