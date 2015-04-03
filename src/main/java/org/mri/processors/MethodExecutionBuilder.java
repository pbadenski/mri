package org.mri.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtAbstractInvocation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.visitor.filter.AbstractFilter;
import spoon.support.QueueProcessingManager;
import spoon.support.reflect.declaration.CtExecutableImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodExecutionBuilder {
    private Map<CtExecutableReference, List<CtExecutableReference>> callList = new HashMap<>();
    private static Logger logger = LoggerFactory.getLogger(MethodExecutionBuilder.class);

    private class Processor extends AbstractProcessor<CtExecutableImpl> {
        @Override
        public void process(CtExecutableImpl ctMethod) {
            List<CtElement> elements = ctMethod.getElements(new AbstractFilter<CtElement>(CtElement.class) {
                @Override
                public boolean matches(CtElement ctElement) {
                    return ctElement instanceof CtAbstractInvocation;
                }
            });
            List<CtExecutableReference> calls = new ArrayList<>();
            for (CtElement element : elements) {
                CtAbstractInvocation invocation = (CtAbstractInvocation) element;
                calls.add(invocation.getExecutable());

            }
            callList.put(ctMethod.getReference(), calls);
        }

    }

    public Map<CtExecutableReference, List<CtExecutableReference>> build(QueueProcessingManager queueProcessingManager) throws Exception {
        queueProcessingManager.addProcessor(new Processor());
        queueProcessingManager.process();
        logger.debug("Method calls: " + callList);
        return callList;
    }
}

