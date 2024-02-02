package io.smallrye.opentelemetry.implementation.micrometer.cdi;

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

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.instrument.MeterRegistry;

public class CountedInterceptorBean implements Interceptor<CountedInterceptor>, Prioritized {

    private final BeanManager beanManager;

    public CountedInterceptorBean(BeanManager beanManager) {
        this.beanManager = beanManager;
    }

    @Override
    public Set<Annotation> getInterceptorBindings() {
        return Collections.singleton(CountedLiteral.INSTANCE);
    }

    @Override
    public boolean intercepts(InterceptionType type) {
        return InterceptionType.AROUND_INVOKE.equals(type);
    }

    @Override
    public Object intercept(InterceptionType type, CountedInterceptor instance, InvocationContext ctx) throws Exception {
        return instance.intercept(ctx);
    }

    @Override
    public Class<?> getBeanClass() {
        return CountedInterceptorBean.class;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.emptySet();
    }

    @Override
    public CountedInterceptor create(CreationalContext<CountedInterceptor> creationalContext) {
        Bean<?> bean = (Bean<?>) beanManager.resolve(beanManager.getBeans(MeterRegistry.class));
        MeterRegistry meterRegistry = (MeterRegistry) beanManager.getReference(bean, MeterRegistry.class, creationalContext);
        return new CountedInterceptor(meterRegistry);
    }

    @Override
    public void destroy(CountedInterceptor instance, CreationalContext<CountedInterceptor> creationalContext) {
        //empty
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

    public static class CountedLiteral extends AnnotationLiteral<Counted> implements Counted {
        public static final Annotation INSTANCE = new CountedLiteral();

        @Override
        public String value() {
            return null;
        }

        @Override
        public boolean recordFailuresOnly() {
            return false;
        }

        @Override
        public String[] extraTags() {
            return new String[0];
        }

        @Override
        public String description() {
            return null;
        }
    }
}
