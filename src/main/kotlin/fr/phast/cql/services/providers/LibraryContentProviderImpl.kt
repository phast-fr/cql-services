package fr.phast.cql.services.providers

import fr.phast.cql.services.content.LibraryContentType
import org.cqframework.cql.cql2elm.FhirLibrarySourceProvider
import org.hl7.elm.r1.VersionedIdentifier
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.InputStream

class LibraryContentProviderImpl<LibraryType, AttachmentType>(
    private val provider: LibraryResolutionProvider<LibraryType>,
    private val getAttachments: (library: LibraryType) -> Iterable<AttachmentType>?,
    private val getContentType: (attachment: AttachmentType) -> String?,
    private val getContent: (attachment: AttachmentType) -> ByteArray?
): LibraryContentProvider {

    private val innerProvider = FhirLibrarySourceProvider()

    override fun getLibraryContent(
        versionedIdentifier: VersionedIdentifier,
        libraryContentType: LibraryContentType
    ): InputStream? {
        // TODO: Support loading ELM
        if (libraryContentType !== LibraryContentType.CQL) {
            return null
        }

        try {
            val library = this.provider.resolveLibraryByName(
                versionedIdentifier.id,
                versionedIdentifier.version
            )

            library?.let<LibraryType, Iterable<AttachmentType>?> {
                this.getAttachments(it)
            }?.forEach { attachment ->
                if (this.getContentType(attachment) == "text/cql") {
                    return ByteArrayInputStream(this.getContent(attachment))
                }
            }
        }
        catch (e: Exception) {
            logger.warn(
                "Failed to parse Library source for VersionedIdentifier '$versionedIdentifier'!"
                        + System.lineSeparator() + e.message, e
            )
        }

        return innerProvider.getLibrarySource(versionedIdentifier)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LibraryContentProviderImpl::class.java)
    }
}
