package org.vertexium.elasticsearch7.bulk;

import com.google.common.collect.ImmutableMap;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.vertexium.ElementLocation;
import org.vertexium.VertexiumException;
import org.vertexium.VertexiumObjectId;
import org.vertexium.elasticsearch7.Elasticsearch7SearchIndex;

import java.util.*;

import static org.vertexium.elasticsearch7.bulk.BulkUtils.*;

public class BulkUpdateItem extends BulkItem<UpdateItem> {
    private final ElementLocation sourceElementLocation;
    private final Map<String, String> source = new HashMap<>();
    private final Map<String, Object> fieldsToSet = new HashMap<>();
    private final Set<String> fieldsToRemove = new HashSet<>();
    private final Map<String, String> fieldsToRename = new HashMap<>();
    private final Set<String> additionalVisibilities = new HashSet<>();
    private final Set<String> additionalVisibilitiesToDelete = new HashSet<>();
    private boolean updateOnly = true;
    private Integer size;

    public BulkUpdateItem(
        String indexName,
        String type,
        String documentId,
        VertexiumObjectId vertexiumObjectId,
        ElementLocation sourceElementLocation
    ) {
        super(indexName, type, documentId, vertexiumObjectId);
        this.sourceElementLocation = sourceElementLocation;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void add(UpdateItem item) {
        super.add(item);
        size = null;

        for (Map.Entry<String, Object> itemEntry : item.getFieldsToSet().entrySet()) {
            Object itemValue = itemEntry.getValue();
            fieldsToSet.compute(itemEntry.getKey(), (key, existingValue) -> {
                if (existingValue == null) {
                    if (itemValue instanceof List) {
                        return new ArrayList((List) itemValue);
                    } else {
                        List newValue = new ArrayList();
                        newValue.add(itemValue);
                        return newValue;
                    }
                } else {
                    if (itemValue instanceof List) {
                        ((List) existingValue).addAll((List) itemValue);
                    } else {
                        ((List) existingValue).add(itemValue);
                    }
                    return existingValue;
                }
            });
        }

        for (Map.Entry<String, String> itemEntry : item.getFieldsToRename().entrySet()) {
            String itemValue = itemEntry.getValue();
            fieldsToRename.compute(itemEntry.getKey(), (key, existingValue) -> {
                if (existingValue == null) {
                    return itemValue;
                } else if (existingValue.equals(itemValue)) {
                    return itemValue;
                } else {
                    throw new VertexiumException("Changing the same property to two different visibilities in the same batch is not allowed: " + itemEntry.getKey());
                }
            });
        }

        source.putAll(item.getSource());
        fieldsToRemove.addAll(item.getFieldsToRemove());
        additionalVisibilities.addAll(item.getAdditionalVisibilities());
        additionalVisibilitiesToDelete.addAll(item.getAdditionalVisibilitiesToDelete());
        if (!item.isExistingElement()) {
            updateOnly = false;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void addToBulkRequest(Client client, BulkRequestBuilder bulkRequestBuilder) {
        UpdateRequestBuilder updateRequestBuilder = client
            .prepareUpdate(getIndexName(), getType(), getDocumentId());
        if (!updateOnly) {
            updateRequestBuilder = updateRequestBuilder
                .setScriptedUpsert(true)
                .setUpsert((Map<String, Object>) (Map) source);
        }
        UpdateRequest updateRequest = updateRequestBuilder
            .setScript(new Script(
                ScriptType.STORED,
                null,
                "updateFieldsOnDocumentScript",
                ImmutableMap.of(
                    "fieldsToSet", fieldsToSet,
                    "fieldsToRemove", new ArrayList<>(fieldsToRemove),
                    "fieldsToRename", fieldsToRename,
                    "additionalVisibilities", new ArrayList<>(additionalVisibilities),
                    "additionalVisibilitiesToDelete", new ArrayList<>(additionalVisibilitiesToDelete)
                )
            ))
            .setRetryOnConflict(Elasticsearch7SearchIndex.MAX_RETRIES)
            .request();
        bulkRequestBuilder.add(updateRequest);
    }

    @Override
    public int getSize() {
        if (size == null) {
            size = getIndexName().length()
                + getType().length()
                + getDocumentId().length()
                + calculateSizeOfId(getVertexiumObjectId())
                + calculateSizeOfMap(source)
                + calculateSizeOfMap(fieldsToSet)
                + calculateSizeOfList(fieldsToRemove)
                + calculateSizeOfMap(fieldsToRename)
                + calculateSizeOfList(additionalVisibilities)
                + calculateSizeOfList(additionalVisibilitiesToDelete);
        }
        return size;
    }

    public ElementLocation getSourceElementLocation() {
        return sourceElementLocation;
    }
}
