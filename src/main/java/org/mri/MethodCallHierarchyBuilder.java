package org.mri;

import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;

import java.util.*;

public class MethodCallHierarchyBuilder {
    private final CtExecutableReference executableReference;
    private final Map<CtExecutableReference, List<CtExecutableReference>> callList;
    private final Map<CtTypeReference, Set<CtTypeReference>> classHierarchy;

    public MethodCallHierarchyBuilder(CtExecutableReference executableReference,
                                       Map<CtExecutableReference, List<CtExecutableReference>> callList,
                                       Map<CtTypeReference, Set<CtTypeReference>> classHierarchy) {
        this.executableReference = executableReference;
        this.callList = callList;
        this.classHierarchy = classHierarchy;
    }

    public static List<MethodCallHierarchyBuilder> forMethodName(String methodName,
                                                           Map<CtExecutableReference, List<CtExecutableReference>> callList,
                                                           Map<CtTypeReference, Set<CtTypeReference>> classHierarchy) {
        ArrayList<MethodCallHierarchyBuilder> result = new ArrayList<>();
        for (CtExecutableReference executableReference : findExecutablesForMethodName(methodName, callList)) {
            result.add(new MethodCallHierarchyBuilder(executableReference, callList, classHierarchy));
        }
        return result;
    }

    static List<CtExecutableReference> findExecutablesForMethodName(String methodName, Map<CtExecutableReference, List<CtExecutableReference>> callList) {
        ArrayList<CtExecutableReference> result = new ArrayList<>();
        for (CtExecutableReference executableReference : callList.keySet()) {
            String executableReferenceMethodName = ASTHelpers.signatureOf(executableReference);
            if (executableReferenceMethodName.equals(methodName)
                    || executableReference.toString().contains(methodName)
                    || executableReference.toString().matches(methodName)) {
                result.add(executableReference);
            }
        }
        return result;
    }

    public MethodCall buildCallHierarchy() {
        MethodCall methodCall = new MethodCall(executableReference);
        buildCallHierarchy(executableReference, new HashSet<CtExecutableReference>(), methodCall);
        return methodCall;
    }

    private void buildCallHierarchy(
            CtExecutableReference method, Set<CtExecutableReference> alreadyVisited, MethodCall methodCall) {
        if (alreadyVisited.contains(method)) {
            return;
        }
        alreadyVisited.add(method);
        List<CtExecutableReference> callListForMethod = callList.get(method);
        if (callListForMethod == null) {
            return;
        }
        for (CtExecutableReference eachReference : callListForMethod) {
            MethodCall childCall = new MethodCall(eachReference);
            methodCall.add(childCall);

            buildCallHierarchy(eachReference, alreadyVisited, childCall);
            Set<CtTypeReference> subclasses = classHierarchy.get(eachReference.getDeclaringType());
            if (subclasses != null) {
                for (CtTypeReference subclass : subclasses) {
                    CtExecutableReference reference = eachReference.getOverridingExecutable(subclass);
                    if (reference != null) {
                        childCall = new MethodCall(reference);
                        methodCall.add(childCall);
                        buildCallHierarchy(reference, alreadyVisited, childCall);
                    }
                }
            }
        }
    }
}
