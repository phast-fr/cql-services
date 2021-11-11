package fr.phast.cql.services.helpers

import org.hl7.fhir.r4.model.CanonicalType

object CanonicalHelper {

    fun getId(canonical: CanonicalType): String {
        val id = canonical.value
        val temp = if (id.contains("/")) id.substring(id.lastIndexOf("/") + 1) else id
        return temp.split("\\|".toRegex()).toTypedArray()[0]
    }

    fun getResourceName(canonical: CanonicalType): String? {
        var id = canonical.value
        if (id.contains("/")) {
            id = id.replace(id.substring(id.lastIndexOf("/")), "")
            return if (id.contains("/")) id.substring(id.lastIndexOf("/") + 1) else id
        }
        return null
    }
}
