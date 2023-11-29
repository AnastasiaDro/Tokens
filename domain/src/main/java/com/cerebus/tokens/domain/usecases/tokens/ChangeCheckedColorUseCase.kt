package com.cerebus.tokens.domain.usecases.tokens

import com.cerebus.tokens.domain.repository.TokensRepository

class ChangeCheckedColorUseCase(private val repository: com.cerebus.tokens.domain.repository.TokensRepository) {

    fun execute(color: Int) {
        if (repository.getCheckedColor() != color) repository.changeCheckedColor(color)
    }

}