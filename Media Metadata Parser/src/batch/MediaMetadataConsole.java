package batch;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import cli.CommandFlagParser;
import cli.FlagType;
import logger.LogFactory;
import progressbar.ConsoleProgressBar;
import util.ProjectBuildInfo;

/**
 * The primary Command Line Interface (CLI) entry point for media metadata operations.
 *
 * <p>
 * This class coordinates the application lifecycle from initial argument parsing to task
 * dispatching. It leverages a {@link MetadataScanner} to discover media and routes the execution
 * flow based on user intent.
 * </p>
 *
 * <p>
 * Depending on the configuration, this console either provides a detailed view of media metadata or
 * delegates to the {@link MediaBatchProcessor} for chronological renaming and file processing.
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
     * Constructs a console interface using a validated configuration.
     *
     * <p>
     * This constructor is typically invoked via {@link BatchBuilder#build()} to ensure all
     * configuration constraints and path validations are satisfied before instantiation.
     * </p>
     *
     * @param config
     *        the immutable configuration object containing the validated parameters
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
     * This method establishes the validation rules for the CLI, defining which flags require
     * values, which act as switches, and how separators should be handled.
     * </p>
     *
     * @param arguments
     *        the raw command-line strings provided at runtime
     * @return a configured {@link CommandFlagParser} ready for interrogation
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
     * Begins the execution process by reading arguments from the command line and initialising the
     * configuration builder.
     *
     * <p>
     * This method handles the mapping of parsed flags to the {@link BatchBuilder} API and invokes
     * the terminal run state.
     * </p>
     *
     * @param arguments
     *        the raw command-line arguments provided at runtime
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
            LOGGER.error(exc.getMessage());
            System.exit(1);
        }
    }

    /**
     * Determines the number of supported image files within a specific directory.
     *
     * <p>
     * This method provides a "Pass 1" count using a high-performance {@link DirectoryStream}
     * filtered by common image extensions (.jpg, .png, .heic, .webp).
     * </p>
     *
     * @param dir
     *        the directory path to analyse
     * @return the count of files matching the image criteria
     * @throws IOException
     *         if the directory is inaccessible
     */
    @SuppressWarnings("unused")
    private int countImages(Path dir) throws IOException
    {
        int count = 0;

        DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>()
        {
            @Override
            public boolean accept(Path entry) throws IOException
            {
                String name = entry.getFileName().toString().toLowerCase();

                return name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".heic") || name.endsWith(".webp");
            }
        };

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, filter))
        {
            Iterator<Path> iter = stream.iterator();

            while (iter.hasNext())
            {
                iter.next();
                count++;
            }
        }

        return count;
    }

    /**
     * Facilitates the media operation defined by the user configuration.
     *
     * <p>
     * The execution flow follows a two-stage process:
     * </p>
     *
     * <ol>
     * <li><b>Discovery:</b> The {@link MetadataScanner} traverses the source to build a sorted set
     * of media records.</li>
     * <li><b>Execution:</b> Depending on configuration, the system either lists extracted metadata
     * for inspection or initiates a {@link MediaBatchProcessor} to perform file operations.</li>
     * </ol>
     *
     * @throws BatchErrorException
     *         if the scan or subsequent task fails
     */
    public void run() throws BatchErrorException
    {
        scanner.start();

        int total = scanner.getRecordCount();

        if (config.isShowMetadata())
        {
            DisplayMetadata display = new DisplayMetadata(scanner);

            display.execute();
        }

        else if (total > 0)
        {
            MediaBatchProcessor processor = new MediaBatchProcessor(config, scanner);

            processor.addProgressListener(new ConsoleProgressBar(0, total));
            processor.execute();

            System.out.println("Done");
        }

        else
        {
            System.out.println("No valid media files found in [" + config.getSource() + "]");
        }
    }

    /**
     * Entry point for the application.
     *
     * @param args
     *        command-line arguments provided at runtime
     */
    public static void main(String[] args)
    {
        MediaMetadataConsole.execute(args);
    }
}