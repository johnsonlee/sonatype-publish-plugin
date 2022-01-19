package io.johnsonlee.gradle.publish

import org.eclipse.jgit.diff.HistogramDiff
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.diff.RawTextComparator
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinTask
import kotlin.reflect.KClass

sealed class License(
        val name: String,
        val keyword: String,
        val family: Boolean = false
) {

    object AFL_3_0 : License("Academic Free License v3.0", "afl-3.0")
    object APACHE_2_0 : License("Apache license 2.0", "apache-2.0")
    object ARTISTIC_2_0 : License("Artistic license 2.0", "artistic-2.0")
    object BSL_1_0 : License("Boost Software License 1.0", "bsl-1.0")
    object BSD_2_CLAUSE : License("BSD 2-clause \"Simplified\" license", "bsd-2-clause")
    object BSD_3_CLAUSE : License("BSD 3-clause \"New\" or \"Revised\" license", "bsd-3-clause")
    object BSD_3_CLAUSE_CLEAR : License("BSD 3-clause Clear license", "bsd-3-clause-clear")
    object CC : License("Creative Commons license family", "cc", true)
    object CC0_1_0 : License("Creative Commons Zero v1.0 Universal", "cc0-1.0")
    object CC_BY_4_0 : License("Creative Commons Attribution 4.0", "cc-by-4.0")
    object CC_BY_SA_4_0 : License("Creative Commons Attribution Share Alike 4.0", "cc-by-sa-4.0")
    object WTFPL : License("Do What The F*ck You Want To Public License", "wtfpl")
    object ECL_2_0 : License("Educational Community License v2.0", "ecl-2.0")
    object EPL_1_0 : License("Eclipse Public License 1.0", "epl-1.0")
    object EPL_2_0 : License("Eclipse Public License 2.0", "epl-2.0")
    object EUPL_1_1 : License("European Union Public License 1.1", "eupl-1.1")
    object AGPL_3_0 : License("GNU Affero General Public License v3.0", "agpl-3.0")
    object GPL : License("GNU General Public License family", "gpl", true)
    object GPL_1_0 : License("GNU General Public License v1.0", "gpl-1.0")
    object GPL_2_0 : License("GNU General Public License v2.0", "gpl-2.0")
    object GPL_3_0 : License("GNU General Public License v3.0", "gpl-3.0")
    object LGPL : License("GNU Lesser General Public License family", "lgpl", true)
    object LGPL_2_1 : License("GNU Lesser General Public License v2.1", "lgpl-2.1")
    object LGPL_3_0 : License("GNU Lesser General Public License v3.0", "lgpl-3.0")
    object ISC : License("ISC", "isc")
    object LPPL_1_3C : License("LaTeX Project Public License v1.3c", "lppl-1.3c")
    object MS_PL : License("Microsoft Public License", "ms-pl")
    object MIT : License("MIT", "mit")
    object MPL_2_0 : License("Mozilla Public License 2.0", "mpl-2.0")
    object OSL_3_0 : License("Open Software License 3.0", "osl-3.0")
    object POSTGRESQL : License("PostgreSQL License", "postgresql")
    object OFL_1_1 : License("SIL Open Font License 1.1", "ofl-1.1")
    object NCSA : License("University of Illinois/NCSA Open Source License", "ncsa")
    object UNLICENSE : License("The Unlicense", "unlicense")
    object ZLIB : License("zLib License", "zlib")

    val url
        get() = "https://opensource.org/licenses/${keyword}"

    companion object {

        private val FILENAMES = setOf("LICENSE", "LICENSE.txt", "LICENSE.md", "LICENSE.rst")

        private val LICENSES = License::class.sealedSubclasses.mapNotNull(KClass<out License>::objectInstance).filterNot(License::family)

        fun of(file: File): License? {
            FILENAMES.takeIf {
                file.isFile
            }?.firstOrNull {
                it.equals(file.name, true)
            } ?: return null

            val rt1 = RawText(file)
            val differ = HistogramDiff()
            val diffs = LICENSES.map { license ->
                ForkJoinPool.commonPool().submit(Callable {
                    val rt2 = License::class.java.classLoader.getResourceAsStream("licenses/${license.keyword}.txt").use {
                        it.buffered().readBytes().let(::RawText)
                    }
                    val changes = differ.diff(RawTextComparator.WS_IGNORE_ALL, rt1, rt2).changes
                    1.0f - (changes.toFloat() / rt2.rawContent.size.toFloat()).coerceAtMost(1.0f)
                })
            }.map(ForkJoinTask<Float>::get)

            return LICENSES.zip(diffs).maxBy(Pair<License, Float>::second)?.first
        }
    }

}
