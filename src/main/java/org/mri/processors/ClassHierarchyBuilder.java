package org.mri.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.processing.AbstractProcessor;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.QueueProcessingManager;
import spoon.support.reflect.declaration.CtClassImpl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClassHierarchyBuilder {
    private final static Logger logger = LoggerFactory.getLogger(ClassHierarchyBuilder.class);

    private final Map<CtTypeReference, Set<CtTypeReference>> implementors = new HashMap<>();

    private class Processor extends AbstractProcessor<CtClassImpl> {
        private void reportInheritance(CtTypeReference clazz, CtTypeReference superClass) {
            Set<CtTypeReference> subclasses = implementors.get(superClass);
            if (subclasses == null) {
                subclasses = new HashSet<>();
                implementors.put(superClass, subclasses);
            }
            subclasses.add(clazz);
        }

        @Override
        public void process(CtClassImpl clazz) {
            if (clazz.getReference().isAnonymous()) {
                return;
            }
            if (clazz.getSuperclass() != null) {
                reportInheritance(clazz.getReference(), clazz.getSuperclass());
            }
            for (Object o : clazz.getSuperInterfaces()) {
                CtTypeReference superclass = (CtTypeReference) o;
                reportInheritance(clazz.getReference(), superclass);
            }
        }
    }

    public Map<CtTypeReference, java.util.Set<CtTypeReference>> build(QueueProcessingManager queueProcessingManager) throws Exception {
        queueProcessingManager.addProcessor(new Processor());
        queueProcessingManager.process();
        logger.debug("Class Hierarchy: " + implementors);
        return implementors;
    }
}
