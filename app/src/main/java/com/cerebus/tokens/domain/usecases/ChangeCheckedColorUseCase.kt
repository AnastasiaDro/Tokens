package com.cerebus.tokens.domain.usecases

import com.cerebus.tokens.domain.repository.TokensRepository

class ChangeCheckedColorUseCase(private val repository: TokensRepository) {

    fun execute(color: Int) = repository.changeCheckedColor(color)
}