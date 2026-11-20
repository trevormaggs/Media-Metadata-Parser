package common; // or common.binary depending on your package structure

import java.nio.ByteOrder;
import java.util.Arrays;

public class ByteConverterBenchmark
{
    private static final int WARMUP_ITERATIONS = 50_000;
    private static final int BENCHMARK_ITERATIONS = 500_000;

    public static void main(String[] args)
    {
        // 1. Prepare sample data (e.g., 1,024 bytes containing test values)
        byte[] data = new byte[1024];
        for (int i = 0; i < data.length; i++)
        {
            data[i] = (byte) (i % 256);
        }

        ByteOrder order = ByteOrder.BIG_ENDIAN;

        System.out.println("=== Starting Byte Converter Performance Benchmark ===");
        System.out.println("Iterations per test: " + BENCHMARK_ITERATIONS);
        System.out.println();

        // ==========================================
        // WARM-UP PHASE (Forces JIT compilation)
        // ==========================================
        System.out.println("Warming up JVM...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++)
        {
            int offset = (i % 600);
            ByteValueConverter.toInteger(data, offset, order);
            ByteValueConverterBB.toInteger(data, offset, order);
            ByteValueConverter.toIntegerArray(data, 0, order);
            ByteValueConverterBB.toIntegerArray(data, 0, order);
        }
        System.out.println("Warm-up complete.\n");

        // ==========================================
        // TEST 1: Scalar Parsing (toInteger)
        // ==========================================
        long startOriginalScalar = System.nanoTime();
        int dummy1 = 0;
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++)
        {
            int offset = (i % 600);
            dummy1 += ByteValueConverter.toInteger(data, offset, order);
        }
        long durationOriginalScalar = System.nanoTime() - startOriginalScalar;

        long startBB際のScalar = System.nanoTime();
        int dummy2 = 0;
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++)
        {
            int offset = (i % 600);
            dummy2 += ByteValueConverterBB.toInteger(data, offset, order);
        }
        long durationBBScalar = System.nanoTime() - startBB際のScalar;

        // ==========================================
        // TEST 2: Array Parsing (toIntegerArray)
        // ==========================================
        // Use a clean 400-byte block (multiple of 4)
        byte[] arrayData = Arrays.copyOf(data, 400);

        long startOriginalArray = System.nanoTime();
        int[] result1 = null;
        for (int i = 0; i < BENCHMARK_ITERATIONS / 10; i++) // fewer iterations since arrays take more work
        {
            result1 = ByteValueConverter.toIntegerArray(arrayData, 0, order);
        }
        long durationOriginalArray = System.nanoTime() - startOriginalArray;

        long startBBArray = System.nanoTime();
        int[] result2 = null;
        for (int i = 0; i < BENCHMARK_ITERATIONS / 10; i++)
        {
            result2 = ByteValueConverterBB.toIntegerArray(arrayData, 0, order);
        }
        long durationBBArray = System.nanoTime() - startBBArray;

        // ==========================================
        // RESULTS OUTPUT
        // ==========================================
        System.out.println("--- Results ---");
        System.out.printf("1. Scalar 'toInteger()' (%d calls):\n", BENCHMARK_ITERATIONS);
        System.out.printf("   - Original (Bit-shift): %.2f ms\n", durationOriginalScalar / 1_000_000.0);
        System.out.printf("   - ByteBuffer-backed:     %.2f ms\n", durationBBScalar / 1_000_000.0);

        System.out.printf("\n2. Array 'toIntegerArray()' (%d calls):\n", BENCHMARK_ITERATIONS / 10);
        System.out.printf("   - Original (Bit-shift): %.2f ms\n", durationOriginalArray / 1_000_000.0);
        System.out.printf("   - ByteBuffer-backed:     %.2f ms\n", durationBBArray / 1_000_000.0);

        // Prevent JVM from optimizing away dummy results
        if (dummy1 == 999999 && result1 == null && dummy2 == 99999 && result2 == null) {
            System.out.println();
        }
    }
}