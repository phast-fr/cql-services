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
