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

package fr.phast.cql.services.loaders

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule
import fr.phast.cql.services.content.LibraryContentType
import fr.phast.cql.services.converters.VersionedIdentifierConverter.toElmIdentifier
import fr.phast.cql.services.providers.LibraryContentProvider
import fr.phast.cql.services.utils.TranslatorOptionUtil.getTranslatorOptions
import org.cqframework.cql.cql2elm.*
import org.cqframework.cql.cql2elm.model.serialization.LibraryWrapper
import org.cqframework.cql.elm.execution.Library
import org.cqframework.cql.elm.execution.VersionedIdentifier
import org.opencds.cqf.cql.engine.exception.CqlException
import org.opencds.cqf.cql.engine.execution.CqlLibraryReader
import org.opencds.cqf.cql.engine.execution.JsonCqlLibraryReader
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*
import javax.xml.bind.JAXBException

class TranslatingLibraryLoader(
    private val libraryManager: LibraryManager,
    private val libraryContentProviders: List<LibraryContentProvider>,
    translatorOptions: CqlTranslatorOptions?
): TranslatorOptionAwareLibraryLoader {

    private val cqlTranslatorOptions = translatorOptions ?: CqlTranslatorOptions.defaultOptions()

    override fun getCqlTranslatorOptions(): CqlTranslatorOptions {
        return cqlTranslatorOptions
    }

    override fun getLibraryManager(): LibraryManager {
        return libraryManager
    }

    override fun load(libraryIdentifier: VersionedIdentifier): Library {
        val library = this.getLibraryFromElm(libraryIdentifier)

        if (library != null && this.translatorOptionsMatch(library)) {
            return library
        }

        return this.translate(libraryIdentifier)?: throw Exception()
    }

    private fun getLibraryFromElm(libraryIdentifier: VersionedIdentifier): Library? {
        val versionedIdentifier: org.hl7.elm.r1.VersionedIdentifier = toElmIdentifier(libraryIdentifier)
        var content = getLibraryContent(versionedIdentifier, LibraryContentType.JXSON)
        if (content != null) {
            try {
                return this.readJxson(content)
            }
            catch (e: Exception) {
                // Intentionally empty. Fall through to xml
            }
        }
        content = getLibraryContent(versionedIdentifier, LibraryContentType.XML)
        if (content != null) {
            try {
                return this.readXml(content)
            }
            catch (e: Exception) {
                // Intentionally empty. Fall through to null
            }
        }
        return null
    }

    private fun translatorOptionsMatch(library: Library): Boolean {
        val options: EnumSet<CqlTranslator.Options> =
            getTranslatorOptions(library) ?: return false
        return options == cqlTranslatorOptions.options
    }

    private fun getLibraryContent(
        libraryIdentifier: org.hl7.elm.r1.VersionedIdentifier,
        libraryContentType: LibraryContentType
    ): InputStream? {
        libraryContentProviders.forEach { provider ->
            val content = provider.getLibraryContent(libraryIdentifier, libraryContentType)
            if (content != null) {
                return content
            }
        }
        return null
    }

    private fun translate(libraryIdentifier: VersionedIdentifier): Library? {
        val errors = mutableListOf<CqlTranslatorException>()
        val library = try {
            libraryManager.resolveLibrary(
                toElmIdentifier(libraryIdentifier),
                cqlTranslatorOptions, errors
            )
        }
        catch (e: Exception) {
            throw CqlException(String.format("Unable translate library %s", libraryIdentifier.id, e))
        }
        if (errors.isNotEmpty()) {
            errors.forEach { e ->
                if (e.severity == CqlTranslatorException.ErrorSeverity.Error) {
                    throw CqlException(
                        String.format(
                            "Translation of library %s failed with the following message: %s",
                            libraryIdentifier.id,
                            e.message
                        )
                    )
                }
            }
        }
        if (library != null) {
            return try {
                // note cannot use jxson version
                // best way is use a mapper between ELM to Engine models
                this.readXml(CqlTranslator.convertToXml(library.library))
            }
            catch (e: Exception) {
                throw CqlException(String.format("Mapping of library %s failed", libraryIdentifier.id), e)
            }
        }
        return null
    }

    @Synchronized
    @Throws(IOException::class, JAXBException::class)
    private fun readJxson(json: String): Library {
        return this.readJxson(ByteArrayInputStream(json.toByteArray(StandardCharsets.UTF_8)))
    }

    @Synchronized
    @Throws(IOException::class)
    private fun readJxson(inputStream: InputStream): Library {
        return JsonCqlLibraryReader.read(InputStreamReader(inputStream))
    }

    @Synchronized
    @Throws(IOException::class, JAXBException::class)
    private fun readXml(inputStream: InputStream): Library {
        return CqlLibraryReader.read(inputStream)
    }

    @Synchronized
    @Throws(IOException::class, JAXBException::class)
    private fun readXml(xml: String): Library {
        return this.readXml(ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8)))
    }

    @Synchronized
    private fun getJxsonMapper(): ObjectMapper {
        if (objectMapper == null) {
            val mapper = ObjectMapper()
            mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_DEFAULT)
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true)
            val annotationModule = JaxbAnnotationModule()
            mapper.registerModule(annotationModule)
            objectMapper = mapper
        }
        return objectMapper!!
    }

    @Throws(JsonProcessingException::class)
    fun convertToJxson(library: org.hl7.elm.r1.Library): String {
        val wrapper = LibraryWrapper()
        wrapper.library = library
        return getJxsonMapper().writeValueAsString(wrapper)
    }

    companion object {
        private var objectMapper: ObjectMapper? = null
    }
}
