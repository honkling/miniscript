package me.honkling.miniscript.stdlib

import me.honkling.miniscript.MiniScript
import me.honkling.miniscript.parser.ast.SymbolTable
import me.honkling.miniscript.parser.ast.prototype.Class
import me.honkling.miniscript.parser.ast.prototype.ClassInstance
import me.honkling.miniscript.parser.ast.statement.nativeBlock
import java.io.File
import java.nio.file.Paths
import kotlin.collections.toByteArray
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries

fun registerStdlibFiles(miniScript: MiniScript, symbols: SymbolTable) {
    val fileClass = symbols["File"] as Class
    fileClass.tryResolveFunction("get_absolute")!!.nativeBlock {
        val `this` = getSymbol("this") as ClassInstance
        val path = miniScript.environment.stringifyValue(`this`.fields["path"])

        if (path.startsWith("/")) { // File is already absolute
            parent.returnValue = `this`
            return@nativeBlock
        }

        // File is a relative path
        val absolutePath = Paths.get(path).toAbsolutePath().toString()
        parent.returnValue = fileClass.instantiate(absolutePath)
    }

    fileClass.tryResolveFunction("exists")!!.nativeBlock {
        val `this` = getSymbol("this") as ClassInstance
        val path = Paths.get(miniScript.environment.stringifyValue(`this`.fields["path"]))
        parent.returnValue = path.exists()
    }

    fileClass.tryResolveFunction("is_directory")!!.nativeBlock {
        val `this` = getSymbol("this") as ClassInstance
        val path = Paths.get(miniScript.environment.stringifyValue(`this`.fields["path"]))
        parent.returnValue = path.isDirectory()
    }

    fileClass.tryResolveFunction("list_files")!!.nativeBlock {
        val `this` = getSymbol("this") as ClassInstance
        val recursive = getSymbol("recursive") as Boolean
        val file = File(miniScript.environment.stringifyValue(`this`.fields["path"]))
        val files = mutableListOf<ClassInstance>()

        fun listFiles(file: File) {
            val listing = file.listFiles()

            for (file in listing) {
                if (file.isDirectory && recursive) {
                    listFiles(file)
                    continue
                }

                val instance = fileClass.instantiate(file.path)
                files += instance
            }
        }

        listFiles(file)
        parent.returnValue = miniScript.environment.arrayClass.instantiate(files)
    }

    fileClass.tryResolveFunction("write_text")!!.nativeBlock {
        val `this` = getSymbol("this") as ClassInstance
        val file = File(miniScript.environment.stringifyValue(`this`.fields["path"]))
        val content = miniScript.environment.stringifyValue(getSymbol("text") as ClassInstance)
        file.writeText(content)
    }

    fileClass.tryResolveFunction("write_bytes")!!.nativeBlock {
        val `this` = getSymbol("this") as ClassInstance
        val file = File(miniScript.environment.stringifyValue(`this`.fields["path"]))
        val content = (getSymbol("bytes") as ClassInstance).fields["data"] as List<Byte>
        file.writeBytes(content.toByteArray())
    }

    fileClass.tryResolveFunction("read_text")!!.nativeBlock {
        val `this` = getSymbol("this") as ClassInstance
        val file = File(miniScript.environment.stringifyValue(`this`.fields["path"]))
        parent.returnValue = miniScript.environment.stringClass.instantiate(file.readText())
    }

    fileClass.tryResolveFunction("read_bytes")!!.nativeBlock {
        val `this` = getSymbol("this") as ClassInstance
        val file = File(miniScript.environment.stringifyValue(`this`.fields["path"]))
        parent.returnValue = miniScript.environment.arrayClass.instantiate(file.readBytes().toMutableList())
    }
}