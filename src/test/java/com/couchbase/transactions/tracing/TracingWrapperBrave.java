package com.couchbase.transactions.tracing;

import brave.Tracing;
import brave.opentracing.BraveTracer;
import io.opentracing.Tracer;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.okhttp3.OkHttpSender;

public class TracingWrapperBrave implements TracingWrapper {
    private final Tracer tracer;
    private final Tracing tracing;
    private final AsyncReporter<Span> spanReporter;
    private final OkHttpSender sender;

    public TracingWrapperBrave(Tracing tracing, AsyncReporter<Span> spanReporter, OkHttpSender sender) {
        this.tracing = tracing;
        this.spanReporter = spanReporter;
        this.sender = sender;
        tracer = BraveTracer.create(tracing);
    }

    @Override
    public void close() {
        tracing.close();
        spanReporter.close();
        sender.close();
    }

    @Override
    public Tracer tracer() {
        return tracer;
    }
}
