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

package fr.phast.cql.services

import fr.phast.cql.services.decorators.CacheAwareLibraryLoaderDecorator
import fr.phast.cql.services.loaders.TranslatingLibraryLoader
import fr.phast.cql.services.managers.CacheAwareModelManager
import fr.phast.cql.services.providers.LibraryContentProvider
import fr.phast.cql.services.providers.LibraryContentProviderImpl
import fr.phast.cql.services.providers.LibraryResolutionProvider
import org.cqframework.cql.cql2elm.CqlTranslatorOptions
import org.cqframework.cql.cql2elm.LibraryManager
import org.cqframework.cql.cql2elm.ModelManager
import org.cqframework.cql.cql2elm.model.Model
import org.cqframework.cql.elm.execution.Library
import org.hl7.elm.r1.VersionedIdentifier
import org.opencds.cqf.cql.engine.execution.LibraryLoader

class LibraryService(
    private val modelCache: MutableMap<VersionedIdentifier, Model>,
    private val libraryCache: MutableMap<org.cqframework.cql.elm.execution.VersionedIdentifier, Library>,
    private val translatorOptions: CqlTranslatorOptions
) {

    fun getTranslatorOptions(): CqlTranslatorOptions {
        return translatorOptions
    }

    fun getLibraryCache(): Map<org.cqframework.cql.elm.execution.VersionedIdentifier, Library> {
        return libraryCache
    }

    fun createLibraryLoader(
        provider: LibraryResolutionProvider<org.hl7.fhir.r4.model.Library>
    ): LibraryLoader {
        val contentProvider = getLibraryContentProvider(provider)
        val libraryManager = getLibraryManager(getModelManager(), contentProvider)
        val translatingLibraryLoader = TranslatingLibraryLoader(
            libraryManager, listOf(contentProvider), translatorOptions
        )
        return CacheAwareLibraryLoaderDecorator(translatingLibraryLoader, libraryCache)
    }

    private fun getModelManager(): ModelManager {
        return CacheAwareModelManager(modelCache)
    }

    private fun getLibraryContentProvider(provider: LibraryResolutionProvider<org.hl7.fhir.r4.model.Library>):
            LibraryContentProvider {
        return LibraryContentProviderImpl(provider,
            { x -> x.content },
            { x -> x.contentType?.value },
            { x -> x.data?.toByteArray() }
        )
    }

    private fun getLibraryManager(modelManager: ModelManager,
                                  libraryContentProvider: LibraryContentProvider): LibraryManager {
        val libraryManager = LibraryManager(modelManager)
        libraryManager.librarySourceLoader.registerProvider(libraryContentProvider)
        return libraryManager
    }
}
