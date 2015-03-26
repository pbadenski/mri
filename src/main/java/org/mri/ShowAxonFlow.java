package org.mri;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.spi.StringArrayOptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.Launcher;
import spoon.compiler.ModelBuildingException;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.QueueProcessingManager;
import spoon.support.compiler.FileSystemFolder;
import spoon.support.reflect.declaration.CtMethodImpl;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ShowAxonFlow {
    public static final String AXON_EVENT_HANDLER = "@org.axonframework.eventhandling.annotation.EventHandler";
    public static final String AXON_COMMAND_HANDLER = "@org.axonframework.commandhandling.annotation.CommandHandler";

    private static Logger logger = LoggerFactory.getLogger(ShowAxonFlow.class);

    @Option(name="-s", aliases = "--source-folder", metaVar = "SOURCE_FOLDERS",
            usage="source folder(s) for the analyzed project",
            handler = StringArrayOptionHandler.class,
            required = true)
    private List<String> sourceFolders;

    @Option(name="-m", aliases = "--method-name", metaVar = "METHOD_NAME",
            usage="method name to print call hierarchy",
            required = true)
    private String methodName;

    @Option(name="-c", aliases = "--classpath",  metaVar = "CLASSPATH",
            usage="classpath for the analyzed project")
    private String classpath;

    @Option(name="--classpath-file", metaVar = "CLASSPATH_FILE", usage="file containing the classpath for the analyzed project",
            forbids = "--classpath")
    private File classpathFile;
    private PrintStream printStream;

    public static void main(String[] args) throws Exception {
        ShowAxonFlow.parse(args).doMain();
    }

    private static ShowAxonFlow parse(String[] args) {
        ShowAxonFlow showAxonFlow = new ShowAxonFlow(System.out);
        CmdLineParser parser = new CmdLineParser(showAxonFlow);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            System.err.print("Usage: java -jar <CHP_JAR_PATH>" + parser.printExample(OptionHandlerFilter.REQUIRED));
            System.err.println();
            System.err.println();
            System.err.println("Options:");
            parser.printUsage(System.err);
            System.exit(1);
        }
        return showAxonFlow;
    }

    public ShowAxonFlow(PrintStream printStream) {
        this.printStream = printStream;
    }

    public ShowAxonFlow(String classpath, List<String> sourceFolders, String methodName, PrintStream printStream) {
        this(printStream);
        this.sourceFolders = sourceFolders;
        this.methodName = methodName;
        this.classpath = classpath;
    }

    public void doMain() throws Exception {
        Launcher launcher = new Launcher();
        if (classpath != null) {
            launcher.setArgs(new String[] { "--source-classpath", classpath});
        }
        if (classpathFile != null) {
            launcher.setArgs(new String[] { "--source-classpath", StringUtils.strip(FileUtils.readFileToString(classpathFile), "\n\r\t ")});
        }
        for (String sourceFolder : sourceFolders) {
            launcher.addInputResource(new FileSystemFolder(new File(sourceFolder)));
        }
        try {
            launcher.run();
        } catch (ModelBuildingException e) {
            throw new RuntimeException("You most likely have not specified your classpath. Pass it in using either '--claspath' or '--classpath-file'.", e);
        }

        printCallHierarchy(launcher, printStream);
    }

    private void printCallHierarchy(Launcher launcher, PrintStream printStream) throws Exception {
        QueueProcessingManager queueProcessingManager = new QueueProcessingManager(launcher.getFactory());
        Map<CtTypeReference, Set<CtTypeReference>> classHierarchy =
                new ClassHierarchyProcessor().executeSpoon(queueProcessingManager);
        Map<CtExecutableReference, List<CtExecutableReference>> callList =
                new MethodExecutionProcessor().executeSpoon(queueProcessingManager);
        final Map<CtTypeReference, List<CtMethodImpl>> eventHandlers =
                new AnnotatedEventHandlers(AXON_EVENT_HANDLER).executeSpoon(queueProcessingManager);
        final Map<CtTypeReference, CtMethodImpl> commandHandlers =
                new AnnotatedCommandHandlers(AXON_COMMAND_HANDLER).executeSpoon(queueProcessingManager);

        List<MethodCallHierarchyBuilder> methodCallHierarchyBuilders = MethodCallHierarchyBuilder.forMethodName(methodName, callList, classHierarchy);
        if (methodCallHierarchyBuilders.isEmpty()) {
            printStream.println("No method containing `" + methodName + "` found.");
        }
        if (methodCallHierarchyBuilders.size() > 1) {
            printStream.println("Found " + methodCallHierarchyBuilders.size() + " matching methods...");
            printStream.println();
        }
        for (MethodCallHierarchyBuilder each : methodCallHierarchyBuilders) {
            MethodCall methodCall = each.buildCallHierarchy();
            System.out.println(methodCall.reference().toString());
            CtMethodImpl commandHandler = forCommandCaller(commandHandlers, methodCall, "\t");
            forEvent(classHierarchy, callList, eventHandlers, commandHandler, commandHandlers, "\t\t");
        }
    }

    private CtMethodImpl forCommandCaller(final Map<CtTypeReference, CtMethodImpl> allCommandHandlers, MethodCall each, String indent) {
        Optional<MethodCall> commandConstruction = Iterables.tryFind(each.asList(), new Predicate<MethodCall>() {
            @Override
            public boolean apply(MethodCall input) {
                return allCommandHandlers.keySet().contains(input.reference().getDeclaringType());
            }
        });
        if (!commandConstruction.isPresent()) {
            return null;
        }
        CtMethodImpl commandHandler = allCommandHandlers.get(commandConstruction.get().reference().getDeclaringType());
        System.out.println(indent + "-> " + commandConstruction.get().reference().toString());
        System.out.println(indent + "-- [handler] --");
        System.out.println(indent + "  -> " + commandHandler.getReference().toString());
        return commandHandler;
    }

    private List<CtMethodImpl> forEvent(Map<CtTypeReference, Set<CtTypeReference>> classHierarchy, Map<CtExecutableReference, List<CtExecutableReference>> callList, final Map<CtTypeReference, List<CtMethodImpl>> allEventHandlers, CtMethodImpl commandHandler, Map<CtTypeReference, CtMethodImpl> commandHandlers, String indent) {
        MethodCallHierarchyBuilder commandHandlerCall = new MethodCallHierarchyBuilder(commandHandler.getReference(), callList, classHierarchy);
        MethodCall secondMethodCall = commandHandlerCall.buildCallHierarchy();

        Iterable<MethodCall> eventConstructionInstances = Iterables.filter(secondMethodCall.asList(), new Predicate<MethodCall>() {
            @Override
            public boolean apply(MethodCall input) {
                return allEventHandlers.keySet().contains(input.reference().getDeclaringType());
            }
        });
        List<CtMethodImpl> eventHandlers = new ArrayList<>();
        for (MethodCall eventConstruction : eventConstructionInstances) {
            System.out.println(indent + "-> " + eventConstruction.reference().toString());
            System.out.println(indent + "-- [listeners] --");
            for (CtMethodImpl eventHandler : allEventHandlers.get(eventConstruction.reference().getDeclaringType())) {
                System.out.println(indent + "  -> " + eventHandler.getReference().toString());
                eventHandlers.add(eventHandler);
                forCommandCaller(
                        commandHandlers,
                        new MethodCallHierarchyBuilder(eventHandler.getReference(), callList, classHierarchy).buildCallHierarchy(), "\t\t\t");
                System.out.println();
            }
        }
        return eventHandlers;
    }
}
