package gui;

import java.util.LinkedHashMap;
import java.util.Map;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.web.WebView;
import tif.DirectoryIFD;
import tif.tagspecs.GpsDataManager;
import tif.tagspecs.TagIFD_GPS;

/**
 * Manages UI presentation, ComboBox synchronization, and Leaflet map rendering
 * for {@link MetadataViewerDialog} using {@link GpsDataManager} as a static delegate.
 */
public class GpsMetadataViewerManager
{
    private final WebView mapView;
    private final ComboBox<String> cbGpsFiles;
    private final Map<String, GpsLocation> locationMap;

    public GpsMetadataViewerManager(WebView mapView, ComboBox<String> cbGpsFiles)
    {
        this.mapView = mapView;
        this.cbGpsFiles = cbGpsFiles;
        this.locationMap = new LinkedHashMap<>();

        this.cbGpsFiles.setOnAction(new EventHandler<ActionEvent>()
        {
            @Override
            public void handle(ActionEvent event)
            {
                String selectedFile = cbGpsFiles.getValue();

                if (selectedFile != null)
                {
                    renderMap(selectedFile);
                }
            }
        });
    }

    public void clear()
    {
        locationMap.clear();
    }

    public void processIfd(String fileName, DirectoryIFD ifd)
    {
        if (ifd != null && !locationMap.containsKey(fileName))
        {
            String latRef = null;
            String lonRef = null;
            Object rawLatData = null;
            Object rawLonData = null;

            for (DirectoryIFD.EntryIFD entry : ifd)
            {
                if (entry.getTag() instanceof TagIFD_GPS)
                {
                    TagIFD_GPS tag = (TagIFD_GPS) entry.getTag();

                    switch (tag)
                    {
                        case GPS_LATITUDE_REF:
                            latRef = GpsDataManager.getDisplayValue(entry.getData(), tag);
                            break;

                        case GPS_LONGITUDE_REF:
                            lonRef = GpsDataManager.getDisplayValue(entry.getData(), tag);
                            break;

                        case GPS_LATITUDE:
                            rawLatData = entry.getData();
                            break;

                        case GPS_LONGITUDE:
                            rawLonData = entry.getData();
                            break;

                        default:
                            break;
                    }
                }
            }

            if (rawLatData != null && rawLonData != null)
            {
                Double lat = GpsDataManager.parseToDecimal(rawLatData, latRef);
                Double lon = GpsDataManager.parseToDecimal(rawLonData, lonRef);

                if (lat != null && lon != null)
                {
                    locationMap.put(fileName, new GpsLocation(fileName, lat, lon));
                }
            }
        }
    }

    public void syncUi()
    {
        cbGpsFiles.getItems().clear();

        if (hasLocations())
        {
            cbGpsFiles.getItems().addAll(locationMap.keySet());
            cbGpsFiles.getSelectionModel().selectFirst();
            renderMap(cbGpsFiles.getValue());
        }
    }

    public boolean hasLocations()
    {
        return !locationMap.isEmpty();
    }

    private void renderMap(String fileName)
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