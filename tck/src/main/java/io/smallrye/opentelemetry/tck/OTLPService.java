package io.smallrye.opentelemetry.tck;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;
import io.opentelemetry.proto.trace.v1.InstrumentationLibrarySpans;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.Span;

/**
 * @author Pavol Loffay
 */
public class OTLPService extends TraceServiceGrpc.TraceServiceImplBase {

    private List<ResourceSpans> resourceSpansList = new CopyOnWriteArrayList<>();

    @Override
    public void export(ExportTraceServiceRequest request, StreamObserver<ExportTraceServiceResponse> responseObserver) {
        this.resourceSpansList.addAll(request.getResourceSpansList());
        responseObserver.onNext(ExportTraceServiceResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    public List<ResourceSpans> getResourceSpansList() {
        return this.resourceSpansList;
    }

    public List<Span> getSpans() {
        List<Span> spanList = new ArrayList<>();
        for (ResourceSpans rss : this.getResourceSpansList()) {
            for (InstrumentationLibrarySpans instSpans : rss.getInstrumentationLibrarySpansList()) {
                for (Span span : instSpans.getSpansList()) {
                    spanList.add(span);
                }
            }
        }
        return spanList;
    }

    public int getSpanCount() {
        int count = 0;
        for (ResourceSpans rss : this.resourceSpansList) {
            count += rss.getInstrumentationLibrarySpansCount();
        }
        return count;
    }

    public void reset() {
        this.resourceSpansList.clear();
    }
}
