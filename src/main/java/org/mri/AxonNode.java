package org.mri;

import spoon.reflect.reference.CtExecutableReference;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.LOWER_HYPHEN;

public class AxonNode {
    enum Type {
        CONTROLLER, COMMAND, COMMAND_HANDLER, EVENT, EVENT_LISTENER;
    }

    private final Type type;
    private final CtExecutableReference reference;
    private List<AxonNode> children = new ArrayList<>();

    public AxonNode(Type type, CtExecutableReference reference) {
        this.type = type;
        this.reference = reference;
    }

    public void add(AxonNode node) {
        this.children.add(node);
    }

    public CtExecutableReference reference() {
        return reference;
    }

    public void print(PrintStream printStream) {
        if (children.isEmpty()) {
            return;
        }

        print(printStream, "");
    }

    private void print(PrintStream printStream, String indent) {
        printStream.println(indent + "<<" + type + ">>");
        printStream.println(indent + reference.toString());
        for (AxonNode node : children) {
            node.print(printStream, indent.concat("\t"));
        }
    }

    public void printPlantUML(PrintStream printStream) {
        if (children.isEmpty()) {
            return;
        }

        printStream.println("@startuml " + LOWER_CAMEL.to(LOWER_HYPHEN, reference.getSimpleName()) + "-flow.png");
        printPlantUMLComponent(printStream);
        printStream.println("@enduml");
    }

    private void printPlantUMLComponent(PrintStream printStream) {
        for (AxonNode child : children) {
            printStream.println(
                    "\"" + actorName(this.reference()) + "\""
                            + " "
                            + transition()
                            + " "
                            + "\"" + actorName(child.reference()) + "\""
                            + ": "
                            + methodName(child));
            child.printPlantUMLComponent(printStream);
        }
    }

    private String actorName(CtExecutableReference reference) {
        return
                reference.getDeclaringType().getSimpleName()
                        + "#" + reference.getSimpleName();
    }

    private String transition() {
        switch (type) {
            case CONTROLLER:
            case COMMAND_HANDLER:
            case EVENT_LISTENER:
                return "->";
            case COMMAND:
            case EVENT:
                return "-->";
        }
        return "->";
    }

    private String methodName(AxonNode child) {
        switch (type) {
            case CONTROLLER:
            case COMMAND_HANDLER:
            case EVENT_LISTENER:
                return "create";
            case COMMAND:
            case EVENT:
                return child.reference.getSimpleName();
        }
        return "<<call>>";
    }
}
