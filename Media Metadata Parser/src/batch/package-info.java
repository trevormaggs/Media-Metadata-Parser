/**
 * Provides the core orchestration logic for batch-processing media metadata.
 * 
 * <p>
 * The package is designed around a "Surgical" processing model, which ensures that source media is never modified in place. Instead, it facilitates a workflow of discovery, configuration, and execution:
 * </p>
 * 
 * <h2>Primary Components:</h2>
 * 
 * <ul>
 * <li><b>{@link batch.MediaMetadataConsole}:</b> The CLI entry point that parses user arguments and initializes the application lifecycle.</li>
 * <li><b>{@link batch.BatchBuilder}:</b> A fluent API used to construct an immutable {@link batch.BatchConfiguration}.</li>
 * <li><b>{@link batch.MetadataScanner}:</b> Handles the recursive discovery of media files and extracts initial metadata to create a sorted set of records.</li>
 * <li><b>{@link batch.MediaBatchProcessor}:</b> The execution engine that performs the physical file copying, renaming, and binary metadata patching.</li>
 * </ul>
 * 
 * <h2>Data Flow:</h2>
 * 
 * <ol>
 * <li>The user provides input via the Console.</li>
 * <li>A {@link batch.BatchConfiguration} is built to define the rules (prefix, date overrides, etc.).</li>
 * <li>The {@link batch.MetadataScanner} crawls the source and generates {@link batch.MediaRecord} entries.</li>
 * <li>The {@link batch.MediaBatchProcessor} consumes these records to produce the final organised output.</li>
 * </ol>
 * 
 * @author Trevor Maggs
 * @version 1.1
 * @since 5 May 2026
 */
package batch;