package io.smallrye.opentelemetry.jaxrs2.server;

import java.io.IOException;

import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Status;
import io.opentelemetry.trace.attributes.SemanticAttributes;
import io.smallrye.opentelemetry.jaxrs2.server.internal.CastUtils;
import io.smallrye.opentelemetry.jaxrs2.server.internal.SpanWrapper;

/**
 * Filter which finishes span after server processing. It is required to be registered.
 *
 * @author Pavol Loffay
 */
public class SpanFinishingFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        try {
            chain.doFilter(request, response);
        } catch (Exception ex) {
            SpanWrapper spanWrapper = getSpanWrapper(httpRequest);
            if (spanWrapper != null) {
                SemanticAttributes.HTTP_STATUS_CODE.set(spanWrapper.get(),
                        Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
                annotateException(spanWrapper.get(), ex);
            }
            throw ex;
        } finally {
            SpanWrapper spanWrapper = getSpanWrapper(httpRequest);
            if (spanWrapper != null) {
                spanWrapper.getScope().close();
                if (request.isAsyncStarted()) {
                    request.getAsyncContext().addListener(new SpanFinisher(spanWrapper), request, response);
                } else {
                    spanWrapper.finish();
                }
            }
        }
    }

    private SpanWrapper getSpanWrapper(HttpServletRequest request) {
        return CastUtils.cast(request.getAttribute(SpanWrapper.PROPERTY_NAME), SpanWrapper.class);
    }

    @Override
    public void destroy() {
    }

    static class SpanFinisher implements AsyncListener {
        private SpanWrapper spanWrapper;

        SpanFinisher(SpanWrapper spanWrapper) {
            this.spanWrapper = spanWrapper;
        }

        @Override
        public void onComplete(AsyncEvent event) throws IOException {
            HttpServletResponse httpResponse = (HttpServletResponse) event.getSuppliedResponse();
            if (httpResponse.getStatus() >= 500) {
                annotateException(spanWrapper.get(), event.getThrowable());
            }
            SemanticAttributes.HTTP_STATUS_CODE.set(spanWrapper.get(), httpResponse.getStatus());
            spanWrapper.finish();
        }

        @Override
        public void onTimeout(AsyncEvent event) throws IOException {
        }

        @Override
        public void onError(AsyncEvent event) throws IOException {
            // this handler is called when exception is thrown in async handler
            // note that exception logs are added in filter not here
        }

        @Override
        public void onStartAsync(AsyncEvent event) throws IOException {
        }
    }

    private static void annotateException(Span span, Throwable throwable) {
        Status unknownStatus = Status.UNKNOWN;
        if (throwable != null) {
            unknownStatus.withDescription(throwable.getMessage());
        }
        span.setStatus(unknownStatus);
    }
}
