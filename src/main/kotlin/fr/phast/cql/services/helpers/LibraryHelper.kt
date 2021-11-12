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

package fr.phast.cql.services.helpers

import fr.phast.cql.services.loaders.LibraryLoader
import fr.phast.cql.services.providers.LibraryResolutionProvider
import fr.phast.cql.services.providers.LibrarySourceProvider
import org.cqframework.cql.cql2elm.LibraryManager
import org.cqframework.cql.cql2elm.ModelManager
import org.cqframework.cql.elm.execution.VersionedIdentifier
import org.hl7.fhir.r4.model.Library
import org.hl7.fhir.r4.model.PlanDefinition

object LibraryHelper {

    fun createLibraryLoader(provider: LibraryResolutionProvider<Library>): LibraryLoader {
        val libraryManager = LibraryManager(ModelManager())
        libraryManager.librarySourceLoader.clearProviders()
        libraryManager.librarySourceLoader.registerProvider(
            LibrarySourceProvider(provider,
                { x -> x.content?.asIterable() }, { x -> x.contentType?.value }, { x -> x.data?.toByteArray() })
        )
        return LibraryLoader(libraryManager)
    }

    fun resolveLibraryById(
        libraryId: String, libraryLoader: org.opencds.cqf.cql.engine.execution.LibraryLoader,
        libraryResourceProvider: LibraryResolutionProvider<Library>
    ): org.cqframework.cql.elm.execution.Library? {
        val fhirLibrary = libraryResourceProvider.resolveLibraryById(libraryId)
        return libraryLoader
            .load(VersionedIdentifier().withId(fhirLibrary!!.name?.value).withVersion(fhirLibrary.version?.value))
    }

    fun resolvePrimaryLibrary(
        planDefinition: PlanDefinition,
        libraryLoader: org.opencds.cqf.cql.engine.execution.LibraryLoader,
        libraryResourceProvider: LibraryResolutionProvider<Library>
    ): org.cqframework.cql.elm.execution.Library {
        val id = CanonicalHelper.getId(planDefinition.library!![0])
        return resolveLibraryById(id, libraryLoader, libraryResourceProvider)
            ?: throw IllegalArgumentException(
                String.format(
                    "Could not resolve primary library for PlanDefinition/%s",
                    planDefinition.id?.value
                )
            )
    }
}
