package fr.phast.cql.services.managers

import org.cqframework.cql.cql2elm.ModelManager
import org.cqframework.cql.cql2elm.model.Model
import org.cqframework.cql.cql2elm.model.SystemModel
import org.hl7.elm.r1.VersionedIdentifier

class CacheAwareModelManager(private val globalCache: MutableMap<VersionedIdentifier, Model>): ModelManager() {

    private val localCache = mutableMapOf<String, Model>()

    override fun resolveModel(modelIdentifier: VersionedIdentifier): Model {
        var model: Model? = null
        if (localCache.containsKey(modelIdentifier.id)) {
            model = localCache[modelIdentifier.id]
            require(!(modelIdentifier.version != null && modelIdentifier.version != model!!.modelInfo.version)) {
                String.format(
                    "Could not load model information for model %s, version %s because version %s is already loaded.",
                    modelIdentifier.id,
                    modelIdentifier.version,
                    model!!.modelInfo.version
                )
            }
        }
        if (model == null && globalCache.containsKey(modelIdentifier)) {
            model = globalCache[modelIdentifier]
            localCache[modelIdentifier.id] = model!!
        }
        if (model == null) {
            model = buildModel(modelIdentifier)
            globalCache[modelIdentifier] = model
            localCache[modelIdentifier.id] = model
        }
        return model
    }

    private fun buildModel(identifier: VersionedIdentifier): Model {
        val model: Any?
        return try {
            val modelInfo = modelInfoLoader.getModelInfo(identifier)
            model = if (identifier.id == "System") {
                SystemModel(modelInfo)
            }
            else {
                Model(modelInfo, this)
            }
            model
        }
        catch (e: ClassNotFoundException) {
            throw IllegalArgumentException(
                String.format(
                    "Could not load model information for model %s, version %s.",
                    identifier.id,
                    identifier.version
                )
            )
        }
    }
}
