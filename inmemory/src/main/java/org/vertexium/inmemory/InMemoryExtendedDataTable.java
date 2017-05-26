package org.vertexium.inmemory;

import com.google.common.collect.ImmutableSet;
import org.vertexium.*;

public abstract class InMemoryExtendedDataTable {
    public abstract ImmutableSet<String> getTableNames(
            ElementType elementType,
            String elementId,
            Authorizations authorizations
    );

    public abstract Iterable<? extends ExtendedDataRow> getTable(
            ElementType elementType,
            String elementId,
            String tableName,
            Authorizations authorizations
    );

    public abstract void addData(
            ExtendedDataRowId rowId,
            String column,
            Object value,
            long timestamp,
            Visibility visibility
    );

    public abstract void remove(ExtendedDataRowId id);
}