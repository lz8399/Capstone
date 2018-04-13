package org.openstreetmap.josm;

import org.openstreetmap.josm.data.StructUtils;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.conversion.LatLonParser;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.OffsetBookmark;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.io.importexport.JpgImporter;
import org.openstreetmap.josm.gui.layer.*;
import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer;
import org.openstreetmap.josm.gui.layer.geoimage.ImageEntry;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.progress.swing.PleaseWaitProgressMonitor;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import wiremock.org.apache.http.conn.routing.RouteInfo;

import javax.swing.*;

import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.openstreetmap.josm.tools.I18n.tr;

public class DisasterResponse {

    public DisasterResponse() {

    }

    public static void Initialize() {
        // Zoom to Tufts University above Halligan and Tisch Gym
        MapView mv = MainApplication.getMap().mapView;
        LatLon ll = null;
        double zoomLvl = 100;
        while (ll == null) {
            try {
                zoomLvl = Double.parseDouble("14");
                ll = new LatLon(Double.parseDouble("42.4075° N"), Double.parseDouble("71.1190° W"));
            } catch (NumberFormatException ex) {
                try {
                    ll = LatLonParser.parse("42.4075° N" + "; " + "71.1190° W");
                } catch (IllegalArgumentException ex2) {
                    JOptionPane.showMessageDialog(Main.parent,
                            tr("Could not parse Latitude, Longitude or Zoom. Please check."),
                            tr("Unable to parse Lon/Lat"), JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        double zoomFactor = 1/ mv.getDist100Pixel();
        mv.zoomToFactor(mv.getProjection().latlon2eastNorth(ll), zoomFactor * zoomLvl);

        DataSet dataSet = new DataSet();
        double northPos = 42.4075;
        double eastPos = 71.1190;
        ImageNode n1 = new ImageNode(new EastNorth(eastPos+0, northPos+0));
        //n1.set
        ImageNode n2 = new ImageNode(new EastNorth(eastPos-1, northPos+1));
        ImageNode n3 = new ImageNode(new EastNorth(eastPos+1, northPos+1));
        ImageNode n4 = new ImageNode(new EastNorth(eastPos-1, northPos-1));
        ImageNode n5 = new ImageNode(new EastNorth(eastPos+1, northPos-1));
        ImageNode n6 = new ImageNode(new EastNorth(eastPos-1, northPos+0));
        ImageNode n7 = new ImageNode(new EastNorth(eastPos+1, northPos+0));
        dataSet.addPrimitive(n1);
        dataSet.addPrimitive(n2);
        dataSet.addPrimitive(n3);
        dataSet.addPrimitive(n4);
        dataSet.addPrimitive(n5);
        dataSet.addPrimitive(n6);
        dataSet.addPrimitive(n7);

        //ImageryLayer layer2 = new ImageryLayer();
        TMSLayer layer2 = new TMSLayer(new ImageryInfo("Custom Layer", "http://www.url.com/"));
        layer2.getDisplaySettings().setOffsetBookmark(
                new OffsetBookmark(Main.getProjection().toCode(), layer2.getInfo().getName(), "", 12, 34));

        // Might need to overload Node to create custom Node class to display our UI

        // Types of Layers to investigate:
//        public static ImageryLayer create(ImageryInfo info) {
//            switch(info.getImageryType()) {
//                case WMS:
//                    return new WMSLayer(info);
//                case WMTS:
//                    return new WMTSLayer(info);
//                case TMS:
//                case BING:
//                case SCANEX:
//                    return new TMSLayer(info);
//                default:
//                    throw new AssertionError(tr("Unsupported imagery type: {0}", info.getImageryType()));
//            }
//        }

        OsmDataLayer layer = new OsmDataLayer(dataSet, "Custom Layer", null);
        //MainApplication.getLayerManager().addLayer(layer);

        JpgImporter importer = new JpgImporter();
        Component parent = MainApplication.parent;

        String title = "Title";
        ProgressMonitor progressMonitor = new PleaseWaitProgressMonitor(parent, title);
        ArrayList<File> files = new ArrayList<>();
        files.add(new File("C:\\Users\\Kevin\\git\\Capstone\\sample_data"));
        try {
            importer.importData(files);
        } catch (Exception ex) {
        }


        //MainApplication.getMap().mapView.getLayerManager().addLayer(geoImageLayer);

        //() -> AbstractTileSourceLayer.drawImageInside;

        //NavigatableComponent.zoomIn();
        //MainApplication.getMap().mapView.zoomOut();
        //MainApplication.getMap().mapView.zoomOut();
        //MainApplication.getMap().mapView.zoomOut();


        //MinimapDialog();

        // Then we need to move to the proper GPS location
        //Latlon latlon = LatLonParser.parse("N 49.29918 E 19.24788°"); // Tufts 42.4075° N, 71.1190° W


        // Move to the proper zoom level
    }
}
