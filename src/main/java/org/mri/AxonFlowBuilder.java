package org.mri;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.declaration.CtMethodImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AxonFlowBuilder {
    private final EventHandlerIdentificationStrategy eventHandlerIdentificationStrategy;
    private final Map<CtTypeReference, List<CtMethodImpl>> eventHandlers;
    private final Map<CtTypeReference, CtMethodImpl> commandHandlers;
    private final MethodCallHierarchyBuilder callHierarchyBuilder;
    private boolean matchEventsByName;

    public AxonFlowBuilder(Map<CtTypeReference, Set<CtTypeReference>> classHierarchy,
                           Map<CtExecutableReference, List<CtExecutableReference>> callList,
                           Map<CtTypeReference, List<CtMethodImpl>> eventHandlers,
                           Map<CtTypeReference, CtMethodImpl> commandHandlers,
                           boolean matchEventsByName) {
        if (matchEventsByName) {
            this.eventHandlerIdentificationStrategy = new EventHandlerIdentificationByNameStrategy(eventHandlers);
        }
        else {
            this.eventHandlerIdentificationStrategy = new EventHandlerIdentificationBySignatureStrategy(eventHandlers);
        }
        this.eventHandlers = eventHandlers;
        this.commandHandlers = commandHandlers;
        this.matchEventsByName = matchEventsByName;
        this.callHierarchyBuilder = new MethodCallHierarchyBuilder(callList, classHierarchy);
    }

    List<AxonNode> buildFlow(ArrayList<CtExecutableReference> methodReferences) {
        List<AxonNode> nodes = new ArrayList<>();
        for (CtExecutableReference each : methodReferences) {
            AxonNode root = new AxonNode(AxonNode.Type.CONTROLLER, each);
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
        AxonNode commandConstructionNode = new AxonNode(AxonNode.Type.COMMAND, commandConstruction.get().reference());
        node.add(commandConstructionNode);
        AxonNode commandHandlerNode = new AxonNode(AxonNode.Type.COMMAND_HANDLER, commandHandler.getReference());
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

        Iterable<MethodCall> eventConstructionInstances =
                Iterables.filter(methodCall.asList(), eventHandlerIdentificationStrategy.isEventPredicate());
        System.out.println(Lists.newArrayList(eventConstructionInstances));
        for (MethodCall eventConstruction : eventConstructionInstances) {
            AxonNode eventNode = new AxonNode(AxonNode.Type.EVENT, eventConstruction.reference());
            node.add(eventNode);
            for (CtMethodImpl eventHandler : eventHandlerIdentificationStrategy.findEventHandlers(eventNode.reference().getDeclaringType())) {
                AxonNode eventHandlerNode = new AxonNode(AxonNode.Type.EVENT_LISTENER, eventHandler.getReference());
                eventNode.add(eventHandlerNode);
                buildCommandFlow(eventHandlerNode);
            }
        }
        return node;
    }
}
