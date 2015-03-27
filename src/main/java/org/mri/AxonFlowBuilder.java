package org.mri;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.declaration.CtMethodImpl;

import java.util.*;

public class AxonFlowBuilder {
    private final Map<CtTypeReference, List<CtMethodImpl>> eventHandlers;
    private final Map<CtTypeReference, CtMethodImpl> commandHandlers;
    private final MethodCallHierarchyBuilder callHierarchyBuilder;
    private boolean matchEventsByName;

    public AxonFlowBuilder(Map<CtTypeReference, Set<CtTypeReference>> classHierarchy,
                           Map<CtExecutableReference, List<CtExecutableReference>> callList,
                           Map<CtTypeReference, List<CtMethodImpl>> eventHandlers,
                           Map<CtTypeReference, CtMethodImpl> commandHandlers,
                           boolean matchEventsByName) {
        this.eventHandlers = eventHandlers;
        this.commandHandlers = commandHandlers;
        this.matchEventsByName = matchEventsByName;
        this.callHierarchyBuilder = new MethodCallHierarchyBuilder(callList, classHierarchy);
    }

    List<AxonNode> buildFlow(ArrayList<CtExecutableReference> methodReferences) {
        List<AxonNode> nodes = new ArrayList<>();
        for (CtExecutableReference each : methodReferences) {
            AxonNode root = new AxonNode("controller", each);
            nodes.add(root);
            buildCommandFlow(root);
        }
        return nodes;
    }

    private void buildCommandFlow(AxonNode node) {
        MethodCall methodCall = this.callHierarchyBuilder.buildCallHierarchy(node.reference());
        Optional<MethodCall> commandConstruction = Iterables.tryFind(methodCall.asList(), isCommandPredicate());
        if (!commandConstruction.isPresent()) {
            return;
        }
        CtMethodImpl commandHandler = commandHandlers.get(commandConstruction.get().reference().getDeclaringType());
        AxonNode commandConstructionNode = new AxonNode("command", commandConstruction.get().reference());
        node.add(commandConstructionNode);
        AxonNode commandHandlerNode = new AxonNode("command handler", commandHandler.getReference());
        commandConstructionNode.add(commandHandlerNode);
        buildEventFlow(commandHandlerNode);
    }

    private Predicate<MethodCall> isCommandPredicate() {
        return new Predicate<MethodCall>() {
            @Override
            public boolean apply(MethodCall input) {
                return commandHandlers.keySet().contains(input.reference().getDeclaringType());
            }
        };
    }

    private AxonNode buildEventFlow(AxonNode node) {
        MethodCall methodCall = this.callHierarchyBuilder.buildCallHierarchy(node.reference());

        Iterable<MethodCall> eventConstructionInstances = Iterables.filter(methodCall.asList(), isEventPredicate());
        for (MethodCall eventConstruction : eventConstructionInstances) {
            AxonNode eventNode = new AxonNode("event", eventConstruction.reference());
            node.add(eventNode);
            for (CtMethodImpl eventHandler : findEventHandlersFor(eventNode.reference().getDeclaringType())) {
                AxonNode eventHandlerNode = new AxonNode("event listener", eventHandler.getReference());
                eventNode.add(eventHandlerNode);
                buildCommandFlow(eventHandlerNode);
            }
        }
        return node;
    }

    private List<CtMethodImpl> findEventHandlersFor(CtTypeReference type) {
        Map<String, List<CtMethodImpl>> eventHandlerByNames = new HashMap<>();
        for (Map.Entry<CtTypeReference, List<CtMethodImpl>> entry : eventHandlers.entrySet()) {
            eventHandlerByNames.put(entry.getKey().getSimpleName(), entry.getValue());
        }
        if (matchEventsByName) {
            return eventHandlerByNames.get(type.getSimpleName());
        } else {
            return this.eventHandlers.get(type);
        }
    }

    private Predicate<MethodCall> isEventPredicate() {
        if (matchEventsByName){
            return matchEventsByNamePredicate();
        } else {
            return matchEventsBySignaturePredicate();
        }

    }

    private Predicate<MethodCall> matchEventsBySignaturePredicate() {
        return new Predicate<MethodCall>() {
            @Override
            public boolean apply(final MethodCall input) {
                return eventHandlers.keySet().contains(input.reference().getDeclaringType());
            }
        };
    }

    private Predicate<MethodCall> matchEventsByNamePredicate() {
        final List<String> eventHandlerNames =
                Lists.newArrayList(Iterables.transform(eventHandlers.keySet(), new Function<CtTypeReference, String>() {
                    @Override
                    public String apply(CtTypeReference type) {
                        return type.getSimpleName();
                    }
                }));
        return new Predicate<MethodCall>() {
            @Override
            public boolean apply(final MethodCall input) {
                return eventHandlerNames.contains(input.reference().getDeclaringType().getSimpleName());
            }
        };
    }

}
