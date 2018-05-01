package org.openstreetmap.josm;

import jssc.SerialPort;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.IImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.io.FileUtils;
import org.openstreetmap.josm.actions.MergeLayerAction;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.conversion.LatLonParser;
import org.openstreetmap.josm.data.gpx.*;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.io.importexport.JpgImporter;
import org.openstreetmap.josm.gui.layer.*;
import org.openstreetmap.josm.tools.Logging;

import javax.imageio.*;
import javax.swing.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.openstreetmap.josm.tools.I18n.tr;

public class DisasterResponse {

    // e.g., C:\Users\Kevin\AppData\Local\Temp\TeamMahogany
    private static final String save_dir = Paths.get(System.getProperty("java.io.tmpdir"), "TeamMahogany").toString();

    public DisasterResponse() {

    }

    public static void Initialize() {
        // Zoom to Tufts University above Halligan and Tisch Gym
        zoomToTufts();

        // pretend radiation info
        //testRadiationLayer();

        // monitor serial port for incoming images and save them to disk
        new Thread(DisasterResponse::monitorSerialForImages).start();
    }

    private static void monitorSerialForImages()
    {
        // Prep Radiation Layer
        int cpsCounter = 2;
        GpxData data = new GpxData();
        GpxLayer gpx = new GpxLayer(data);
        gpx.setName("Radiation Layer");
        MainApplication.getLayerManager().addLayer(gpx);

        // delete all other files already here
        try {
            File tmpDir = new File(save_dir);
            if (tmpDir.exists()) {
                FileUtils.cleanDirectory(new File(save_dir));
            } else {
                tmpDir.mkdir();
            }
        } catch (IOException ioex) {}

        try {Thread.sleep(3000); } catch (Exception unused) {}
        Logging.info("Attempting to monitor serial port...");

        SimulatedSerialPort serialPort = new SimulatedSerialPort("COM3");
        try {
            serialPort.openPort();//Open serial port
            serialPort.setParams(115200, 8, 1, 0);//Set params.
            Logging.info("MONITORING SERIAL PORT...");
            while (true) {

                byte[] buffer = serialPort.readBytes();
                if (buffer != null) {
                    try {
                        BufferedImage img = ImageIO.read(new ByteArrayInputStream(buffer));
                        DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
                        String timestamp = fmt.format(new Date());
                        File outputFile = new File(String.format("%s\\%s.jpg", save_dir, timestamp));

                        ImageIO.write(img, "jpg", outputFile);

                        TiffImageMetadata metadata = readExifMetadata(buffer);

                        TiffImageMetadata.GPSInfo gps = metadata.getGPS();

                        ExifMetadataEditor editor = new ExifMetadataEditor();
                        File dst = new File(outputFile.getAbsolutePath() + "EDITED.jpg");

                        LatLon coords = new LatLon(gps.getLatitudeAsDegreesNorth(), gps.getLongitudeAsDegreesEast());

                        try {
                            editor.changeExifMetadata(outputFile, dst, coords);
                        } catch (Exception ex) {}

                        outputFile.delete();
                        dst.renameTo(outputFile);

                        ProcessImage(outputFile);

                        // Add radiation layer info as well
                        RadiationMarker radMarker = new RadiationMarker(cpsCounter++, coords);
                        data.addRoute(radMarker);

                    } catch (IOException ex) {
                        Logging.error(ex.toString());
                    }
                }
            }
        } catch (Exception ex) { Logging.error(ex.toString());}
    }

    private static void ProcessImage(File img) {

        JpgImporter importer = new JpgImporter();
        ArrayList<File> files = new ArrayList<>();

        files.add(img);

        try {
            importer.importData(files);
        } catch (Exception ex) {}

        // Need a delay so that the merge can work as expected
        try {
            Thread.sleep(1000);
        } catch(Exception unused) {}

        // Merge the layers!
        MergeLayerAction merge = new MergeLayerAction();
        List<Layer> curLayers = MainApplication.getLayerManager().getLayers(); //only gets the one Premium imagery layer!!
        List<Layer> geoTaggedLayers = new ArrayList<>();
        for (Layer layer : curLayers) {
            if (layer.getName().contains("Geotagged"))
            {
                geoTaggedLayers.add(layer);
            }
        }
        if (geoTaggedLayers.size() > 0) {
            Layer targetLayer = geoTaggedLayers.get(0);

            // merge all other layers onto the target
            for (int i = 1; i < geoTaggedLayers.size(); i++) {
                Layer geoTaggedLayer = geoTaggedLayers.get(i);
                merge.mergeOntoTargetLayer(targetLayer, geoTaggedLayer); //source and target
            }
        }
    }

    private static void testRadiationLayer() {
        GpxData data = new GpxData();

        LatLon latlon = new LatLon(0,0);
        LatLon latlon10 = new LatLon(1,0);

        RadiationMarker radMarker = new RadiationMarker(10, latlon);
        RadiationMarker radMarker2 = new RadiationMarker(100, latlon10);
        data.addRoute(radMarker2);
        data.addRoute(radMarker);

        GpxLayer gpx = new GpxLayer(data);
        gpx.setName("Radiation Layer");
        MainApplication.getLayerManager().addLayer(gpx);

    }

    private static ImmutableGpxTrack waypointGpxTrack(WayPoint... wps) {
        return new ImmutableGpxTrack(Collections.singleton(Arrays.asList(wps)), Collections.emptyMap());
    }

    private static TiffImageMetadata readExifMetadata(byte[] jpegData) throws ImageReadException, IOException {
        try {
            IImageMetadata imageMetadata = Imaging.getMetadata(jpegData);
            if (imageMetadata == null) {
                return null;
            }
            JpegImageMetadata jpegMetadata = (JpegImageMetadata) imageMetadata;
            TiffImageMetadata exif = jpegMetadata.getExif();
            if (exif == null) {
                return null;
            }
            return exif;
        } catch (Exception ex) { return null; }
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
}
