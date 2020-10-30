package com.heimdall.redis.cache.core;

import java.util.Collection;


public class ScanCursor<T> {

    private long cursorId;
    private Collection<T> items;

    public ScanCursor() {
    }

    public ScanCursor(long cursorId, Collection<T> items) {
        this.cursorId = cursorId;
        this.items = items;
    }

    public long getCursorId() {
        return cursorId;
    }

    public void setCursorId(long cursorId) {
        this.cursorId = cursorId;
    }

    public Collection<T> getItems() {
        return items;
    }

    public void setItems(Collection<T> items) {
        this.items = items;
    }
}