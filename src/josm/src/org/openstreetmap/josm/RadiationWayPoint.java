package org.openstreetmap.josm;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.WayPoint;

import java.util.Optional;

public class RadiationWayPoint extends WayPoint {

    private final Optional<Double> countsPerSec;

    RadiationWayPoint(Optional<Double> cps, LatLon latlon) {
        super(latlon);

        if (cps.isPresent()) {
            double cpsTemp = cps.get();
            cpsTemp = (double)Math.round(cpsTemp * 1000d) / 1000d; // round to 3 digits
            countsPerSec = Optional.of(cpsTemp);
        }
        else {
            countsPerSec = Optional.empty();
        }
    }

    public String getCPS() {
        return countsPerSec.isPresent() ? "CPS: "+countsPerSec.get() : "";
    }

    public Double getCPSDouble() {
        return countsPerSec.isPresent() ? countsPerSec.get() : 0.0;
    }

}