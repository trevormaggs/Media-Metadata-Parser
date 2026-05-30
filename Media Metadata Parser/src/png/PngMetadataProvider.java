package png;

import common.Metadata;
import xmp.XmpDirectory;

public interface PngMetadataProvider extends Metadata<PngDirectory>
{
    PngDirectory getDirectory(ChunkType.Category key);
    void addXmpDirectory(XmpDirectory dir);
    XmpDirectory getXmpDirectory();
    boolean hasTextualData();
}