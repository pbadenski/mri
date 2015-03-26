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

}
