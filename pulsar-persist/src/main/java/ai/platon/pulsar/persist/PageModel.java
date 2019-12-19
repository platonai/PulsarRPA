package ai.platon.pulsar.persist;

import ai.platon.pulsar.persist.gora.generated.GFieldGroup;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Created by vincent on 17-8-3.
 * Copyright @ 2013-2017 Platon AI. All rights reserved
 * <p>
 * The core concept of Document Data Model, DDM
 */
public class PageModel {
    private List<GFieldGroup> fieldGroups;

    private PageModel(List<GFieldGroup> fieldGroups) {
        this.fieldGroups = fieldGroups;
    }

    public static PageModel box(List<GFieldGroup> fieldGroups) {
        return new PageModel(fieldGroups);
    }

    public List<GFieldGroup> unbox() {
        return fieldGroups;
    }

    @Nullable
    public FieldGroup first() {
        return isEmpty() ? null : get(0);
    }

    public FieldGroup get(int i) {
        return FieldGroup.box(fieldGroups.get(i));
    }

    public boolean add(FieldGroup fieldGroup) {
        return fieldGroups.add(fieldGroup.unbox());
    }

    public void add(int i, FieldGroup fieldGroup) {
        fieldGroups.add(i, fieldGroup.unbox());
    }

    @NotNull
    public FieldGroup emplace(long id, long parentId, String group, Map<CharSequence, CharSequence> fields) {
        FieldGroup fieldGroup = FieldGroup.newFieldGroup(id, group, parentId);
        fieldGroup.setFields(fields);
        add(fieldGroup);
        return fieldGroup;
    }

    public boolean isEmpty() {
        return fieldGroups.isEmpty();
    }

    public int size() {
        return fieldGroups.size();
    }

    public void clear() {
        fieldGroups.clear();
    }

    public FieldGroup findById(long id) {
        GFieldGroup gFieldGroup = CollectionUtils.find(fieldGroups, fg -> fg.getId() == id);
        return gFieldGroup == null ? null : FieldGroup.box(gFieldGroup);
    }
}
