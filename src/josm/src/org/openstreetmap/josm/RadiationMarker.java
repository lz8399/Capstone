package org.openstreetmap.josm;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxRoute;
import org.openstreetmap.josm.data.gpx.WayPoint;

import java.util.ArrayList;
import java.util.Optional;

public class RadiationMarker extends GpxRoute {

     RadiationMarker(int cps, LatLon latlon) {
        super();

        ArrayList<WayPoint> routePoints = new ArrayList<>();
        double latOffset = 0.000001;
        double lonOffset = 0.000001;
        Optional<Integer> cpsOpt = Optional.of(cps);
        for (int i=0; i < cps; i++) {
            routePoints.add(new RadiationWayPoint(Optional.empty(), new LatLon(latlon.lat(),latlon.lon())));
            routePoints.add(new RadiationWayPoint(Optional.empty(), new LatLon(latlon.lat(),latlon.lon()+lonOffset)));
            routePoints.add(new RadiationWayPoint(Optional.empty(), new LatLon(latlon.lat()+latOffset,latlon.lon()+lonOffset)));
            routePoints.add(new RadiationWayPoint(cpsOpt, new LatLon(latlon.lat()+latOffset,latlon.lon())));
            routePoints.add(new RadiationWayPoint(Optional.empty(), new LatLon(latlon.lat(),latlon.lon())));
        }
        this.routePoints = routePoints;
    }
}
