/*
 * Waltz - Enterprise Architecture
 * Copyright (C) 2016, 2017, 2018, 2019 Waltz open source project
 * See README.md for more information
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific
 *
 */

package org.finos.waltz.data.measurable_rating_planned_decommission;


import org.finos.waltz.common.DateTimeUtilities;
import org.finos.waltz.common.exception.ModifyingReadOnlyRecordException;
import org.finos.waltz.data.InlineSelectFieldFactory;
import org.finos.waltz.model.EntityKind;
import org.finos.waltz.model.EntityReference;
import org.finos.waltz.model.Operation;
import org.finos.waltz.model.command.DateFieldChange;
import org.finos.waltz.model.measurable_rating_planned_decommission.ImmutableMeasurableRatingPlannedDecommission;
import org.finos.waltz.model.measurable_rating_planned_decommission.MeasurableRatingPlannedDecommission;
import org.finos.waltz.schema.tables.records.MeasurableRatingPlannedDecommissionRecord;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.lambda.tuple.Tuple2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Set;

import static org.finos.waltz.common.Checks.checkNotNull;
import static org.finos.waltz.common.DateTimeUtilities.toLocalDateTime;
import static org.finos.waltz.common.DateTimeUtilities.toSqlDate;
import static org.finos.waltz.common.SetUtilities.asSet;
import static org.finos.waltz.common.SetUtilities.union;
import static org.finos.waltz.common.StringUtilities.notEmpty;
import static org.finos.waltz.model.EntityReference.mkRef;
import static org.finos.waltz.schema.Tables.MEASURABLE_CATEGORY;
import static org.finos.waltz.schema.Tables.USER_ROLE;
import static org.finos.waltz.schema.tables.MeasurableRating.MEASURABLE_RATING;
import static org.finos.waltz.schema.tables.MeasurableRatingPlannedDecommission.MEASURABLE_RATING_PLANNED_DECOMMISSION;
import static org.finos.waltz.schema.tables.MeasurableRatingReplacement.MEASURABLE_RATING_REPLACEMENT;
import static org.jooq.lambda.tuple.Tuple.tuple;

@Repository
public class MeasurableRatingPlannedDecommissionDao {

    private static final Field<String> NAME_FIELD = InlineSelectFieldFactory.mkNameField(
            MEASURABLE_RATING_PLANNED_DECOMMISSION.ENTITY_ID,
            MEASURABLE_RATING_PLANNED_DECOMMISSION.ENTITY_KIND,
            asSet(EntityKind.APPLICATION));

    public static final RecordMapper<? super Record, MeasurableRatingPlannedDecommission> TO_DOMAIN_MAPPER = record -> {

        MeasurableRatingPlannedDecommissionRecord r = record.into(MEASURABLE_RATING_PLANNED_DECOMMISSION);

        return ImmutableMeasurableRatingPlannedDecommission.builder()
                .id(r.getId())
                .entityReference(mkRef(
                        EntityKind.valueOf(r.getEntityKind()),
                        r.getEntityId(),
                        record.get(NAME_FIELD)))
                .measurableId(r.getMeasurableId())
                .plannedDecommissionDate(r.getPlannedDecommissionDate())
                .createdAt(toLocalDateTime(r.getCreatedAt()))
                .createdBy(r.getCreatedBy())
                .lastUpdatedAt(toLocalDateTime(r.getUpdatedAt()))
                .lastUpdatedBy(r.getUpdatedBy())
                .build();
    };


    private final DSLContext dsl;



    @Autowired
    MeasurableRatingPlannedDecommissionDao(DSLContext dsl){
        checkNotNull(dsl, "dsl must not be null");
        this.dsl = dsl;
    }


