package com.couchbase.transactions.tracing;

import brave.Tracing;
import io.jaegertracing.Configuration;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.okhttp3.OkHttpSender;

public class TracingUtils {
    public static TracingWrapper getTracer() {
        return getTracerJaeger();
    }

    private static TracingWrapper getTracerJaeger() {
        Tracer tracer = Configuration.fromEnv("txn-tests")
                .withSampler(Configuration.SamplerConfiguration.fromEnv().withType("const").withParam(1))
                .getTracer();
        TracingWrapper out = new TracingWrapperJaeger(tracer);

        if (!GlobalTracer.isRegistered()) {
            GlobalTracer.register(out.tracer());
        }

        return out;
    }

    private static TracingWrapper getTracerBrave() {
        OkHttpSender sender = OkHttpSender.create("http://127.0.0.1:9411/api/v2/spans");
        AsyncReporter<Span> spanReporter = AsyncReporter.create(sender);

        Tracing tracing = Tracing.newBuilder()
                .spanReporter(spanReporter)
                .localServiceName("StandardTest")
                .build();

        TracingWrapper out = new TracingWrapperBrave(tracing, spanReporter, sender);

        if (!GlobalTracer.isRegistered()) {
            GlobalTracer.register(out.tracer());
        }

        return out;
    }

    private TracingUtils() {}
}
