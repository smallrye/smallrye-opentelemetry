package io.smallrye.opentelemetry.implementation.micrometer.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedParameter;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.util.Nonbinding;

import io.micrometer.core.annotation.Counted;

public class MicrometerExtension implements Extension {
    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager) {
        beforeBeanDiscovery.addInterceptorBinding(
                new CountedAnnotatedType(beanManager.createAnnotatedType(Counted.class)));

        beforeBeanDiscovery.addAnnotatedType(MeterRegistryProducer.class, MeterRegistryProducer.class.getName());
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {
        afterBeanDiscovery.addBean(new CountedInterceptorBean(beanManager));
    }

    static class CountedAnnotatedType implements AnnotatedType<Counted> {
        private final AnnotatedType<Counted> delegate;
        private final Set<AnnotatedMethod<? super Counted>> methods;

        public CountedAnnotatedType(AnnotatedType<Counted> delegate) {
            this.delegate = delegate;
            this.methods = new HashSet<>();

            for (AnnotatedMethod<? super Counted> method : delegate.getMethods()) {
                methods.add(new AnnotatedMethod<Counted>() {
                    private final AnnotatedMethod<Counted> delegate = (AnnotatedMethod<Counted>) method;
                    private final Set<Annotation> annotations = Collections.singleton(Nonbinding.Literal.INSTANCE);

                    @Override
                    public Method getJavaMember() {
                        return delegate.getJavaMember();
                    }

                    @Override
                    public List<AnnotatedParameter<Counted>> getParameters() {
                        return delegate.getParameters();
                    }

                    @Override
                    public boolean isStatic() {
                        return delegate.isStatic();
                    }

                    @Override
                    public AnnotatedType<Counted> getDeclaringType() {
                        return delegate.getDeclaringType();
                    }

                    @Override
                    public Type getBaseType() {
                        return delegate.getBaseType();
                    }

                    @Override
                    public Set<Type> getTypeClosure() {
                        return delegate.getTypeClosure();
                    }

                    @Override
                    public <T extends Annotation> T getAnnotation(final Class<T> annotationType) {
                        if (annotationType.equals(Nonbinding.class)) {
                            return (T) annotations.iterator().next();
                        }
                        return null;
                    }

                    @Override
                    public Set<Annotation> getAnnotations() {
                        return annotations;
                    }

                    @Override
                    public boolean isAnnotationPresent(final Class<? extends Annotation> annotationType) {
                        return annotationType.equals(Nonbinding.class);
                    }
                });
            }

        }

        @Override
        public Class<Counted> getJavaClass() {
            return delegate.getJavaClass();
        }

        @Override
        public Set<AnnotatedConstructor<Counted>> getConstructors() {
            return delegate.getConstructors();
        }

        @Override
        public Set<AnnotatedMethod<? super Counted>> getMethods() {
            return methods;
        }

        @Override
        public Set<AnnotatedField<? super Counted>> getFields() {
            return delegate.getFields();
        }

        @Override
        public Type getBaseType() {
            return delegate.getBaseType();
        }

        @Override
        public Set<Type> getTypeClosure() {
            return delegate.getTypeClosure();
        }

        @Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
            return delegate.getAnnotation(annotationType);
        }

        @Override
        public Set<Annotation> getAnnotations() {
            return delegate.getAnnotations();
        }

        @Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
            return delegate.isAnnotationPresent(annotationType);
        }
    }
}
