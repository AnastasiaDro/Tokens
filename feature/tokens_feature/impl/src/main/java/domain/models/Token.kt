package domain.models

data class Token(
    val isChecked: Boolean,
    val checkedColor: Int,
    val id: String = java.util.UUID.randomUUID().toString(),
)
