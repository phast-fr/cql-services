package fr.phast.cql.services.utils

import org.cqframework.cql.cql2elm.CqlTranslator
import org.cqframework.cql.elm.execution.Library
import org.hl7.cql_annotations.r1.CqlToElmInfo
import org.w3c.dom.NamedNodeMap
import java.util.*

object TranslatorOptionUtil {

    /**
     * Gets the translator options used to generate an elm Library.
     *
     * Returns null if the translator options could not be determined.
     * (for example, the Library was translated without annotations)
     * @param library The library to extracts the options from.
     * @return The set of options used to translate the library.
     */
    fun getTranslatorOptions(library: Library): EnumSet<CqlTranslator.Options>? {
        Objects.requireNonNull(library, "library can not be null")
        if (library.annotation == null || library.annotation.isEmpty()) {
            return null
        }
        val translatorOptions = getTranslatorOptions(library.annotation)
        return parseTranslatorOptions(translatorOptions)
    }

    // TODO: This has some hackery to work around type serialization that's being tracked here:
    // https://github.com/DBCG/cql_engine/issues/436
    // Once the deserializers are fixed this should only need engine annotation types.
    private fun getTranslatorOptions(annotations: List<Any>): String? {
        for (o in annotations) {
            // Library mapped through the  Library mapper
            // The Library mapper currently uses the translator types instead of the engine
            // types because that's all there are.
            if (o is CqlToElmInfo) {
                return o.translatorOptions
            }

            // Library loaded from JSON
            if (o is LinkedHashMap<*, *>) {
                try {
                    val lhm = o as LinkedHashMap<String, String>
                    val options = lhm["translatorOptions"]
                    if (options != null) {
                        return options
                    }
                }
                catch (e: Exception) {
                    continue
                }
            }

            // Library read from XML
            // ElementNsImpl is a private class internal to the JVM
            // that we aren't allowed to use, hence all the reflection
            if (o.javaClass.simpleName == "ElementNSImpl") {
                return try {
                    val elementNsClass: Class<*> = o.javaClass
                    val method = elementNsClass.getMethod("getAttributes")
                    val nodeMap = method.invoke(o) as NamedNodeMap
                    val attributeNode = nodeMap.getNamedItem("translatorOptions") ?: continue
                    attributeNode.nodeValue
                } catch (e: Exception) {
                    continue
                }
            }
        }
        return null
    }

    /**
     * Parses a string representing CQL Translator Options into an EnumSet. The string is expected
     * to be a comma delimited list of values from the CqlTranslator.Options enumeration.
     * For example "EnableListPromotion, EnableListDemotion".
     * @param translatorOptions the string to parse
     * @return the set of options
     */
    fun parseTranslatorOptions(translatorOptions: String?): EnumSet<CqlTranslator.Options>? {
        if (translatorOptions == null) {
            return null
        }
        val optionSet = EnumSet.noneOf(CqlTranslator.Options::class.java)
        val options = translatorOptions.trim { it <= ' ' }.split(",".toRegex()).toTypedArray()
        for (option in options) {
            optionSet.add(CqlTranslator.Options.valueOf(option))
        }
        return optionSet
    }
}
