// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.Bounds;

/**
 * A gpx track segment consisting of multiple waypoints, that cannot be changed.
 */
public class ImmutableGpxTrackSegment implements GpxTrackSegment {

    private final List<WayPoint> wayPoints;
    private final Bounds bounds;
    private final double length;

    /**
     * Constructs a new {@code ImmutableGpxTrackSegment}.
     * @param wayPoints list of waypoints
     */
    public ImmutableGpxTrackSegment(Collection<WayPoint> wayPoints) {
        this.wayPoints = Collections.unmodifiableList(new ArrayList<>(wayPoints));
        this.bounds = calculateBounds();
        this.length = calculateLength();
    }

    private Bounds calculateBounds() {
        Bounds result = null;
        for (WayPoint wpt: wayPoints) {
            if (result == null) {
                result = new Bounds(wpt.getCoor());
            } else {
                result.extend(wpt.getCoor());
            }
        }
        return result;
    }

    private double calculateLength() {
        double result = 0.0; // in meters
        WayPoint last = null;
        for (WayPoint tpt : wayPoints) {
            if (last != null) {
                Double d = last.getCoor().greatCircleDistance(tpt.getCoor());
                if (!d.isNaN() && !d.isInfinite()) {
                    result += d;
                }
            }
            last = tpt;
        }
        return result;
    }

    @Override
    public Bounds getBounds() {
        return bounds == null ? null : new Bounds(bounds);
    }

    @Override
    public Collection<WayPoint> getWayPoints() {
        return Collections.unmodifiableList(wayPoints);
    }

    @Override
    public double length() {
        return length;
    }

    @Override
    public int getUpdateCount() {
        return 0;
    }

    @Override
    public int hashCode() {
        return 31 + ((wayPoints == null) ? 0 : wayPoints.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ImmutableGpxTrackSegment other = (ImmutableGpxTrackSegment) obj;
        if (wayPoints == null) {
            if (other.wayPoints != null)
                return false;
        } else if (!wayPoints.equals(other.wayPoints))
            return false;
        return true;
    }
}