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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnotatedEventHandlers extends AbstractProcessor<CtMethodImpl> {
    private final String annotation;
    private Map<CtTypeReference, List<CtMethodImpl>> methodMap = new HashMap<>();

    public AnnotatedEventHandlers(String annotation) {
        this.annotation = annotation;
    }

    @Override
    public void process(CtMethodImpl method) {
        Optional<CtAnnotation<? extends Annotation>> annotation = Iterables.tryFind(
                method.getAnnotations(),
                signatureEqualTo(this.annotation));
        if (annotation.isPresent()) {
            CtTypeReference parameterType = ((CtParameter) method.getParameters().get(0)).getType();
            List<CtMethodImpl> methods = methodMap.get(parameterType);
            if (methods == null) {
                methods = new ArrayList<>();
                methodMap.put(parameterType, methods);
            }
            methods.add(method);
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

    public Map<CtTypeReference, List<CtMethodImpl>> executeSpoon(QueueProcessingManager queueProcessingManager) {
        queueProcessingManager.addProcessor(this);
        queueProcessingManager.process();
        return methodMap;
    }
}
