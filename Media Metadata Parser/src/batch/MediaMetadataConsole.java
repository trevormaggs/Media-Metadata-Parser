package batch;

import cli.CommandFlagParser;
import cli.FlagType;
import logger.LogFactory;
import util.ProjectBuildInfo;

/**
 * The primary Command Line Interface (CLI) entry point for media metadata operations.
 *
 * <p>
 * This class acts as the system orchestrator, managing the application lifecycle from initial
 * argument parsing to task dispatching. It leverages a {@link MetadataScanner} to discover media
 * and routes the execution flow based on user intent.
 * </p>
 *
 * <p>
 * Depending on the configuration, this console either provides a detailed diagnostic view of media
 * metadata via {@link DisplayMetadata} or delegates to the {@link BatchExecutor} for chronological
 * renaming and file processing.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.2
 * @since 1 May 2026
 */
public final class MediaMetadataConsole
{
    private static final LogFactory LOGGER = LogFactory.getLogger(MediaMetadataConsole.class);
    private final BatchConfiguration config;
    private final MetadataScanner scanner;

    /**
     * Constructs a console interface using a {@link BatchConfiguration} configuration.
     *
     * <p>
     * This constructor is invoked via {@link BatchBuilder#build()} to ensure all configuration
     * constraints are validated before instantiation.
     * </p>
     *
     * @param config
     *        the immutable configuration object containing the validated parameters required to
     *        execute the batch
     *
     * @throws BatchErrorException
     *         if any metadata-related reading error occurs during initialisation
     */
    public MediaMetadataConsole(BatchConfiguration config)
    {
        this.config = config;
        this.scanner = new MetadataScanner(config);
    }

    /**
     * Configures the supported command-line flags and definitions.
     *
     * <p>
     * This method defines the grammar for the CLI, including optional arguments, blank flags, and
     * separators. It ensures the raw input is structured into a {@link CommandFlagParser} before
     * being consumed by the {@link BatchBuilder}.
     * </p>
     *
     * @param arguments
     *        the raw command-line strings from the terminal
     * @return a parsed and validated CommandFlagParser
     */
    private static CommandFlagParser scanArguments(String[] arguments)
    {
        CommandFlagParser cli = new CommandFlagParser(arguments);

        try
        {
            cli.addDefinition("-p", FlagType.ARG_OPTIONAL);
            cli.addDefinition("-t", FlagType.ARG_OPTIONAL);
            cli.addDefinition("-e", FlagType.ARG_BLANK);
            cli.addDefinition("-m", FlagType.ARG_OPTIONAL);
            cli.addDefinition("-f", FlagType.ARG_BLANK);
            cli.addDefinition("-l", FlagType.SEP_OPTIONAL);
            cli.addDefinition("-k", FlagType.ARG_BLANK);
            cli.addDefinition("-s", FlagType.ARG_BLANK);
            cli.addDefinition("--desc", FlagType.ARG_BLANK);
            cli.addDefinition("-v", FlagType.ARG_BLANK);
            cli.addDefinition("--version", FlagType.ARG_BLANK);
            cli.addDefinition("-d", FlagType.ARG_BLANK);
            cli.addDefinition("--debug", FlagType.ARG_BLANK);
            cli.addDefinition("-h", FlagType.ARG_BLANK);
            cli.addDefinition("--help", FlagType.ARG_BLANK);

            cli.setFreeArgumentLimit(1);
            cli.parse();
        }

        catch (Exception exc)
        {
            System.err.println(exc.getMessage());

            showUsage();
            System.exit(1);
        }

        return cli;
    }

    /**
     * Prints the command usage line, showing the correct flag options.
     */
    private static void showUsage()
    {
        System.out.format("Usage: %s [-p prefix] [-t target directory] [-e] [-m date taken] [-f] [-l <File 1> ... <File n>] [-k] [-s] [--desc] [-v|--version] [-h|--help] [-d|--debug] <Source Directory>%n",
                ProjectBuildInfo.getInstance(MediaMetadataConsole.class).getShortFileName());
    }

    /**
     * Prints detailed usage help information, providing guidance on how to use this program.
     */
    private static void showHelp()
    {
        showUsage();
        System.out.println("\nOptions:");
        System.out.println("  -p <prefix>        Prepend copied files with user-defined prefix");
        System.out.println("  -t <directory>     Target directory where copied files are saved");
        System.out.println("  -e                 Embed date and time in copied file names");
        System.out.println("  -m <date>          Modify file's 'Date Taken' metadata properties if empty");
        System.out.println("  -f                 Force user-defined date modification regardless of metadata. -m flag must be specified");
        System.out.println("  -l <files...>      Comma-separated list of specific file names to process");
        System.out.println("  -k                 Skip media files (videos, etc)");
        System.out.println("  -s                 List detailed metadata entries");
        System.out.println("  --desc             Sort the images in descending order");
        System.out.println("  -v                 Display last build date");
        System.out.println("  -h                 Display this help message and exit");
        System.out.println("  -d                 Enable debugging");
    }

    /**
     * Begins the execution process by reading arguments from the command line and triggering the
     * configuration builder.
     * 
     * <p>
     * This method handles top-level CLI interrupts (Help/Version), maps parsed flags to the
     * {@link BatchBuilder} API, and invokes the terminal run state.
     * </p>
     *
     * @param arguments
     *        raw command-line arguments
     */
    private static void execute(String[] arguments)
    {
        CommandFlagParser cli = scanArguments(arguments);

        if (cli.existsFlag("-h") || cli.existsFlag("--help"))
        {
            showHelp();
            System.exit(0);
        }

        if (cli.existsFlag("-v") || cli.existsFlag("--version"))
        {
            System.out.printf("Build date: %s%n", ProjectBuildInfo.getInstance(MediaMetadataConsole.class).getBuildDate());
            System.exit(0);
        }

        BatchBuilder builder = new BatchBuilder()
                .source(cli.getFirstFreeArgument())
                .prefix(cli.getValueByFlag("-p"))
                .target(cli.getValueByFlag("-t"))
                .embedDateTime(cli.existsFlag("-e"))
                .userDate(cli.getValueByFlag("-m"))
                .skipVideo(cli.existsFlag("-k"))
                .showMetadata(cli.existsFlag("-s"))
                .descending(cli.existsFlag("--desc"))
                .forceDateChange(cli.existsFlag("-f"))
                .debug(cli.existsFlag("-d") || cli.existsFlag("--debug"));

        if (cli.existsFlag("-l") && cli.getValueLength("-l") > 0)
        {
            builder.fileSet(cli.getValuesByFlag("-l"));
        }

        try
        {
            builder.newBuild();
        }

        catch (BatchErrorException exc)
        {
            LOGGER.error(exc.getMessage());
            System.exit(1);
        }
    }

    /**
     * Executes the requested media operation.
     * 
     * <p>
     * This method initiates the directory scan to build a sorted set of {@link MediaRecord}
     * entries. Once discovery is complete, it routes the flow to either displaying the metadata or
     * activating the batch renaming engine based on the selected date/time attributes.
     * </p>
     * 
     * @throws BatchErrorException
     *         if the scan or subsequent task fails
     */
    public void run() throws BatchErrorException
    {
        scanner.start();

        if (config.isShowMetadata())
        {
            // DisplayMetadata.print(scanner);
            System.out.printf("%s\n", "DisplayMetadata.print(scanner)");
        }

        else
        {
            // new BatchExecutor(config, scanner).execute();
            System.out.printf("%s\n", "new BatchExecutor(config, scanner).execute()");
        }
    }

    public static void main(String[] args)
    {
        MediaMetadataConsole.execute(args);
    }
}