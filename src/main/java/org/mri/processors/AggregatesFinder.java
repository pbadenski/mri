package org.mri.processors;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.QueueProcessingManager;
import spoon.support.reflect.declaration.CtFieldImpl;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public class AggregatesFinder {
    public static final String AXON_AGGREGATE_IDENTIFIER_ANNOTATION =
            "@org.axonframework.eventsourcing.annotation.AggregateIdentifier";
    private List<CtTypeReference> aggregates = new ArrayList<>();

    private class Processor extends AbstractProcessor<CtFieldImpl> {

        @Override
        public void process(CtFieldImpl field) {
            Optional<CtAnnotation<? extends Annotation>> annotation = Iterables.tryFind(
                    field.getAnnotations(),
                    signatureEqualTo(AXON_AGGREGATE_IDENTIFIER_ANNOTATION));
            if (annotation.isPresent()) {
                aggregates.add(field.getDeclaringType().getReference());
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

    }
    public List<CtTypeReference> all(QueueProcessingManager queueProcessingManager) {
        queueProcessingManager.addProcessor(new Processor());
        queueProcessingManager.process();
        return aggregates;
    }
}
