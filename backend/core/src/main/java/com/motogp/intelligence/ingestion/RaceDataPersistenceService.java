package com.motogp.intelligence.ingestion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RaceDataPersistenceService {
    private final RaceDataStore store;
    private final IdentityMappingStore identityStore;

    public RaceDataPersistenceService(RaceDataStore store, IdentityMappingStore identityStore) {
        this.store = store;
        this.identityStore = identityStore;
    }

    @Transactional
    public RaceDataIngestionResult persist(String providerCode, List<SessionResultReference> results,
                                           List<LapReference> laps, List<RacePositionReference> positions,
                                           Map<String, UUID> sessionIds, Map<String, UUID> riderIds,
                                           Map<String, UUID> teamIds, Map<String, UUID> manufacturerIds) {
        UUID providerId = identityStore.providerId(providerCode);
        Map<String, UUID> resultIds = new java.util.HashMap<>();
        int written = 0;

        for (SessionResultReference result : results) {
            UUID sessionId = sessionIds.get(result.sessionExternalId());
            UUID riderId = riderIds.get(result.riderExternalId());
            UUID teamId = result.teamExternalId() == null ? null : teamIds.get(result.teamExternalId());
            UUID manufacturerId = result.manufacturerExternalId() == null ? null : manufacturerIds.get(result.manufacturerExternalId());
            RaceDataStore.SessionResultRow existing = store.findSessionResult(sessionId, riderId).orElse(null);
            UUID resultId;
            if (existing == null) {
                resultId = store.insertSessionResult(result, sessionId, riderId, teamId, manufacturerId);
                written++;
            } else {
                requireSame(sameResult(existing, result, sessionId, riderId, teamId, manufacturerId),
                        ReferenceEntityType.SESSION_RESULT, result.externalId(), "existing classification differs");
                resultId = existing.id();
            }
            UUID sourceId = identityStore.upsertSourceRecord(providerId, ReferenceEntityType.SESSION_RESULT,
                    result.externalId(), result.sourceVersion(), java.time.Instant.now());
            store.attachPrimarySourceRecord("session_results", resultId, sourceId);
            resultIds.put(result.sessionExternalId() + ":" + result.riderExternalId(), resultId);
        }

        for (LapReference lap : laps) {
            UUID resultId = resultIds.get(lap.sessionExternalId() + ":" + lap.riderExternalId());
            RaceDataStore.LapRow existing = store.findLap(resultId, lap.lapNumber()).orElse(null);
            UUID lapId;
            if (existing == null) {
                lapId = store.insertLap(lap, resultId);
                written++;
            } else {
                requireSame(sameLap(existing, lap), ReferenceEntityType.RIDER_LAP, lap.externalId(),
                        "existing lap differs");
                lapId = existing.id();
            }
            UUID sourceId = identityStore.upsertSourceRecord(providerId, ReferenceEntityType.RIDER_LAP,
                    lap.externalId(), lap.sourceVersion(), java.time.Instant.now());
            store.attachPrimarySourceRecord("rider_laps", lapId, sourceId);
        }

        for (RacePositionReference position : positions) {
            UUID resultId = resultIds.get(position.sessionExternalId() + ":" + position.riderExternalId());
            RaceDataStore.PositionRow existing = store.findPosition(resultId, position.lapNumber()).orElse(null);
            UUID positionId;
            if (existing == null) {
                positionId = store.insertPosition(position, resultId);
                written++;
            } else {
                requireSame(samePosition(existing, position), ReferenceEntityType.RACE_POSITION,
                        position.externalId(), "existing position differs");
                positionId = existing.id();
            }
            UUID sourceId = identityStore.upsertSourceRecord(providerId, ReferenceEntityType.RACE_POSITION,
                    position.externalId(), position.sourceVersion(), java.time.Instant.now());
            store.attachPrimarySourceRecord("race_positions", positionId, sourceId);
        }
        return new RaceDataIngestionResult(providerCode,
                results.size() + laps.size() + positions.size(), written);
    }

    private boolean sameResult(RaceDataStore.SessionResultRow row, SessionResultReference data,
                                UUID sessionId, UUID riderId, UUID teamId, UUID manufacturerId) {
        return row.sessionId().equals(sessionId) && row.riderId().equals(riderId)
                && equal(row.teamId(), teamId) && equal(row.manufacturerId(), manufacturerId)
                && equal(row.gridPosition(), data.gridPosition()) && equal(row.classificationPosition(), data.classificationPosition())
                && row.classificationStatus().equals(data.classificationStatus())
                && equal(row.lapsCompleted(), data.lapsCompleted()) && equal(row.totalTimeMs(), data.totalTimeMs())
                && equal(row.points(), data.points());
    }

    private boolean sameLap(RaceDataStore.LapRow row, LapReference data) {
        return row.lapNumber() == data.lapNumber() && equal(row.lapTimeMs(), data.lapTimeMs())
                && equal(row.valid(), data.valid()) && equal(row.observedAt(), data.observedAt());
    }

    private boolean samePosition(RaceDataStore.PositionRow row, RacePositionReference data) {
        return row.lapNumber() == data.lapNumber() && equal(row.position(), data.position())
                && equal(row.observedAt(), data.observedAt());
    }

    private void requireSame(boolean same, ReferenceEntityType type, String externalId, String message) {
        if (!same) {
            throw new RaceDataConflictException(type, externalId, message);
        }
    }

    private boolean equal(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }
}
