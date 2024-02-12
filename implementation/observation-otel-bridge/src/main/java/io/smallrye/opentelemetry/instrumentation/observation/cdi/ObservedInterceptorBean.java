package io.smallrye.opentelemetry.instrumentation.observation.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.InterceptionType;
import jakarta.enterprise.inject.spi.Interceptor;
import jakarta.enterprise.inject.spi.Prioritized;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.interceptor.InvocationContext;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;

public class ObservedInterceptorBean implements Interceptor<ObservedInterceptor>, Prioritized {
    private final BeanManager beanManager;

    public ObservedInterceptorBean(final BeanManager beanManager) {
        this.beanManager = beanManager;
    }

    @Override
    public Set<Annotation> getInterceptorBindings() {
        return Collections.singleton(ObservedLiteral.INSTANCE);
    }

    @Override
    public boolean intercepts(final InterceptionType type) {
        return InterceptionType.AROUND_INVOKE.equals(type);
    }

    @Override
    public Object intercept(
            final InterceptionType type,
            final ObservedInterceptor instance,
            final InvocationContext invocationContext)
            throws Exception {

        return instance.span(invocationContext);
    }

    @Override
    public Class<?> getBeanClass() {
        return ObservedInterceptorBean.class;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public ObservedInterceptor create(final CreationalContext<ObservedInterceptor> creationalContext) {
        Bean<?> bean = beanManager.resolve(beanManager.getBeans(ObservationRegistry.class));
        ObservationRegistry registry = (ObservationRegistry) beanManager.getReference(bean, ObservationRegistry.class,
                creationalContext);
        return new ObservedInterceptor(registry, null); // FIXME allow users to provide their own bean
    }

    @Override
    public void destroy(
            final ObservedInterceptor instance,
            final CreationalContext<ObservedInterceptor> creationalContext) {

    }

    @Override
    public Set<Type> getTypes() {
        return Collections.singleton(this.getBeanClass());
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return Collections.emptySet();
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return Dependent.class;
    }

    @Override
    public String getName() {
        return getBeanClass().getName();
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.emptySet();
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public int getPriority() {
        return 100;
    }

    public static class ObservedLiteral extends AnnotationLiteral<Observed> implements Observed {
        public static final ObservedLiteral INSTANCE = new ObservedLiteral();

        @Override
        public String name() {
            return null;
        }

        @Override
        public String contextualName() {
            return null;
        }

        @Override
        public String[] lowCardinalityKeyValues() {
            return new String[0];
        }
    }
}
