package batch;

import cli.CommandFlagParser;
import cli.FlagType;
import progressbar.ConsoleProgressBar;
import util.ProjectBuildInfo;

/**
 * The primary Command Line Interface (CLI) entry point for media metadata operations.
 *
 * @author Trevor Maggs
 * @version 1.2
 * @since 2 June 2026
 */
public final class MediaMetadataConsoleTest
{
    private final BatchConfiguration config;
    private MediaBatchProcessor activeProcessor;

    public MediaMetadataConsoleTest(BatchConfiguration config)
    {
        this.config = config;
    }

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
            cli.addDefinition("-i", FlagType.SEP_OPTIONAL);
            cli.addDefinition("-S", FlagType.ARG_BLANK);
            cli.addDefinition("-X", FlagType.ARG_BLANK);
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

    private static void showUsage()
    {
        System.out.format("Usage: %s [-p prefix] [-t target directory] [-e] [-m date taken] [-f] [-i=<File 1> ... <File n>] [-S] [-X] [--desc] [-v|--version] [-h|--help] [-d|--debug] <Source Directory>%n",
                ProjectBuildInfo.getInstance(MediaMetadataConsole.class).getShortFileName());
    }

    private static void showHelp()
    {
        showUsage();
        System.out.println("\nOptions:");
        System.out.println("  -p <prefix>        Prepend copied files with user-defined prefix");
        System.out.println("  -t <directory>     Target directory where copied files are saved. Default is '" + MediaBatchProcessor.DEFAULT_TARGET_DIRECTORY + "'");
        System.out.println("  -e                 Embed date and time in copied file names");
        System.out.println("  -m <date>          Modify file's 'Date Taken' metadata properties if empty");
        System.out.println("  -f                 Force user-defined date modification regardless of metadata. -m flag must be specified");
        System.out.println("  -i=<files...>      Includes comma-separated list of specific file names to process");
        System.out.println("  -S                 Skip other media files");
        System.out.println("  -X                 Display detailed metadata entries similar to 'exiftool -G1 -a -s -u'");
        System.out.println("  --desc             Sort the images in descending order");
        System.out.println("  -v                 Display last build date");
        System.out.println("  -h                 Display this help message and exit");
        System.out.println("  -d                 Enable debugging");
    }

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
                .skipVideo(cli.existsFlag("-S"))
                .showMetadata(cli.existsFlag("-X"))
                .descending(cli.existsFlag("--desc"))
                .forceDateChange(cli.existsFlag("-f"))
                .debug(cli.existsFlag("-d") || cli.existsFlag("--debug"));

        if (cli.existsFlag("-i") && cli.getValueLength("-i") > 0)
        {
            builder.fileSet(cli.getValuesByFlag("-i"));
        }

        try
        {
            BatchConfiguration config = builder.build();
            MediaMetadataConsole console = new MediaMetadataConsole(config);
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
     * Registers a JVM shutdown hook to capture SIGINT (Ctrl+C) and trigger cancellation.
     *
     * @throws BatchErrorException if scanning or subsequent processing fails
     */
    public void run() throws BatchErrorException
    {
        // 1. Setup the Shutdown Hook to handle Ctrl+C
        Thread shutdownHook = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                if (activeProcessor != null && !activeProcessor.isCancelled())
                {
                    System.out.println("\n[INFO] Interrupt signal received (Ctrl+C). Cancelling process...");
                    activeProcessor.cancel();
                }
            }
        });

        Runtime.getRuntime().addShutdownHook(shutdownHook);

        try
        {
            if (config.isShowMetadata())
            {
                DisplayMetadata display = new DisplayMetadata(config);
                display.execute();
            }
            else
            {
                activeProcessor = new MediaBatchProcessor(config);
                activeProcessor.addProgressListener(new ConsoleProgressBar());
                activeProcessor.execute();

                if (activeProcessor.isCancelled())
                {
                    System.out.println("\nProcess cancelled.");
                }
                else
                {
                    System.out.println("\nDone");
                }
            }
        }
        finally
        {
            // 2. Safely remove shutdown hook on normal completion
            try
            {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            }
            catch (IllegalStateException ignored)
            {
                // Thrown if the JVM is already shutting down
            }
        }
    }

    public static void main(String[] args)
    {
        //MediaMetadataConsole.execute(args);
    }
}