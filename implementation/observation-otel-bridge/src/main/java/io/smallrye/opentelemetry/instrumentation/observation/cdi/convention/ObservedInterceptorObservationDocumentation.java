package io.smallrye.opentelemetry.instrumentation.observation.cdi.convention;

import io.micrometer.common.docs.KeyName;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationConvention;
import io.micrometer.observation.docs.ObservationDocumentation;

/**
 * Class that will provide the default names for the attributes.
 * Also used to generate documentation about those attributes
 */
public enum ObservedInterceptorObservationDocumentation implements ObservationDocumentation {
    /**
     * The observation used by the Observed interceptor.
     */
    DEFAULT {
        @Override
        public Class<? extends ObservationConvention<? extends Observation.Context>> getDefaultConvention() {
            return DefaultObservedInterceptorObservationConvention.class;
        }

        @Override
        public KeyName[] getLowCardinalityKeyNames() {
            return ObservedKeyValues.values();
        }
    };

    enum ObservedKeyValues implements KeyName {

        /**
         * The method being observed (traced or measured).
         */
        CODE_FUNCTION {
            @Override
            public String asString() {
                // TODO See https://github.com/micrometer-metrics/micrometer-docs-generator/issues/130#issuecomment-1935680555
                // return SemanticAttributes.CODE_FUNCTION.getKey();
                return "code.function";
            }
        },
        /**
         * The class of method being observed (traced or measured).
         */
        CODE_NAMESPACE {
            @Override
            public String asString() {
                return "code.namespace";
            }
        }
    }
}
