package org.mri;

import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;

/**
 * Hack because current version of spoon (4.1.0) has broken hashCode and equals behavior
 * for CtExecutable and CtExecutableReference (one ignores the class name containing the method
 * and another parameters of the method).
 */
public class MethodWrapper {
    private final CtTypeReference type;
    private final CtExecutable method;

    private MethodWrapper(CtTypeReference type, CtExecutable method) {
        this.type = type;
        this.method = method;
    }

    public MethodWrapper(CtExecutable executable) {
        this(executable.getReference().getDeclaringType(), executable);
    }

    public MethodWrapper(CtExecutableReference executableReference) {
        this(executableReference.getDeclaringType(), executableReference.getDeclaration());
    }

    public CtTypeReference type() {
        return type;
    }

    public CtExecutable method() {
        return method;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MethodWrapper that = (MethodWrapper) o;

        if (method != null ? !method.equals(that.method) : that.method != null) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (method != null ? method.hashCode() : 0);
        return result;
    }
}
