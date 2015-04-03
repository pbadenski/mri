package org.mri.processors;

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

public class CommandHandlersFinder {
    private static final String AXON_COMMAND_HANDLER = "@org.axonframework.commandhandling.annotation.CommandHandler";

    private Map<CtTypeReference, CtMethodImpl> methods = new HashMap<>();

    private class Processor extends AbstractProcessor<CtMethodImpl> {
        @Override
        public void process(CtMethodImpl method) {
            Optional<CtAnnotation<? extends Annotation>> annotation = Iterables.tryFind(
                    method.getAnnotations(),
                    signatureEqualTo(AXON_COMMAND_HANDLER));
            if (annotation.isPresent()) {
                methods.put(((CtParameter)method.getParameters().get(0)).getType(), method);
            }
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

    public Map<CtTypeReference, CtMethodImpl> all(QueueProcessingManager queueProcessingManager) {
        queueProcessingManager.addProcessor(new Processor());
        queueProcessingManager.process();
        return methods;
    }
}
