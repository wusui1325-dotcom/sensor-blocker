package com.wusper.sensorblocker

object FocusedPackageParser {
    private val packageInComponent = Regex("""([A-Za-z0-9_.]+)/[A-Za-z0-9_.${'$'}]+""")

    fun parse(dumpLineOrBlock: String): String? {
        val match = packageInComponent.find(dumpLineOrBlock) ?: return null
        val pkg = match.groupValues[1]
        return pkg.takeIf { SensorCommands.isSafePackageName(it) && it != "null" }
    }
}
