package jpg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class JpgParserTest
{
    public void testExtendedXmpReconstruction() throws IOException
    {
        // 1. Setup Mock Standard Baseline XMP strictly using the xmpNote:HasExtendedXMP format and Section A
        String targetGuid = "ABCDEF1234567890ABCDEF1234567890";
        String mockStandardXml = "\n" +
                "<x:xmpmeta xmlns:x=\"adobe:ns:meta/\">\n" +
                " <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
                "   \n" +
                "   \n" +
                "   <rdf:Description rdf:about=\"\"\n" +
                "       xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
                "       xmlns:xmpNote=\"http://ns.adobe.com/xmp/note/\"\n" +
                "       xmpNote:HasExtendedXMP=\"" + targetGuid + "\">\n" +
                "     <dc:creator>John Doe</dc:creator>\n" +
                "   </rdf:Description>\n";
        byte[] standardXmpBytes = mockStandardXml.getBytes(StandardCharsets.UTF_8);

        // 2. Create raw mock data segments to split up matching Section B
        String chunk1Text = "   \n" +
                "   \n" +
                "   <rdf:Description rdf:about=\"\"\n" +
                "       xmlns:drone=\"http://ns.adobe.com/drone-metadata/1.0/\"\n" +
                "       xmlns:camera=\"http://ns.adobe.com/camera-depth/1.0/\">\n" +
                "     <drone:FlightRollDegree>+12.45</drone:FlightRollDegree>\n";
        String chunk2Text = "     <camera:DepthMapData>mQENBF2...</camera:DepthMapData>\n" +
                "   </rdf:Description>\n" +
                " </rdf:RDF>\n" +
                "</x:xmpmeta>";

        byte[] chunk1Data = chunk1Text.getBytes(StandardCharsets.UTF_8);
        byte[] chunk2Data = chunk2Text.getBytes(StandardCharsets.UTF_8);
        int totalExtendedLength = chunk1Data.length + chunk2Data.length;

        // 3. Pack fake segments out of order (Chunk 2 added before Chunk 1)
        byte[] segmentB = createMockSegment(targetGuid, totalExtendedLength, chunk1Data.length, chunk2Data);
        byte[] segmentA = createMockSegment(targetGuid, totalExtendedLength, 0, chunk1Data);

        List<byte[]> segments = new ArrayList<>();
        segments.add(segmentB); // Offset (Part 2) added first
        segments.add(segmentA); // Offset 0  (Part 1) added second

        // 4. Instantiate parser with dummy file path and run target method
        JpgParser parser = new JpgParser("mockFilePath.jpg");
        byte[] result = parser.reconstructExtendedXmpSegments(standardXmpBytes, segments);

        // 5. Native validation checks
        if (result == null) {
            throw new RuntimeException("Assertion Failed: Resulting byte array is null!");
        }
        
        String finalOutput = new String(result, StandardCharsets.UTF_8);

        if (!finalOutput.contains(mockStandardXml)) {
            throw new RuntimeException("Assertion Failed: Missing Standard XMP header");
        }
        if (!finalOutput.contains("<dc:creator>John Doe</dc:creator>")) {
            throw new RuntimeException("Assertion Failed: Section A content missing");
        }
        if (!finalOutput.contains("<drone:FlightRollDegree>+12.45</drone:FlightRollDegree>")) {
            throw new RuntimeException("Assertion Failed: Custom drone attributes missing or corrupted");
        }
        if (!finalOutput.contains("<camera:DepthMapData>mQENBF2...</camera:DepthMapData>")) {
            throw new RuntimeException("Assertion Failed: Out-of-order custom domain chunks were not stitched cleanly");
        }
    }

    /**
     * Helper to pack raw binary segments mirroring the spec:
     * [32 Bytes GUID] + [4 Bytes Total Length] + [4 Bytes Offset] + [Payload Data]
     */
    private byte[] createMockSegment(String guid, int totalLen, int offset, byte[] data) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(guid.getBytes(StandardCharsets.UTF_8));

        // 4-Byte Big-Endian Total Length
        baos.write((totalLen >>> 24) & 0xFF);
        baos.write((totalLen >>> 16) & 0xFF);
        baos.write((totalLen >>> 8) & 0xFF);
        baos.write(totalLen & 0xFF);

        // 4-Byte Big-Endian Offset
        baos.write((offset >>> 24) & 0xFF);
        baos.write((offset >>> 16) & 0xFF);
        baos.write((offset >>> 8) & 0xFF);
        baos.write(offset & 0xFF);

        baos.write(data);
        return baos.toByteArray();
    }

    /**
     * Main method acting as a standalone test runner
     */
    public static void main(String[] args)
    {
        try
        {
            System.out.println("Starting Extended XMP Reconstruction Test with multi-section domain schemas...");

            JpgParserTest testInstance = new JpgParserTest();
            testInstance.testExtendedXmpReconstruction();

            System.out.println("TEST PASSED SUCCESSFULLY! All custom domain sections stitched and validated perfectly.");
        }
        catch (Throwable t)
        {
            System.err.println("TEST FAILED!");
            t.printStackTrace();
        }
    }
}