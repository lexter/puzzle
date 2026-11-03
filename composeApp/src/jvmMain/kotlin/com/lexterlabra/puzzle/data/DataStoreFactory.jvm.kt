package com.lexterlabra.puzzle.data

import java.io.File

actual fun dataStorePath(fileName: String): String =
    File(System.getProperty("java.io.tmpdir"), "slide-puzzle/$fileName").also { it.parentFile.mkdirs() }.absolutePath
