package org.vertexium.elasticsearch7.utils;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.vertexium.elasticsearch7.ElasticsearchGraphQueryIdIterable;
import org.vertexium.elasticsearch7.IdStrategy;
import org.vertexium.query.AggregationResult;
import org.vertexium.query.IterableWithScores;
import org.vertexium.query.QueryResultsIterable;
import org.vertexium.util.CloseableIterator;
import org.vertexium.util.CloseableUtils;
import org.vertexium.util.VertexiumLogger;
import org.vertexium.util.VertexiumLoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public abstract class InfiniteScrollIterable<T> implements QueryResultsIterable<T>, IterableWithScores<T> {
    private static final VertexiumLogger LOGGER = VertexiumLoggerFactory.getLogger(InfiniteScrollIterable.class);
    private static final String SCROLL_API_STACK_TRACE_LOGGER_NAME = "org.vertexium.elasticsearch7.SCROLL_API_STACK_TRACE";
    private static final VertexiumLogger SCROLL_API_STACK_TRACE_LOGGER = VertexiumLoggerFactory.getLogger(SCROLL_API_STACK_TRACE_LOGGER_NAME);
    private final Long limit;
    private QueryResultsIterable<T> firstIterable;
    private boolean initCalled;
    private boolean firstCall;
    private SearchResponse response;

    protected InfiniteScrollIterable(Long limit) {
        this.limit = limit;
    }

    protected abstract SearchResponse getInitialSearchResponse();

    protected abstract SearchResponse getNextSearchResponse(String scrollId);

    protected abstract QueryResultsIterable<T> searchResponseToIterable(SearchResponse searchResponse);

    protected abstract void closeScroll(String scrollId);

    protected abstract IdStrategy getIdStrategy();

    @Override
    public void close() {
    }

    private void init() {
        if (initCalled) {
            return;
        }
        response = getInitialSearchResponse();
        if (response == null) {
            firstIterable = null;
        } else {
            firstIterable = searchResponseToIterable(response);
        }
        firstCall = true;
        initCalled = true;
    }

    @Override
    public <TResult extends AggregationResult> TResult getAggregationResult(String name, Class<? extends TResult> resultType) {
        init();
        if (firstIterable == null) {
            return AggregationResult.createEmptyResult(resultType);
        }
        return firstIterable.getAggregationResult(name, resultType);
    }

    @Override
    public long getTotalHits() {
        init();
        if (firstIterable == null) {
            return 0;
        }
        return firstIterable.getTotalHits();
    }

    @Override
    public Double getScore(Object id) {
        if (response == null) {
            return null;
        }
        for (SearchHit hit : response.getHits()) {
            Object hitId = ElasticsearchGraphQueryIdIterable.idFromSearchHit(hit, getIdStrategy());
            if (hitId == null) {
                continue;
            }
            if (id.equals(hitId)) {
                return (double) hit.getScore();
            }
        }
        return null;
    }

    @Override
    public Iterator<T> iterator() {
        init();
        if (response == null) {
            return new ArrayList<T>().iterator();
        }

        Iterator<T> it;
        if (firstCall) {
            it = firstIterable.iterator();
            firstCall = false;
        } else {
            response = getInitialSearchResponse();
            it = searchResponseToIterable(response).iterator();
        }
        String scrollId = response.getScrollId();
        return new InfiniteIterator(scrollId, it);
    }

    private class InfiniteIterator implements CloseableIterator<T> {
        private final String scrollId;
        private final StackTraceElement[] stackTrace;
        private boolean scrollIdClosed;
        private Iterator<T> it;
        private T next;
        private T current;
        private long currentResultNumber = 0;

        public InfiniteIterator(String scrollId, Iterator<T> it) {
            this.scrollId = scrollId;
            this.it = it;
            if (SCROLL_API_STACK_TRACE_LOGGER.isTraceEnabled()) {
                stackTrace = Thread.currentThread().getStackTrace();
            } else {
                stackTrace = null;
            }
        }

        @Override
        public boolean hasNext() {
            loadNext();
            if (next == null) {
                close();
            }
            return next != null;
        }

        @Override
        public T next() {
            loadNext();
            if (next == null) {
                throw new NoSuchElementException();
            }
            this.current = this.next;
            this.next = null;
            return this.current;
        }

        private void loadNext() {
            if (this.next != null || it == null) {
                return;
            }

            boolean isUnderLimit = limit == null || currentResultNumber < limit;
            if (isUnderLimit && it.hasNext()) {
                this.next = it.next();
                currentResultNumber++;
            } else {
                CloseableUtils.closeQuietly(it);
                it = null;

                if (isUnderLimit && getTotalHits() > currentResultNumber) {
                    QueryResultsIterable<T> iterable = searchResponseToIterable(getNextSearchResponse(scrollId));
                    it = iterable.iterator();
                    if (!it.hasNext()) {
                        it = null;
                    } else {
                        this.next = it.next();
                        currentResultNumber++;
                    }
                }
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
            closeScroll(scrollId);
            scrollIdClosed = true;
            CloseableUtils.closeQuietly(it);
            InfiniteScrollIterable.this.close();
        }

        @Override
        protected void finalize() throws Throwable {
            if (!scrollIdClosed) {
                LOGGER.warn(
                    "Elasticsearch scroll not closed. This can occur if you do not iterate completly or did not call close on the iterable.%s",
                    stackTrace != null && SCROLL_API_STACK_TRACE_LOGGER.isTraceEnabled()
                        ? ""
                        : String.format(" To enable stack traces enable trace logging on \"%s\"", SCROLL_API_STACK_TRACE_LOGGER_NAME)
                );
                if (stackTrace != null) {
                    SCROLL_API_STACK_TRACE_LOGGER.trace(
                        "Source of unclosed iterable:\n  %s",
                        Arrays.stream(stackTrace)
                            .map(StackTraceElement::toString)
                            .collect(Collectors.joining("\n  "))
                    );
                }
                close();
            }
            super.finalize();
        }
    }
}
