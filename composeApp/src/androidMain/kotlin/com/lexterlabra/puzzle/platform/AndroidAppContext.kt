package com.lexterlabra.puzzle.platform

import android.content.Context

/** Application context for platform services that live outside composition. */
object AndroidAppContext {
    private var context: Context? = null

    fun init(context: Context) {
        this.context = context.applicationContext
    }

    fun get(): Context = checkNotNull(context) { "AndroidAppContext.init() must run in MainActivity.onCreate" }
}
