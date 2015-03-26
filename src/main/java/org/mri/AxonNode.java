package org.mri;

import spoon.reflect.reference.CtExecutableReference;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class AxonNode {
    private final String type;
    private final CtExecutableReference reference;
    private List<AxonNode> children = new ArrayList<>();

    public AxonNode(String type, CtExecutableReference reference) {
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
        printStream.println("@startuml");
        printPlantUMLComponent(printStream);
        printStream.println("@enduml");
    }

    private void printPlantUMLComponent(PrintStream printStream) {
        for (AxonNode child : children) {
            printStream.println(
                    this.reference().getDeclaringType().getSimpleName()
                            + " "
                            + transition(child)
                            + " "
                            + child.reference().getDeclaringType().getSimpleName()
                            + ": "
                            + methodName(child));
            child.printPlantUMLComponent(printStream);
        }
    }

    private String transition(AxonNode child) {
        switch (type) {
            case "controller":
            case "command handler":
            case "event listener":
                return "->";
            case "command":
            case "event":
                return "-->";
        }
        return "->";
    }

    private String methodName(AxonNode child) {
        switch (type) {
            case "controller":
            case "command handler":
            case "event listener":
                return "create";
            case "command":
            case "event":
                return child.reference.getSimpleName();
        }
        return "<<call>>";
    }
}
