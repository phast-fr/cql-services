package fr.phast.cql.services.converters

import org.cqframework.cql.elm.execution.VersionedIdentifier

object VersionedIdentifierConverter {

    fun toElmIdentifier(versionedIdentifier: VersionedIdentifier): org.hl7.elm.r1.VersionedIdentifier {
        return org.hl7.elm.r1.VersionedIdentifier()
            .withSystem(versionedIdentifier.system).withId(versionedIdentifier.id)
            .withVersion(versionedIdentifier.version)
    }

    fun toEngineIdentifier(versionedIdentifier: org.hl7.elm.r1.VersionedIdentifier): VersionedIdentifier {
        return VersionedIdentifier().withSystem(versionedIdentifier.system)
            .withId(versionedIdentifier.id).withVersion(versionedIdentifier.version)
    }
}
