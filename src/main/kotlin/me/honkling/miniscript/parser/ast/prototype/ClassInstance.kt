package me.honkling.miniscript.parser.ast.prototype

class ClassInstance(var classRef: Class) {
    class SuperInstance(val instance: ClassInstance)
    
    val fields = mutableMapOf<Any?, Any?>()
}