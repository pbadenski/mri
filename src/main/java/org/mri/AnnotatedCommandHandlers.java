package org.mri;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.QueueProcessingManager;
import spoon.support.reflect.declaration.CtMethodImpl;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

public class AnnotatedCommandHandlers extends AbstractProcessor<CtMethodImpl> {
    private final String annotation;
    private Map<CtTypeReference, CtMethodImpl> methods = new HashMap<>();

    public AnnotatedCommandHandlers(String annotation) {
        this.annotation = annotation;
    }

    @Override
    public void process(CtMethodImpl method) {
        Optional<CtAnnotation<? extends Annotation>> annotation = Iterables.tryFind(
                method.getAnnotations(),
                signatureEqualTo(this.annotation));
        if (annotation.isPresent()) {
            methods.put(((CtParameter)method.getParameters().get(0)).getType(), method);
        }
    }

    private Predicate<CtAnnotation> signatureEqualTo(final String value) {
        return new Predicate<CtAnnotation>() {
            @Override
            public boolean apply(CtAnnotation annotation) {
                return annotation.getSignature().equals(value);
            }
        };
    }

    public Map<CtTypeReference, CtMethodImpl> executeSpoon(QueueProcessingManager queueProcessingManager) {
        queueProcessingManager.addProcessor(this);
        queueProcessingManager.process();
        return methods;
    }
}
