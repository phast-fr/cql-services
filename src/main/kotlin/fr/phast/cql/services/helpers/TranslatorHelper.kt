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

import fr.phast.cql.services.content.LibraryContentType
import org.cqframework.cql.cql2elm.CqlTranslator
import org.cqframework.cql.cql2elm.CqlTranslatorException
import org.cqframework.cql.cql2elm.LibraryManager
import org.cqframework.cql.cql2elm.ModelManager
import org.cqframework.cql.elm.execution.Library
import org.opencds.cqf.cql.engine.execution.CqlLibraryReader
import org.opencds.cqf.cql.engine.execution.JsonCqlLibraryReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import javax.xml.bind.JAXBException

object TranslatorHelper {

    fun readLibrary(stream: InputStream, contentType: LibraryContentType): Library {
        return when (contentType) {
            LibraryContentType.XML -> try {
                CqlLibraryReader.read(stream)
            }
            catch (e: IOException) {
                throw IllegalArgumentException("Error encountered while reading ELM xml: " + e.message)
            }
            catch (e: JAXBException) {
                throw IllegalArgumentException("Error encountered while reading ELM xml: " + e.message)
            }
            LibraryContentType.JXSON -> try {
                JsonCqlLibraryReader.read(InputStreamReader(stream))
            }
            catch (e: IOException) {
                throw IllegalArgumentException("Error encountered while reading ELM json: " + e.message)
            }
            else -> throw IllegalArgumentException("Error encountered: unsupported format !")
        }
    }

    fun getTranslator(cql: String, libraryManager: LibraryManager, modelManager: ModelManager?): CqlTranslator {
        return getTranslator(
            ByteArrayInputStream(cql.toByteArray(StandardCharsets.UTF_8)), libraryManager,
            modelManager
        )
    }

    fun getTranslator(
        cqlStream: InputStream, libraryManager: LibraryManager, modelManager: ModelManager?): CqlTranslator {
        val options = listOf(
            CqlTranslator.Options.EnableAnnotations,
            CqlTranslator.Options.EnableLocators,
            CqlTranslator.Options.DisableListDemotion,
            CqlTranslator.Options.DisableListPromotion,
            CqlTranslator.Options.DisableMethodInvocation
        )
        val translator = try {
            CqlTranslator.fromStream(
                cqlStream, modelManager, libraryManager,
                *options.toTypedArray()
            )
        }
        catch (e: IOException) {
            throw IllegalArgumentException(String.format("Errors occurred translating library: %s", e.message))
        }
        return translator
    }

    fun translateLibrary(translator: CqlTranslator): Library {
        return readLibrary(
            ByteArrayInputStream(
                translator.toJxson().toByteArray(StandardCharsets.UTF_8)
            ), LibraryContentType.JXSON
        )
    }

    fun errorsToString(exceptions: Iterable<CqlTranslatorException>): String {
        val errors = ArrayList<String>()
        for (error: CqlTranslatorException in exceptions) {
            val tb = error.locator
            val lines = if (tb == null) "[n/a]" else String.format(
                "%s[%d:%d, %d:%d]",
                if (tb.library != null) tb.library.id
                        + if (tb.library.version != null) "-" + tb.library.version else "" else "",
                tb.startLine, tb.startChar, tb.endLine, tb.endChar
            )
            errors.add(lines + error.message)
        }
        return errors.toString()
    }
}
