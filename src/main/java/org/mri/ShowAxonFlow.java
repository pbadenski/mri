package org.mri;

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
    enum Format {
        DEFAULT, PLANTUML;
    }
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

    @Option(name="-f", aliases = "--format", metaVar = "FORMAT",
            usage="format of the output")
    private Format format = Format.DEFAULT;

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

        ArrayList<CtExecutableReference> methodReferences = MethodCallHierarchyBuilder.forMethodName(methodName, callList, classHierarchy);
        if (methodReferences.isEmpty()) {
            printStream.println("No method containing `" + methodName + "` found.");
        }
        if (methodReferences.size() > 1) {
            printStream.println("Found " + methodReferences.size() + " matching methods...");
            printStream.println();
        }
        AxonFlowBuilder axonFlowBuilder = new AxonFlowBuilder(classHierarchy, callList, eventHandlers, commandHandlers);
        List<AxonNode> axonNodes = axonFlowBuilder.buildFlow(methodReferences);
        for (AxonNode axonNode : axonNodes) {
            switch (format) {
                case PLANTUML:
                    axonNode.printPlantUML(printStream);
                    break;
                default:
                    axonNode.print(printStream);
                    break;
            }
        }
    }

}
