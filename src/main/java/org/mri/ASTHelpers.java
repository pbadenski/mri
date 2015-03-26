package org.mri;

import spoon.reflect.reference.CtExecutableReference;

public class ASTHelpers {
    public static String signatureOf(CtExecutableReference executableReference) {
        return executableReference.getDeclaringType().getQualifiedName() + "." + executableReference.getSimpleName();
    }
}
