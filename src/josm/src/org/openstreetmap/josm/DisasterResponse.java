package org.openstreetmap.josm;

import org.openstreetmap.josm.actions.DiskAccessAction;
import org.openstreetmap.josm.actions.MergeLayerAction;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.conversion.LatLonParser;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.io.importexport.JpgImporter;
import org.openstreetmap.josm.gui.layer.*;
import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer;
import org.openstreetmap.josm.gui.layer.geoimage.ImageEntry;
import org.openstreetmap.josm.gui.widgets.AbstractFileChooser;

import javax.swing.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.openstreetmap.josm.tools.I18n.tr;

public class DisasterResponse {

    public DisasterResponse() {

    }

    public static void Initialize() {
        // Zoom to Tufts University above Halligan and Tisch Gym
        zoomToTufts();

        // monitor new images and process them in a new dynamic "Geotagged Images" layer
        new Thread(DisasterResponse::monitorThread).start();

        // pretend radiation info
        //testRadiationLayer();

    }

    private static void monitorThread() {

        parseInputFromSerialUSB();

        //Run on a separate thread. Have third image source and add those while program is running to confirm REAL TIME!
        // Add programatically... of course..
        // Also add UI to start monitor and end monitor
        // Take out unwanted/confusing pictures.

        try {
            Thread.sleep(3000);
        } catch(Exception unused) {}
        MergeLayerAction merge = new MergeLayerAction();

        List<Layer> curLayers = MainApplication.getLayerManager().getLayers(); //only gets the one Premium imagery layer!!

        List<Layer> geoTaggedLayers = new ArrayList<>();
        for (Layer layer : curLayers) {
            if (layer.getName().contains("Geotagged"))
            {
                geoTaggedLayers.add(layer);
            }
        }

        Layer targetLayer = geoTaggedLayers.get(0);

        // merge all other layers onto the target
        for (int i = 1; i < geoTaggedLayers.size(); i++) {
            Layer geoTaggedLayer = geoTaggedLayers.get(i);
            merge.mergeOntoTargetLayer(targetLayer, geoTaggedLayer); //source and target
        }
    }

    private static void testRadiationLayer() {
        DataSet dataSet = new DataSet();
        double northPos = 42.4075;
        double eastPos = 71.1190;

        //double northPos = 0;
        //double eastPos = 0;

//        ImageNode n1 = new ImageNode(new EastNorth(eastPos+0, northPos+0));
//        ImageNode n2 = new ImageNode(new EastNorth(eastPos-1, northPos+1));
//        ImageNode n3 = new ImageNode(new EastNorth(eastPos+1, northPos+1));
//        ImageNode n4 = new ImageNode(new EastNorth(eastPos-1, northPos-1));
//        ImageNode n5 = new ImageNode(new EastNorth(eastPos+1, northPos-1));
//        ImageNode n6 = new ImageNode(new EastNorth(eastPos-1, northPos+0));
//        ImageNode n7 = new ImageNode(new EastNorth(eastPos+1, northPos+0));

        Node n1 = new Node(new EastNorth(eastPos+0, northPos+0));
        Node n2 = new Node(new EastNorth(eastPos-1, northPos+1));
        Node n3 = new Node(new EastNorth(eastPos+1, northPos+1));
        Node n4 = new Node(new EastNorth(eastPos-1, northPos-1));
        Node n5 = new Node(new EastNorth(eastPos+1, northPos-1));
        Node n6 = new Node(new EastNorth(eastPos-1, northPos+0));
        Node n7 = new Node(new EastNorth(eastPos+1, northPos+0));

        dataSet.addPrimitive(n1);
        dataSet.addPrimitive(n2);
        dataSet.addPrimitive(n3);
        dataSet.addPrimitive(n4);
        dataSet.addPrimitive(n5);
        dataSet.addPrimitive(n6);
        dataSet.addPrimitive(n7);

        //ImageryLayer layer2 = new ImageryLayer();
        //TMSLayer layer2 = new TMSLayer(new ImageryInfo("Radiation Layer", "http://www.url.com/"));
        //layer2.getDisplaySettings().setOffsetBookmark(
        //       new OffsetBookmark(Main.getProjection().toCode(), layer2.getInfo().getName(), "", 12, 34));

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

        OsmDataLayer layer = new OsmDataLayer(dataSet, "Radiation Layer", null);
        MainApplication.getLayerManager().addLayer(layer);
    }

    private static void zoomToTufts() {
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
    }