    public static void checkIfReadOnly(DSLContext myDsl, long id) {
        Boolean readOnly = myDsl
                .select(MEASURABLE_RATING.IS_READONLY)
                .from(MEASURABLE_RATING)
                .innerJoin(MEASURABLE_RATING_PLANNED_DECOMMISSION)
                .on(MEASURABLE_RATING_PLANNED_DECOMMISSION.ENTITY_ID.eq(MEASURABLE_RATING.ENTITY_ID))
                .and(MEASURABLE_RATING_PLANNED_DECOMMISSION.ENTITY_KIND.eq(MEASURABLE_RATING.ENTITY_KIND))
                .and(MEASURABLE_RATING_PLANNED_DECOMMISSION.MEASURABLE_ID.eq(MEASURABLE_RATING.MEASURABLE_ID))
                .where(MEASURABLE_RATING_PLANNED_DECOMMISSION.ID.eq(id))
                .fetchOne(MEASURABLE_RATING.IS_READONLY);
        if (readOnly) {
            throw new ModifyingReadOnlyRecordException("MRPD_READ_ONLY", "Cannot update the measurable rating planned decommission as it is read only");
        }
    }


    public MeasurableRatingPlannedDecommission getById(Long id){
        return dsl
                .select(MEASURABLE_RATING_PLANNED_DECOMMISSION.fields())
                .select(NAME_FIELD)
                .from(MEASURABLE_RATING_PLANNED_DECOMMISSION)
                .where(MEASURABLE_RATING_PLANNED_DECOMMISSION.ID.eq(id))
                .fetchOne(TO_DOMAIN_MAPPER);
    }


    public MeasurableRatingPlannedDecommission getByEntityAndMeasurable(EntityReference entityReference,
                                                                        long measurableId) {
        return dsl
                .select(MEASURABLE_RATING_PLANNED_DECOMMISSION.fields())
                .select(NAME_FIELD)
                .from(MEASURABLE_RATING_PLANNED_DECOMMISSION)
                .where(mkRefCondition(entityReference)
                        .and(MEASURABLE_RATING_PLANNED_DECOMMISSION.MEASURABLE_ID.eq(measurableId)))
                .fetchOne(TO_DOMAIN_MAPPER);
    }


    public Set<MeasurableRatingPlannedDecommission> findByEntityRef(EntityReference ref){
        return dsl
                .select(MEASURABLE_RATING_PLANNED_DECOMMISSION.fields())
                .select(NAME_FIELD)
                .from(MEASURABLE_RATING_PLANNED_DECOMMISSION)
                .where(mkRefCondition(ref))
                .fetchSet(TO_DOMAIN_MAPPER);
    }


    public Collection<MeasurableRatingPlannedDecommission> findByReplacingEntityRef(EntityReference ref) {
        return dsl
                .select(MEASURABLE_RATING_PLANNED_DECOMMISSION.fields())
                .select(NAME_FIELD)
                .from(MEASURABLE_RATING_PLANNED_DECOMMISSION)
                .innerJoin(MEASURABLE_RATING_REPLACEMENT)
                    .on(MEASURABLE_RATING_REPLACEMENT.DECOMMISSION_ID.eq(MEASURABLE_RATING_PLANNED_DECOMMISSION.ID))
                .where(MEASURABLE_RATING_REPLACEMENT.ENTITY_ID.eq(ref.id()))
                .and(MEASURABLE_RATING_REPLACEMENT.ENTITY_KIND.eq(ref.kind().name()))
                .fetchSet(TO_DOMAIN_MAPPER);
    }


