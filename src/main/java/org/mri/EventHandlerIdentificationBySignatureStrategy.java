package org.mri;


import com.google.common.base.Predicate;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.declaration.CtMethodImpl;

import java.util.List;
import java.util.Map;

public class EventHandlerIdentificationBySignatureStrategy implements EventHandlerIdentificationStrategy {

    private final Map<CtTypeReference, List<CtMethodImpl>> eventHandlers;

    public EventHandlerIdentificationBySignatureStrategy(Map<CtTypeReference, List<CtMethodImpl>> eventHandlers) {
        this.eventHandlers = eventHandlers;
    }

    @Override
    public Predicate<MethodCall> isEventPredicate() {
        return matchEventsBySignaturePredicate();
    }

    @Override
    public List<CtMethodImpl> findEventHandlers(CtTypeReference type) {
        return this.eventHandlers.get(type);
    }

    private Predicate<MethodCall> matchEventsBySignaturePredicate() {
        return new Predicate<MethodCall>() {
            @Override
            public boolean apply(final MethodCall input) {
                return eventHandlers.keySet().contains(input.reference().getDeclaringType());
            }
        };
    }

}
