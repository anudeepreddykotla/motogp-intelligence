package com.motogp.intelligence.ingestion;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class IdentityMappingService {
    private final ProviderAdapterRegistry providers;
    private final IdentityMappingStore store;
    private final Clock clock;

    @Autowired
    public IdentityMappingService(ProviderAdapterRegistry providers, IdentityMappingStore store) {
        this(providers, store, Clock.systemUTC());
    }

    IdentityMappingService(ProviderAdapterRegistry providers, IdentityMappingStore store, Clock clock) {
        this.providers = providers;
        this.store = store;
        this.clock = clock;
    }

    @Transactional
    public IdentityMappingResult map(String providerCode, ReferenceEntityType entityType,
                                     String externalId, UUID internalId) {
        ExternalReference reference = providers.require(providerCode)
                .findReference(new ReferenceQuery(entityType, externalId))
                .orElseThrow(() -> new UnknownExternalReferenceException(providerCode, entityType, externalId));
        store.requireInternalEntity(entityType, internalId);

        Instant observedAt = Instant.now(clock);
        UUID providerId = store.providerId(providerCode);
        UUID sourceRecordId = store.upsertSourceRecord(
                providerId, entityType, reference.externalId(), reference.sourceVersion(), observedAt);
        IdentityMappingStore.ExistingMapping existing = store.findMapping(providerId, entityType, externalId);
        if (existing != null) {
            if (!existing.internalId().equals(internalId)) {
                throw new IdentityMappingConflictException(
                        providerCode, entityType, externalId, existing.internalId(), internalId);
            }
            return new IdentityMappingResult(existing.mappingId(), sourceRecordId, providerCode,
                    entityType, externalId, internalId, IdentityMappingResult.MappingOutcome.UNCHANGED);
        }

        try {
            UUID mappingId = store.insertMapping(providerId, entityType, externalId, internalId, observedAt);
            return new IdentityMappingResult(mappingId, sourceRecordId, providerCode,
                    entityType, externalId, internalId, IdentityMappingResult.MappingOutcome.CREATED);
        } catch (DuplicateKeyException duplicate) {
            throw new IdentityMappingConflictException(
                    providerCode, entityType, externalId, internalId, internalId);
        }
    }
}
