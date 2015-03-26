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
    private final Map<CtTypeReference, List<CtMethodImpl>> eventHandlers;
    private final Map<CtTypeReference, CtMethodImpl> commandHandlers;
    private final MethodCallHierarchyBuilder callHierarchyBuilder;

    public AxonFlowBuilder(Map<CtTypeReference, Set<CtTypeReference>> classHierarchy,
                           Map<CtExecutableReference, List<CtExecutableReference>> callList,
                           Map<CtTypeReference, List<CtMethodImpl>> eventHandlers,
                           Map<CtTypeReference, CtMethodImpl> commandHandlers) {
        this.eventHandlers = eventHandlers;
        this.commandHandlers = commandHandlers;
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
        Optional<MethodCall> commandConstruction = Iterables.tryFind(methodCall.asList(), new Predicate<MethodCall>() {
            @Override
            public boolean apply(MethodCall input) {
                return commandHandlers.keySet().contains(input.reference().getDeclaringType());
            }
        });
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

    private AxonNode buildEventFlow(AxonNode node) {
        MethodCall methodCall = this.callHierarchyBuilder.buildCallHierarchy(node.reference());

        Iterable<MethodCall> eventConstructionInstances = Iterables.filter(methodCall.asList(), new Predicate<MethodCall>() {
            @Override
            public boolean apply(MethodCall input) {
                return eventHandlers.keySet().contains(input.reference().getDeclaringType());
            }
        });
        for (MethodCall eventConstruction : eventConstructionInstances) {
            AxonNode eventNode = new AxonNode("event", eventConstruction.reference());
            node.add(eventNode);
            for (CtMethodImpl eventHandler : this.eventHandlers.get(eventNode.reference().getDeclaringType())) {
                AxonNode eventHandlerNode = new AxonNode("event listener", eventHandler.getReference());
                eventNode.add(eventHandlerNode);
                buildCommandFlow(eventHandlerNode);
            }
        }
        return node;
    }

}
