package io.smallrye.opentelemetry.implementation.common;

import io.opentelemetry.extension.annotations.SpanAttribute;
import io.opentelemetry.extension.annotations.WithSpan;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import static io.opentelemetry.api.trace.SpanKind.SERVER;

@ApplicationScoped
public class SpanBean {
    @WithSpan
    public void span() {

    }

    @WithSpan("name")
    public void spanName() {

    }

    @WithSpan(kind = SERVER)
    public void spanKind() {

    }

    @WithSpan
    public void spanArgs(@SpanAttribute(value = "arg") String arg) {

    }

    @Inject
    SpanChildBean spanChildBean;

    @WithSpan
    public void spanChild() {
        spanChildBean.spanChild();
    }

    @ApplicationScoped
    public static class SpanChildBean {
        @WithSpan
        public void spanChild() {

        }
    }
}