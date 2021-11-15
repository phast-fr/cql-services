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

package fr.phast.cql.services.decorators

import org.opencds.cqf.cql.engine.runtime.Code
import org.opencds.cqf.cql.engine.terminology.CodeSystemInfo
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider
import org.opencds.cqf.cql.engine.terminology.ValueSetInfo

class CacheAwareTerminologyDecorator(
    private val innerTerminologyProvider: TerminologyProvider,
    private val globalCodeCache: MutableMap<String, Iterable<Code>>): TerminologyProvider {

    override fun `in`(code: Code?, valueSet: ValueSetInfo): Boolean {
        val codes = expand(valueSet) ?: return false
        codes.forEach { c ->
            if (c.equivalent(code)) {
                return true
            }
        }
        return false
    }

    override fun expand(valueSet: ValueSetInfo): Iterable<Code>? {
        if (this.globalCodeCache.containsKey(valueSet.id)) {
            return this.globalCodeCache[valueSet.id]
        }
        val codes: Iterable<Code> = this.innerTerminologyProvider.expand(valueSet)
        this.globalCodeCache[valueSet.id] = codes
        return codes
    }

    override fun lookup(code: Code?, codeSystem: CodeSystemInfo?): Code? {
        return this.innerTerminologyProvider.lookup(code, codeSystem)
    }
}
