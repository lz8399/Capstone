package org.openstreetmap.josm;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.conversion.LatLonParser;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;

import javax.swing.*;

import static org.openstreetmap.josm.tools.I18n.tr;

public class DisasterResponse {

    public DisasterResponse() {

    }

    public static void Initialize() {
        // Zoom to Tufts University above Halligan and Tisch Gym - need to call this later!!
        MapView mv = MainApplication.getMap().mapView;
        LatLon ll = null;
        double zoomLvl = 100;
        while (ll == null) {
            //final int option = new JumpToPositionDialog(buttons, panel).showDialog().getValue();

            //if (option != 1) return;
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
