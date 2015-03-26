package org.mri;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.declaration.CtMethodImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AxonFlowBuilder {
    private Map<CtTypeReference, Set<CtTypeReference>> classHierarchy;
    private final Map<CtExecutableReference, List<CtExecutableReference>> callList;
    private final Map<CtTypeReference, List<CtMethodImpl>> eventHandlers;
    private final Map<CtTypeReference, CtMethodImpl> commandHandlers;
    private final MethodCallHierarchyBuilder callHierarchyBuilder;

    public AxonFlowBuilder(Map<CtTypeReference, Set<CtTypeReference>> classHierarchy,
                           Map<CtExecutableReference, List<CtExecutableReference>> callList,
                           Map<CtTypeReference, List<CtMethodImpl>> eventHandlers,
                           Map<CtTypeReference, CtMethodImpl> commandHandlers) {
        this.classHierarchy = classHierarchy;
        this.callList = callList;
        this.eventHandlers = eventHandlers;
        this.commandHandlers = commandHandlers;
        this.callHierarchyBuilder = new MethodCallHierarchyBuilder(callList, classHierarchy);
    }

    void buildFlow(ArrayList<CtExecutableReference> methodReferences) {
        for (CtExecutableReference each : methodReferences) {
            MethodCall methodCall = this.callHierarchyBuilder.buildCallHierarchy(each);
            CtMethodImpl commandHandler = forCommandCaller(methodCall, "\t");
            forEvent(commandHandler, "\t\t");
        }
    }

    private CtMethodImpl forCommandCaller(MethodCall each, String indent) {
        Optional<MethodCall> commandConstruction = Iterables.tryFind(each.asList(), new Predicate<MethodCall>() {
            @Override
            public boolean apply(MethodCall input) {
                return commandHandlers.keySet().contains(input.reference().getDeclaringType());
            }
        });
        if (!commandConstruction.isPresent()) {
            return null;
        }
        CtMethodImpl commandHandler = commandHandlers.get(commandConstruction.get().reference().getDeclaringType());
        System.out.println(indent + "-> " + commandConstruction.get().reference().toString());
        System.out.println(indent + "-- [handler] --");
        System.out.println(indent + "  -> " + commandHandler.getReference().toString());
        return commandHandler;
    }

    private List<CtMethodImpl> forEvent(CtMethodImpl commandHandler, String indent) {
        MethodCallHierarchyBuilder commandHandlerCall = new MethodCallHierarchyBuilder(callList, classHierarchy);
        MethodCall secondMethodCall = commandHandlerCall.buildCallHierarchy(commandHandler.getReference());

        Iterable<MethodCall> eventConstructionInstances = Iterables.filter(secondMethodCall.asList(), new Predicate<MethodCall>() {
            @Override
            public boolean apply(MethodCall input) {
                return eventHandlers.keySet().contains(input.reference().getDeclaringType());
            }
        });
        List<CtMethodImpl> localEventHandlers = new ArrayList<>();
        for (MethodCall eventConstruction : eventConstructionInstances) {
            System.out.println(indent + "-> " + eventConstruction.reference().toString());
            System.out.println(indent + "-- [listeners] --");
            for (CtMethodImpl eventHandler : this.eventHandlers.get(eventConstruction.reference().getDeclaringType())) {
                System.out.println(indent + "  -> " + eventHandler.getReference().toString());
                localEventHandlers.add(eventHandler);
                final MethodCallHierarchyBuilder methodCallHierarchyBuilder = new MethodCallHierarchyBuilder(callList, classHierarchy);
                forCommandCaller(
                        methodCallHierarchyBuilder.buildCallHierarchy(eventHandler.getReference()), "\t\t\t");
                System.out.println();
            }
        }
        return localEventHandlers;
    }

}
