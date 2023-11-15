package com.cerebus.tokens.domain.models

data class TokensData(
    var number: Int,
    var unCheckedColor: Int,
    var checkedColor: Int,
    var minTokensNumber: Int = 1,
    var maxTokensNumber: Int = 10,
    var checkedTokensNumber: Int = 0
) {

}
