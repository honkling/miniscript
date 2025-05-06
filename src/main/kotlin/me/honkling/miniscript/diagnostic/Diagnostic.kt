package me.honkling.miniscript.diagnostic

enum class DiagnosticType {
    Info,
    Warning,
    Error
}

data class Diagnostic(
    val type: DiagnosticType,
    val message: String,
    val location: Location
)