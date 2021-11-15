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
