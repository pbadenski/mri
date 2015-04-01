package org.mri;

import com.google.common.base.Predicate;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.declaration.CtMethodImpl;

import java.util.List;

public interface EventHandlerIdentificationStrategy {
    Predicate<MethodCall> isEventPredicate();

    List<CtMethodImpl> findEventHandlers(CtTypeReference type);
}
