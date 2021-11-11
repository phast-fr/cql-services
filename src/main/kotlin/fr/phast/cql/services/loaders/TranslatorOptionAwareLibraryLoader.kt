package fr.phast.cql.services.loaders

import org.cqframework.cql.cql2elm.CqlTranslatorOptions
import org.opencds.cqf.cql.engine.execution.LibraryLoader

interface TranslatorOptionAwareLibraryLoader: LibraryLoader {
    fun getCqlTranslatorOptions(): CqlTranslatorOptions
}
