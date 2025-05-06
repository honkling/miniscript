package me.honkling.miniscript.diagnostic

class Logger(internal val location: Location) {
    val diagnostics = mutableListOf<Diagnostic>()

    fun log(type: DiagnosticType, message: String, location: Location = this.location.clone()) {
        diagnostics += Diagnostic(type, message, location)
    }

    fun info(message: String, location: Location = this.location.clone()) = log(DiagnosticType.Info, message)
    fun warning(message: String, location: Location = this.location.clone()) = log(DiagnosticType.Warning, message)
    fun error(message: String, location: Location = this.location.clone()) = log(DiagnosticType.Error, message)
}