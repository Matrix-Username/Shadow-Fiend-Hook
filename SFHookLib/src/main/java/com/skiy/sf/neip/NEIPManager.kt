package com.skiy.sf.neip

import android.content.Context
import android.os.Build
import java.io.File
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object NEIPManager {

    @OptIn(ExperimentalEncodingApi::class)
    fun reconstructLibs(context: Context) {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: return
        val abiFieldName = abi.replace("-", "_").uppercase() + "_LIBS"

        val saveDir = File("${context.filesDir.path}/neip/")

        if (!saveDir.exists()) saveDir.mkdirs()

        val neipSupportFile = File(saveDir, "libaliuhook.so")

        if (neipSupportFile.exists()) {
            return
        }

        try {
            val encodedLibsClass = Class.forName("com.skiy.sf.data.EncodedLibs")
            val field = encodedLibsClass.getDeclaredField(abiFieldName)
            field.isAccessible = true

            val libsMap = field.get(null) as? Map<String, String> ?: return

            libsMap.forEach { (libFileName, base64Lib) ->
                val libBytes = Base64.decode(base64Lib.toByteArray())

                val file = File(saveDir, libFileName)
                file.writeBytes(libBytes)
            }
        } catch (e: ClassNotFoundException) {
            println("Сгенерированный класс EncodedLibs не найден")
        } catch (e: NoSuchFieldException) {
            println("Архитектура $abi не поддерживается")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getContextReflectively(): Context? {
        return try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val currentApplicationMethod = activityThreadClass.getMethod("currentApplication")
            val context = currentApplicationMethod.invoke(null) as? Context
            context
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}