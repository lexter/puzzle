package com.lexterlabra.puzzle.data

import com.lexterlabra.puzzle.platform.AndroidAppContext

actual fun dataStorePath(fileName: String): String =
    AndroidAppContext.get().filesDir.resolve(fileName).absolutePath
