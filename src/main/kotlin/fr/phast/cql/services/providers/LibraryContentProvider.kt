package fr.phast.cql.services.providers

import fr.phast.cql.services.content.LibraryContentType
import org.hl7.elm.r1.VersionedIdentifier
import java.io.InputStream

interface LibraryContentProvider: org.cqframework.cql.cql2elm.LibrarySourceProvider {
    fun getLibraryContent(versionedIdentifier: VersionedIdentifier, libraryContentType: LibraryContentType): InputStream?

    override fun getLibrarySource(libraryIdentifier: VersionedIdentifier): InputStream? {
        return getLibraryContent(libraryIdentifier, LibraryContentType.CQL)
    }
}
