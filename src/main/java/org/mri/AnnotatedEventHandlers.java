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
import java.util.*;

public class AnnotatedEventHandlers extends AbstractProcessor<CtMethodImpl> {
    private final List<String> annotations;
    private Map<CtTypeReference, List<CtMethodImpl>> methodMap = new HashMap<>();

    public AnnotatedEventHandlers(String... annotations) {
        this.annotations = Arrays.asList(annotations);
    }

    @Override
    public void process(CtMethodImpl method) {
        Optional<CtAnnotation<? extends Annotation>> annotation = Iterables.tryFind(
                method.getAnnotations(),
                signatureContainsOneOf(this.annotations));
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

    private Predicate<CtAnnotation> signatureContainsOneOf(final List<String> list) {
        return new Predicate<CtAnnotation>() {
            @Override
            public boolean apply(CtAnnotation annotation) {
                return list.contains(annotation.getSignature());
            }
        };
    }

    public Map<CtTypeReference, List<CtMethodImpl>> executeSpoon(QueueProcessingManager queueProcessingManager) {
        queueProcessingManager.addProcessor(this);
        queueProcessingManager.process();
        return methodMap;
    }
}