    public Tuple2<Operation, Boolean> save(EntityReference entityReference,
                                           long measurableId,
                                           DateFieldChange dateChange,
                                           String userName) {
        checkIfReadOnly(entityReference, measurableId);

        Record existingRecord = dsl
                .select(MEASURABLE_RATING_PLANNED_DECOMMISSION.fields())
                .from(MEASURABLE_RATING_PLANNED_DECOMMISSION)
                .where(mkRefCondition(entityReference)
                        .and(MEASURABLE_RATING_PLANNED_DECOMMISSION.MEASURABLE_ID.eq(measurableId)))
                .fetchOne();

        if (existingRecord != null) {
            MeasurableRatingPlannedDecommissionRecord existingDecommRecord = existingRecord.into(MEASURABLE_RATING_PLANNED_DECOMMISSION);
            updateDecommDateOnRecord(existingDecommRecord, dateChange, userName);
            boolean updatedRecord = existingDecommRecord.update() == 1;
            return tuple(Operation.UPDATE, updatedRecord);
        } else {
            MeasurableRatingPlannedDecommissionRecord record = dsl.newRecord(MEASURABLE_RATING_PLANNED_DECOMMISSION);
            updateDecommDateOnRecord(record, dateChange, userName);
            record.setCreatedAt(DateTimeUtilities.nowUtcTimestamp());
            record.setCreatedBy(userName);
            record.setEntityId(entityReference.id());
            record.setEntityKind(entityReference.kind().name());
            record.setMeasurableId(measurableId);
            boolean recordsInserted = record.insert() == 1;
            return tuple(Operation.ADD, recordsInserted);
        }
    }


    public boolean remove(Long id){
        checkIfReadOnly(dsl, id);
        return dsl
                .deleteFrom(MEASURABLE_RATING_PLANNED_DECOMMISSION)
                .where(MEASURABLE_RATING_PLANNED_DECOMMISSION.ID.eq(id))
                .execute() == 1;
    }


    // -- HELPERS ----

    private Condition mkRefCondition(EntityReference ref) {
        return MEASURABLE_RATING_PLANNED_DECOMMISSION.ENTITY_ID.eq(ref.id())
                .and(MEASURABLE_RATING_PLANNED_DECOMMISSION.ENTITY_KIND.eq(ref.kind().name()));
    }


    private void checkIfReadOnly(EntityReference entityReference, long measurableId) {
        Boolean readOnly = dsl
                .select(MEASURABLE_RATING.IS_READONLY)
                .from(MEASURABLE_RATING)
                .where(MEASURABLE_RATING.ENTITY_KIND.eq(entityReference.kind().name()))
                .and(MEASURABLE_RATING.ENTITY_ID.eq(entityReference.id()))
                .and(MEASURABLE_RATING.MEASURABLE_ID.eq(measurableId))
                .fetchOne(MEASURABLE_RATING.IS_READONLY);

        if (readOnly) {
            throw new ModifyingReadOnlyRecordException("MRPD_READ_ONLY", "Cannot update measurable rating planned decomm date as it is read only");
        }
    }


    private void updateDecommDateOnRecord(MeasurableRatingPlannedDecommissionRecord record,
                                          DateFieldChange dateChange,
                                          String userName) {
        record.setUpdatedAt(DateTimeUtilities.nowUtcTimestamp());
        record.setUpdatedBy(userName);
        record.setPlannedDecommissionDate(toSqlDate(dateChange.newVal()));
    }

    public Set<Operation> calculateAmendedDecommOperations(Set<Operation> operationsForEntityAssessment,
                                                           long measurableCategoryId,
                                                           String username) {

        Boolean hasOverrideRole = dsl
                .select(USER_ROLE.ROLE)
                .from(MEASURABLE_CATEGORY)
                .leftJoin(USER_ROLE)
                .on(USER_ROLE.ROLE.eq(MEASURABLE_CATEGORY.RATING_EDITOR_ROLE)
                        .and(USER_ROLE.USER_NAME.eq(username)))
                .where(MEASURABLE_CATEGORY.ID.eq(measurableCategoryId))
                .fetchOne(r -> notEmpty(r.get(USER_ROLE.ROLE)));

        if (hasOverrideRole) {
            return union(operationsForEntityAssessment, asSet(Operation.ADD, Operation.UPDATE, Operation.REMOVE));
        } else {
            return operationsForEntityAssessment;
        }
    }
}
