package com.couchbase.transactions.tracing;

import io.opentracing.Tracer;

public class TracingWrapperJaeger implements TracingWrapper {
    private final Tracer tracer;

    public TracingWrapperJaeger(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void close() {

    }

    @Override
    public Tracer tracer() {
        return tracer;
    }
}
