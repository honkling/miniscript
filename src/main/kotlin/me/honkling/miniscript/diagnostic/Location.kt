package me.honkling.miniscript.diagnostic

data class Location(
    var index: Int = 0,
    var line: Int = 1,
    var column: Int = 1
) {
    fun reset() {
        index = 0
        line = 1
        column = 1
    }

    fun clone(
        index: Int = this.index,
        line: Int = this.line,
        column: Int = this.column
    ): Location {
        return Location(index, line, column)
    }
}