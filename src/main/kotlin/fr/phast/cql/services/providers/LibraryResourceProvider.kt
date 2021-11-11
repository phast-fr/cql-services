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

import org.hl7.fhir.r4.client.rest.RestClient
import org.hl7.fhir.r4.model.Endpoint
import org.hl7.fhir.r4.model.StringType
import org.slf4j.LoggerFactory

class LibraryResourceProvider<LibraryType>(
    uri: String,
    private val cls: Class<LibraryType>): LibraryResolutionProvider<LibraryType> {

    private val fhirClient = RestClient(uri)

    constructor(uri: String, cls: Class<LibraryType>, credential: String?) : this(uri, cls) {
        this.fhirClient.tokenType = "Basic"
        this.fhirClient.credential = credential
    }

    constructor(endpoint: Endpoint, cls: Class<LibraryType>): this(endpoint.address.value, cls) {
        this.fhirClient.tokenType = "Basic"
        this.fhirClient.credential = endpoint.header?.reduce { acc, stringType ->
            StringType("${acc.value}\n${stringType.value}") }.toString()
    }

    override fun resolveLibraryById(libraryId: String): LibraryType? {
        return this.fhirClient
            .read(cls)
            .resourceType("Library")
            .resourceId(libraryId)
            .execute()
            .block()
            ?.body
    }

    override fun resolveLibraryByName(libraryName: String, libraryVersion: String): LibraryType? {
        val response = this.fhirClient
            .search()
            .withResourceType("Library")
            .withName(libraryName)
            .withVersion(libraryVersion)
            .execute()
            .block()
        return response?.body?.entry?.get(0)?.resource as LibraryType?
    }

    override fun resolveLibraryByCanonicalUrl(libraryUrl: String): LibraryType? {
        TODO("Not yet implemented")
    }

    override fun update(library: LibraryType) {
        TODO("Not yet implemented")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LibraryResourceProvider::class.java)
    }
}
