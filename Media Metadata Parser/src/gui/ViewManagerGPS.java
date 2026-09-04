package gui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.web.WebView;
import tif.DirectoryIFD;
import tif.tagspecs.GpsDataManager;
import tif.tagspecs.TagIFD_GPS;

/**
 * Manages Leaflet map rendering for {@link MetadataViewerDialog} using
 * {@link GpsDataManager} as a static delegate.
 */
public class ViewManagerGPS
{
    private final WebView mapView;
    private final Map<String, GpsLocation> locationMap;

    public ViewManagerGPS(WebView mapView)
    {
        this.mapView = mapView;
        this.locationMap = new LinkedHashMap<>();
    }

    public void reset()
    {
        locationMap.clear();
    }

    public void addLocationGPS(String fileName, DirectoryIFD ifd)
    {
        if (ifd != null && !locationMap.containsKey(fileName))
        {
            Object latRef = (ifd.hasTag(TagIFD_GPS.GPS_LATITUDE_REF) ? ifd.getTagEntry(TagIFD_GPS.GPS_LATITUDE_REF).getData() : null);
            Object lonRef = (ifd.hasTag(TagIFD_GPS.GPS_LONGITUDE_REF) ? ifd.getTagEntry(TagIFD_GPS.GPS_LONGITUDE_REF).getData() : null);
            Object rawLatData = (ifd.hasTag(TagIFD_GPS.GPS_LATITUDE) ? ifd.getTagEntry(TagIFD_GPS.GPS_LATITUDE).getData() : null);
            Object rawLonData = (ifd.hasTag(TagIFD_GPS.GPS_LONGITUDE) ? ifd.getTagEntry(TagIFD_GPS.GPS_LONGITUDE).getData() : null);

            if (rawLatData != null && rawLonData != null)
            {
                Double lat = GpsDataManager.parseToDecimal(rawLatData, String.valueOf(latRef));
                Double lon = GpsDataManager.parseToDecimal(rawLonData, String.valueOf(lonRef));

                if (lat != null && lon != null)
                {
                    locationMap.put(fileName, new GpsLocation(fileName, lat, lon));
                }
            }
        }
    }

    /**
     * Returns all processed file names containing GPS data and automatically renders
     * the map for the first available location.
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

    public boolean hasDataGPS()
    {
        return !locationMap.isEmpty();
    }

    public void renderMap(String fileName)
    {
        GpsLocation loc = locationMap.get(fileName);

        if (loc != null)
        {
            String safeTitle = (loc.fileName != null ? loc.fileName.replace("'", "\\'") : "Location");

            mapView.getEngine().loadContent(buildMapHtml(safeTitle, loc.latitude, loc.longitude));
        }
    }

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