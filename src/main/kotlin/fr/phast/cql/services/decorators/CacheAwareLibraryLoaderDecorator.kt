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
