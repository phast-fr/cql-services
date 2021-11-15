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
