package presentation.tokens_screen

import androidx.lifecycle.ViewModel
import domain.usecases.tokens.ChangeTokensNumberUseCase

class SelectTokensNumberViewModel(
    private val changeTokensNumberUseCase: ChangeTokensNumberUseCase
) : ViewModel() {

    fun changeTokensNum(newNum: Int) {
        changeTokensNumberUseCase.execute(newNum)
    }

}