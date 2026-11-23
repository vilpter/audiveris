/*
 * Copyright © Audiveris 2026. All rights reserved.
 * This software is released under the GNU General Public License.
 * Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
 */
package org.audiveris.omr.sheet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.audiveris.omr.sheet.grid.LineInfo;
import org.junit.Test;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Staves around a point, when tight staves leave a band between two staff areas.
 */
public class StavesAroundTest
{
    private static final int INTERLINE = 20;

    private static final int MARGIN = 19; // Area margin larger than half the gap between staves

    /** Staff with 5 straight lines, its first line at top. */
    private static Staff staff (int id,
                                int top)
    {
        final List<LineInfo> lines = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            final int y = top + (i * INTERLINE);
            lines.add(new StaffLine(List.of(new Point2D.Double(100, y), new Point2D.Double(1000, y)), 2));
        }

        return new Staff(id, 100, 1000, INTERLINE, lines);
    }

    /** Three staves 30 pixels apart, whose areas stop MARGIN away from their neighbors. */
    private static SystemInfo tightSystem ()
    {
        final List<Staff> staves = List.of(staff(1, 100), staff(2, 210), staff(3, 320));
        final SystemInfo system = new SystemInfo(1, null, staves);

        for (int i = 0; i < staves.size(); i++) {
            final int north = (i == 0) ? 0 : (staves.get(i - 1).getLastLine().yAt(500) + MARGIN);
            final int south = (i == staves.size() - 1) ? 2000
                    : (staves.get(i + 1).getFirstLine().yAt(500) - MARGIN);
            staves.get(i).setArea(new Area(new Rectangle(0, north, 1100, south - north)));
        }

        return system;
    }

    @Test
    public void pointInTheBandBetweenTwoAreasGetsBothStaves ()
    {
        final SystemInfo system = tightSystem();
        final Point2D point = new Point2D.Double(500, 195); // Between 180 + 0.5 and 210 + 0.5
        assertTrue(system.getStavesOf(point).isEmpty());

        final List<Staff> around = system.getStavesAround(point);
        assertEquals(2, around.size());
        assertEquals(1, around.get(0).getId());
        assertEquals(2, around.get(1).getId());
    }

    @Test
    public void pointOnStaffLinesOutsideItsAreaGetsThatStaff ()
    {
        final SystemInfo system = tightSystem();
        final Staff second = system.getStaves().get(1);
        second.setArea(new Area(new Rectangle(0, 230, 1100, 100))); // Area misses the top line
        final Point2D point = new Point2D.Double(500, 215);
        assertTrue(system.getStavesOf(point).isEmpty());

        final List<Staff> around = system.getStavesAround(point);
        assertEquals(1, around.size());
        assertEquals(2, around.get(0).getId());
    }

    @Test
    public void pointInsideAnAreaIsUnchanged ()
    {
        final SystemInfo system = tightSystem();
        final List<Staff> around = system.getStavesAround(new Point2D.Double(500, 250));
        assertEquals(1, around.size());
        assertEquals(2, around.get(0).getId());
    }

    @Test
    public void pointAboveTheSystemGetsNothing ()
    {
        final SystemInfo system = tightSystem();

        for (Staff staff : system.getStaves()) {
            staff.setArea(new Area(new Rectangle(0, 90, 1100, 1000)));
        }

        assertTrue(system.getStavesAround(new Point2D.Double(500, 50)).isEmpty());
    }
}
