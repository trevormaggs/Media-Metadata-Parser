package batch;

import cli.CommandFlagParser;
import cli.FlagType;
import util.ProjectBuildInfo;

/**
 * The primary Command Line Interface (CLI) entry point for media metadata operations.
 *
 * <p>
 * This class coordinates the application lifecycle from argument parsing through task dispatching.
 * It leverages a {@link MetadataScanner} to discover media files and routes execution based on the
 * supplied command-line options.
 * </p>
 *
 * <p>
 * Depending on the configuration, this console either displays detailed media metadata or delegates
 * to the {@link MediaBatchProcessor} for file renaming and batch processing.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 2 June 2026
 */
public final class MediaMetadataConsole
{
    // private static final LogFactory LOGGER = LogFactory.getLogger(MediaMetadataConsole.class);
    private final BatchConfiguration config;

    /**
     * Constructs a console instance using a validated configuration.
     *
     * <p>
     * This constructor is typically invoked through {@link BatchBuilder#build()} to ensure that all
     * configuration constraints and path validations have been met.
     * </p>
     *
     * @param config
     *        the immutable configuration containing validated parameters
     */
    public MediaMetadataConsole(BatchConfiguration config)
    {
        this.config = config;
    }

    /**
     * Configures the supported command-line flags and parses the arguments provided.
     *
     * <p>
     * This method establishes the validation rules for the CLI, defining which flags require
     * values, which act as switches, and how separator tokens should be handled.
     * </p>
     *
     * @param arguments
     *        the raw command-line arguments
     *
     * @return a configured {@link CommandFlagParser}
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
     * Prints the command usage synopsis.
     */
    private static void showUsage()
    {
        System.out.format("Usage: %s [-p prefix] [-t target directory] [-e] [-m date taken] [-f] [-l <File 1> ... <File n>] [-k] [-s] [--desc] [-v|--version] [-h|--help] [-d|--debug] <Source Directory>%n",
                ProjectBuildInfo.getInstance(MediaMetadataConsole.class).getShortFileName());
    }

    /**
     * Prints detailed help information describing the available command-line options.
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
     * Begins the parsing of the command-line arguments and initialises the configuration builder.
     *
     * <p>
     * This method maps parsed flags to the {@link BatchBuilder} API, validates the resulting
     * configuration, and executes the requested operation.
     * </p>
     *
     * @param arguments
     *        the raw command-line arguments
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
            MediaMetadataConsole console = builder.build();
            console.run();
        }

        catch (BatchErrorException exc)
        {
            System.err.println(exc.getMessage());
            System.exit(1);
        }
    }

    /**
     * Executes the operation defined by the current configuration.
     *
     * <p>
     * The execution flow follows a two-stage process:
     * </p>
     *
     * <ol>
     * <li><b>Discovery:</b> The {@link MetadataScanner} traverses the source to build a sorted set
     * of media records.</li>
     * <li><b>Execution:</b> Depending on the configuration, the system either lists extracted
     * metadata for inspection or initiates a {@link MediaBatchProcessor} to perform file
     * operations.</li>
     * </ol>
     *
     * @throws BatchErrorException
     *         if scanning or subsequent processing fails
     */
    public void run() throws BatchErrorException
    {
        if (config.isShowMetadata())
        {
            DisplayMetadata display = new DisplayMetadata(config);
            display.execute();
        }

        else
        {
            MediaBatchProcessor processor = new MediaBatchProcessor(config);
            processor.execute();

            System.out.println("Done");
        }
    }

    /**
     * Entry point for the application.
     *
     * @param args
     *        the command-line arguments provided at runtime
     */
    public static void main(String[] args)
    {
        MediaMetadataConsole.execute(args);
    }
}