    private static void parseInputFromSerialUSB() {

//        ExifMetadataEditor editor = new ExifMetadataEditor();
        File file = new File("C:\\Users\\Kevin\\git\\Capstone\\sample_data\\Image uploaded from iOS.jpg");
//        File dest = new File("C:\\Users\\Kevin\\git\\Capstone\\sample_data\\Image uploaded from iOS-EDITED.jpg");
//        try {
//            editor.changeExifMetadata(file, dest);
//        } catch (Exception ex){}
//
//        https://wiki.openstreetmap.org/wiki/JOSM/Plugins/Photo_Geotagging#How_to_use
//
//        LatLon coords = null;
//        while (coords == null) {
//            try {
//                coords = new LatLon(Double.parseDouble("42.4075° N"), Double.parseDouble("71.1190° W"));
//            } catch (NumberFormatException ex) {
//                try {
//                    coords = LatLonParser.parse("42.4075° N" + "; " + "71.1190° W");
//                } catch (IllegalArgumentException ex2) {
//                    JOptionPane.showMessageDialog(Main.parent,
//                            tr("Could not parse Latitude, Longitude or Zoom. Please check."),
//                            tr("Unable to parse Lon/Lat"), JOptionPane.ERROR_MESSAGE);
//                }
//            }
//        }
//        ImageEntry entry = new ImageEntry();
//        entry.setFile(file);
//        entry.setExifCoor(coords);
//        entry.flagNewGpsData();

        JpgImporter importer = new JpgImporter();
        //Component parent = MainApplication.parent;
        //String title = "Title";
        //ProgressMonitor progressMonitor = new PleaseWaitProgressMonitor(parent, title);
        ArrayList<File> files = new ArrayList<>();
        files.add(new File("C:\\Users\\Kevin\\git\\Capstone\\sample_data"));
        try {
            importer.importData(files);
        } catch (Exception ex) {
        }

        // create a sample data two, then merge the second layer so we can constantly add to the first layer

        ArrayList<File> files2 = new ArrayList<>();
        files2.add(new File("C:\\Users\\Kevin\\git\\Capstone\\sample_data2"));
        try {
            importer.importData(files2);
        } catch (Exception ex) {
        }

        // try to modify EXIF metadata in application after running
        LatLon coords = null;
        while (coords == null) {
            try {
                coords = new LatLon(Double.parseDouble("42.4075° n"), Double.parseDouble("71.1190° w"));
            } catch (NumberFormatException ex) {
                try {
                    coords = LatLonParser.parse("42.4075° n" + "; " + "71.1190° w");
                } catch (IllegalArgumentException ex2) {
                    JOptionPane.showMessageDialog(Main.parent,
                            tr("could not parse latitude, longitude or zoom. please check."),
                            tr("unable to parse lon/lat"), JOptionPane.ERROR_MESSAGE);
                }
            }
        }

//        List<Layer> curLayers = MainApplication.getLayerManager().getLayers(); //only gets the one Premium imagery layer!!
//
//        List<Layer> geoTaggedLayers = new ArrayList<>();
//        for (Layer layer : curLayers) {
//            if (layer.getName().contains("Geotagged"))
//            {
//                geoTaggedLayers.add(layer);
//            }
//        }
//
//        Layer targetLayer = geoTaggedLayers.get(0);

        //AbstractFileChooser fc = DiskAccessAction.createAndOpenFileChooser(true, false, null,
        //        JpgImporter.FILE_FILTER_WITH_FOLDERS, JFileChooser.FILES_ONLY, "geoimage.lastdirectory");
        //if (fc == null)
        //    return;
        //ImageEntry entry = new ImageEntry(fc.getSelectedFile());
//        ImageEntry entry = new ImageEntry(file);
//
//        //ImageEntry entry = targetLayer.getImages();
//        entry.setFile(file);
//        entry.setPos(coords);
//        entry.setSpeed(90.0);
//        entry.setElevation(99.0);
//        entry.setGpsTime(new Date(2));
//        entry.setExifCoor(coords);
//        entry.setExifTime(new Date(1));
//        entry.setExifImgDir(99.2);
//        entry.flagNewGpsData();
        File jpg = new File("C:\\Users\\Kevin\\git\\Capstone\\sample_data\\Image uploaded from iOS.jpg");
        File dst = new File("C:\\Users\\Kevin\\git\\Capstone\\sample_data\\Image uploaded from iOS-EDITED.jpg");

        // THIS WORKED JUST NEED TO BUILD IN ANT
        ExifMetadataEditor editor = new ExifMetadataEditor();
        try {
            editor.changeExifMetadata(jpg, dst);
        } catch (Exception ex) {}
    }

}
