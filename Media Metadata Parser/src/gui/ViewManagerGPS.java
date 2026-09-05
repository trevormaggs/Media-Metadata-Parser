package gui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.web.WebView;
import tif.DirectoryIFD;
import tif.DirectoryIFD.EntryIFD;
import tif.tagspecs.GpsDataManager;
import tif.tagspecs.TagIFD_GPS;

/**
 * Manages Leaflet map rendering for {@link MetadataViewerDialog} and delegates GPS coordinate
 * parsing to the static methods in {@link GpsDataManager}.
 */
public class ViewManagerGPS
{
    private final WebView mapView;
    private final Map<String, GpsLocation> locationMap;

    /**
     * Constructs a new {@code ViewManagerGPS} associated with the specified {@link WebView}.
     *
     * @param mapView
     *        the JavaFX {@link WebView} used to render Leaflet map instances
     */
    public ViewManagerGPS(WebView mapView)
    {
        this.mapView = mapView;
        this.locationMap = new LinkedHashMap<>();
    }

    /**
     * Clears all cached GPS locations from the internal lookup map.
     */
    public void reset()
    {
        locationMap.clear();
    }

    /**
     * Extracts GPS metadata from an IFD directory and stores the parsed coordinates if they are
     * valid and the file has not already been registered.
     *
     * @param fileName
     *        the file name associated with the metadata directory
     * @param ifd
     *        the target {@link DirectoryIFD} containing potential GPS metadata tags
     */
    public void addLocationGPS(String fileName, DirectoryIFD ifd)
    {
        if (ifd != null && fileName != null && !locationMap.containsKey(fileName))
        {
            EntryIFD latRefEntry = ifd.hasTag(TagIFD_GPS.GPS_LATITUDE_REF) ? ifd.getTagEntry(TagIFD_GPS.GPS_LATITUDE_REF) : null;
            EntryIFD lonRefEntry = ifd.hasTag(TagIFD_GPS.GPS_LONGITUDE_REF) ? ifd.getTagEntry(TagIFD_GPS.GPS_LONGITUDE_REF) : null;
            EntryIFD latDataEntry = ifd.hasTag(TagIFD_GPS.GPS_LATITUDE) ? ifd.getTagEntry(TagIFD_GPS.GPS_LATITUDE) : null;
            EntryIFD lonDataEntry = ifd.hasTag(TagIFD_GPS.GPS_LONGITUDE) ? ifd.getTagEntry(TagIFD_GPS.GPS_LONGITUDE) : null;

            Object rawLatData = (latDataEntry != null ? latDataEntry.getData() : null);
            Object rawLonData = (lonDataEntry != null ? lonDataEntry.getData() : null);

            if (rawLatData != null && rawLonData != null)
            {
                String latRef = (latRefEntry != null && latRefEntry.getData() != null ? String.valueOf(latRefEntry.getData()) : null);
                String lonRef = (lonRefEntry != null && lonRefEntry.getData() != null ? String.valueOf(lonRefEntry.getData()) : null);

                Double lat = GpsDataManager.parseToDecimal(rawLatData, latRef);
                Double lon = GpsDataManager.parseToDecimal(rawLonData, lonRef);

                if (lat != null && lon != null)
                {
                    locationMap.put(fileName, new GpsLocation(fileName, lat, lon));
                }
            }
        }
    }

    /**
     * Returns all registered file names containing valid GPS locations and renders the map for the
     * first registered location.
     *
     * @return a list of file names with valid GPS locations
     */
    public List<String> update()
    {
        List<String> fileNames = new ArrayList<>(locationMap.keySet());

        if (!fileNames.isEmpty())
        {
            renderMap(fileNames.get(0));
        }

        return fileNames;
    }

    /**
     * Checks whether any valid GPS locations are registered.
     *
     * @return {@code true} if at least one valid GPS location is registered
     */
    public boolean hasDataGPS()
    {
        return !locationMap.isEmpty();
    }

    /**
     * Loads and renders the Leaflet map in the associated {@link WebView} for the given file name.
     *
     * @param fileName
     *        the key matching a registered GPS location
     */
    public void renderMap(String fileName)
    {
        GpsLocation loc = locationMap.get(fileName);

        if (loc != null && mapView != null)
        {
            String safeTitle = (loc.fileName != null ? loc.fileName.replace("\\", "\\\\").replace("'", "\\'").replace("\n", " ") : "Location");

            mapView.getEngine().loadContent(buildMapHtml(safeTitle, loc.latitude, loc.longitude));
        }
    }

    /**
     * Builds the raw HTML and JavaScript string containing embedded Leaflet map setup commands.
     *
     * @param title
     *        the escaped popup display label
     * @param lat
     *        latitude value in decimal degrees
     * @param lon
     *        longitude value in decimal degrees
     * @return the complete HTML document containing the Leaflet map
     */
    private String buildMapHtml(String title, double lat, double lon)
    {
        return "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "  <link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>"
                + "  <script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>"
                + "  <style>html, body, #map { height: 100%; margin: 0; padding: 0; }</style>"
                + "</head>"
                + "<body>"
                + "  <div id='map'></div>"
                + "  <script>"
                + "    var map = L.map('map').setView([" + lat + ", " + lon + "], 15);"
                + "    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {"
                + "      maxZoom: 19,"
                + "      attribution: '© OpenStreetMap'"
                + "    }).addTo(map);"
                + "    L.marker([" + lat + ", " + lon + "]).addTo(map)"
                + "      .bindPopup('<b>" + title + "</b><br>Lat: " + lat + "<br>Lon: " + lon + "')"
                + "      .openPopup();"
                + "  </script>"
                + "</body>"
                + "</html>";
    }

    /**
     * Holds parsed geographic coordinates for a single file entry.
     */
    private static class GpsLocation
    {
        private final String fileName;
        private final double latitude;
        private final double longitude;

        GpsLocation(String fileName, double latitude, double longitude)
        {
            this.fileName = fileName;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}