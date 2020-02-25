package com.couchbase.transactions.tracing;

import io.opentracing.Tracer;

public interface TracingWrapper extends AutoCloseable {
    @Override
    void close();

    Tracer tracer();
}

