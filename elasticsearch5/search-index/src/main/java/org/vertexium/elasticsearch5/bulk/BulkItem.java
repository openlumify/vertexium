package org.vertexium.elasticsearch5.bulk;

import org.elasticsearch.action.ActionRequest;
import org.vertexium.ElementId;
import org.vertexium.elasticsearch5.utils.ElasticsearchRequestUtils;

public class BulkItem {
    private final String indexName;
    private final ElementId elementId;
    private final int size;
    private final ActionRequest actionRequest;
    private long createdOrLastTriedTime;
    private int failCount;

    public BulkItem(
        String indexName,
        ElementId elementId,
        ActionRequest actionRequest
    ) {
        this.indexName = indexName;
        this.elementId = elementId;
        this.size = ElasticsearchRequestUtils.getSize(actionRequest);
        this.actionRequest = actionRequest;
        this.createdOrLastTriedTime = System.currentTimeMillis();
    }

    public String getIndexName() {
        return indexName;
    }

    public ElementId getElementId() {
        return elementId;
    }

    public int getSize() {
        return size;
    }

    public ActionRequest getActionRequest() {
        return actionRequest;
    }

    public long getCreatedOrLastTriedTime() {
        return createdOrLastTriedTime;
    }

    public void updateCreatedOrLastTriedTime() {
        this.createdOrLastTriedTime = System.currentTimeMillis();
    }

    public void incrementFailCount() {
        failCount++;
    }

    public int getFailCount() {
        return failCount;
    }

    @Override
    public String toString() {
        return String.format("%s {elementId=%s, actionRequest=%s}", getClass().getSimpleName(), elementId, actionRequest);
    }
}