package com.luxalpa.structuredhighlights

import com.intellij.openapi.diagnostic.Logger

val LOGGER: Logger = Logger.getInstance("StructuredHighlights")

fun debug(message: () -> String) {
    if (!LOGGER.isDebugEnabled) return
    LOGGER.debug(message())
}
