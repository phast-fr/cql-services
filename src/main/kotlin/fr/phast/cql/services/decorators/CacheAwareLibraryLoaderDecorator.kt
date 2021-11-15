/*
 * MIT License
 *
 * Copyright (c) 2021 PHAST
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package fr.phast.cql.services.decorators

import fr.phast.cql.services.loaders.TranslatorOptionAwareLibraryLoader
import fr.phast.cql.services.utils.TranslatorOptionUtil.getTranslatorOptions
import org.cqframework.cql.cql2elm.CqlTranslator
import org.cqframework.cql.cql2elm.CqlTranslatorOptions
import org.cqframework.cql.cql2elm.LibraryManager
import org.cqframework.cql.cql2elm.ModelManager
import org.cqframework.cql.elm.execution.Library
import org.cqframework.cql.elm.execution.VersionedIdentifier
import java.util.*

class CacheAwareLibraryLoaderDecorator(
    private val innerLoader: TranslatorOptionAwareLibraryLoader,
    libraryCache: MutableMap<VersionedIdentifier, Library>?
): TranslatorOptionAwareLibraryLoader {

    constructor(innerLoader: TranslatorOptionAwareLibraryLoader): this(innerLoader, null)

    private val libraryCache = libraryCache?: mutableMapOf()

    override fun load(libraryIdentifier: VersionedIdentifier): Library? {
        var library = libraryCache[libraryIdentifier]
        if (library != null && translatorOptionsMatch(library)) { // Bug on xml libraries not getting annotations
            return library
        }
        library = innerLoader.load(libraryIdentifier)
        if (library == null) {
            return null
        }
        libraryCache[libraryIdentifier] = library
        return library
    }

    private fun translatorOptionsMatch(library: Library): Boolean {
        val options: EnumSet<CqlTranslator.Options> =
            getTranslatorOptions(library) ?: return false
        return options == getCqlTranslatorOptions().options
    }

    override fun getCqlTranslatorOptions(): CqlTranslatorOptions {
        return innerLoader.getCqlTranslatorOptions()
    }

    override fun getLibraryManager(): LibraryManager {
        return innerLoader.getLibraryManager()
    }

    fun getLibraryCache(): Map<VersionedIdentifier, Library> {
        return libraryCache
    }
}
