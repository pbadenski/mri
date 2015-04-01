package org.mri;


import com.google.common.base.Predicate;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.declaration.CtMethodImpl;

import java.util.*;

public class EventHandlerIdentificationByNameStrategy implements EventHandlerIdentificationStrategy {


    private final HashMap<String, List<CtMethodImpl>> eventHandlerByNames = new HashMap<>();

    public EventHandlerIdentificationByNameStrategy(Map<CtTypeReference, List<CtMethodImpl>> eventHandlers) {
        for (Map.Entry<CtTypeReference, List<CtMethodImpl>> entry : eventHandlers.entrySet()) {
            List<CtMethodImpl> collectedEventHandlers = eventHandlerByNames.get(entry.getKey().getSimpleName());
            if (collectedEventHandlers == null) {
                collectedEventHandlers = new ArrayList<>();
                eventHandlerByNames.put(entry.getKey().getSimpleName(), collectedEventHandlers);
            }
            collectedEventHandlers.addAll(entry.getValue());
        }
    }

    @Override
    public Predicate<MethodCall> isEventPredicate() {
        return matchEventsByNamePredicate();
    }

    @Override
    public List<CtMethodImpl> findEventHandlers(CtTypeReference type) {

        return eventHandlerByNames.get(type.getSimpleName());
    }

    private Predicate<MethodCall> matchEventsByNamePredicate() {
        final Set<String> eventHandlerNames = eventHandlerByNames.keySet();
        return new Predicate<MethodCall>() {
            @Override
            public boolean apply(final MethodCall input) {
                return eventHandlerNames.contains(input.reference().getDeclaringType().getSimpleName());
            }
        };
    }

}
