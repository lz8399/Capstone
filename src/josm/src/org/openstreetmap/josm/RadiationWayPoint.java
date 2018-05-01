package org.openstreetmap.josm;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.WayPoint;

import java.util.Optional;

public class RadiationWayPoint extends WayPoint {

    private final Optional<Integer> countsPerSec;

    RadiationWayPoint(Optional<Integer> cps, LatLon latlon) {
        super(latlon);
        countsPerSec = cps;
    }

    public String getCPS() {
        return countsPerSec.isPresent() ? "CPS: "+countsPerSec.get() : "";
    }
}