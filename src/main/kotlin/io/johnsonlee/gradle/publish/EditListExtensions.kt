package io.johnsonlee.gradle.publish

import org.eclipse.jgit.diff.EditList
import kotlin.math.abs

val EditList.changes: Int
    get() = sumBy {
        abs(it.endA - it.beginA) + abs(it.endB - it.beginB)
    }