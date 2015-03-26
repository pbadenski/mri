package org.mri;

import spoon.reflect.reference.CtExecutableReference;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class MethodCall {
    private final CtExecutableReference reference;
    private List<MethodCall> calls = new ArrayList<>();

    public MethodCall(CtExecutableReference reference) {
        this.reference = reference;
    }

    public void add(MethodCall methodCall) {
        calls.add(methodCall);
    }

    public void print(PrintStream printStream) {
        printStream.println("Method call hierarchy callees of " + reference + "");
        print(printStream, "");
    }

    private void print(PrintStream printStream, String indent) {
        printStream.println(indent + reference.toString());
        for (MethodCall call : calls) {
            call.print(printStream, indent.concat("\t"));
        }
    }

    public List<MethodCall> asList() {
        ArrayList<MethodCall> result = new ArrayList<>();
        collectRecursively(result);
        return result;
    }

    private void collectRecursively(List<MethodCall> accumulator) {
        accumulator.add(this);
        for (MethodCall call : calls) {
            call.collectRecursively(accumulator);
        }
    }

    public CtExecutableReference reference() {
        return reference;
    }
}
