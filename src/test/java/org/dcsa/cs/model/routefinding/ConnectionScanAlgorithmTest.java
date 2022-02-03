package org.dcsa.cs.model.routefinding;

import lombok.Data;
import org.dcsa.core.events.model.Vessel;
import org.dcsa.core.events.model.enums.CarrierCodeListProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

public class ConnectionScanAlgorithmTest {

    final ConnectionScanAlgorithm connectionScanAlgorithm = new ConnectionScanAlgorithm();

    private static final Vessel VESSEL_1 = vessel("9665592", "THALASSA HELLAS", "EMC");
    private static final Vessel VESSEL_2 = vessel("9684653", "YM World", "YML");
    private static final Vessel VESSEL_3 = vessel("9863297", "HMM Algeciras", "HMM");
    private static final Vessel VESSEL_4 = vessel("9302152", "ONE HARBOUR", "ONE");

    private static final List<CSAConnection> TIMETABLE_1 = asTimeTable(

            // Route one
            SimpleVesselCSAConnection.of("NLRTM", "GBLGP", date(2022, 1, 1), date(2022, 1, 3), VESSEL_1),
            SimpleVesselCSAConnection.of("GBLGP", "DEHAM", date(2022, 1, 4), date(2022, 1, 6), VESSEL_1),
            SimpleVesselCSAConnection.of("DEHAM", "BEANR", date(2022, 1, 7), date(2022, 1, 10), VESSEL_1),
            SimpleVesselCSAConnection.of("BEANR", "FRLEH", date(2022, 1, 11), date(2022, 1, 14), VESSEL_1),
            SimpleVesselCSAConnection.of("FRLEH", "NLRTM", date(2022, 1, 15), date(2022, 1, 17), VESSEL_1),

            // Route two
            SimpleVesselCSAConnection.of("DKCPH", "DKAAR", date(2022, 1, 6), date(2022, 1, 8), VESSEL_2),
            SimpleVesselCSAConnection.of("DKAAR", "DEHAM", date(2022, 1, 9), date(2022, 1, 11), VESSEL_2),
            SimpleVesselCSAConnection.of("DEHAM", "DKAAL", date(2022, 1, 12), date(2022, 1, 13), VESSEL_2),
            SimpleVesselCSAConnection.of("DKAAL", "DKFRC", date(2022, 1, 14), date(2022, 1, 15), VESSEL_2),
            SimpleVesselCSAConnection.of("DKFRC", "DKCPH", date(2022, 1, 16), date(2022, 1, 17), VESSEL_2),

            // Bonus connections for the purpose of shaking out bugs. They are deliberate corner cases (like direct
            // routes that happen to early in some cases)
            SimpleVesselCSAConnection.of("NLRTM", "GBLGP", date(2021, 11, 1), date(2021, 12, 30), VESSEL_3),
            SimpleVesselCSAConnection.of("GBLGP", "FRLEH", date(2022, 1, 3), date(2022, 1, 7), VESSEL_3),
            SimpleVesselCSAConnection.of("GBLGP", "DKCPH", date(2022, 1, 3), date(2022, 1, 7), VESSEL_4)

    );

    private static final List<CSAConnection> TIMETABLE_2 = asTimeTable(

            // Route one
            SimpleVesselCSAConnection.of("NLRTM", "GBLGP", date(2022, 1, 1), date(2022, 1, 3), VESSEL_1),
            SimpleVesselCSAConnection.of("GBLGP", "DEHAM", date(2022, 1, 4), date(2022, 1, 6), VESSEL_1),
            SimpleVesselCSAConnection.of("DEHAM", "BEANR", date(2022, 1, 7), date(2022, 1, 10), VESSEL_1),
            SimpleVesselCSAConnection.of("BEANR", "FRLEH", date(2022, 1, 11), date(2022, 1, 14), VESSEL_1),
            SimpleVesselCSAConnection.of("FRLEH", "NLRTM", date(2022, 1, 15), date(2022, 1, 17), VESSEL_1),
            SimpleVesselCSAConnection.of("NLRTM", "GBLGP", date(2022, 1, 18), date(2022, 1, 20), VESSEL_1),
            SimpleVesselCSAConnection.of("GBLGP", "DEHAM", date(2022, 1, 21), date(2022, 1, 23), VESSEL_1),

            // Route two
            SimpleVesselCSAConnection.of("DEHAM", "DOCAU", date(2022, 1, 24), date(2022, 1, 27), VESSEL_2)

    );

    @Test
    public void testRoutesTimetable1() {
        verifyRoute(TIMETABLE_1,
                "NLRTM",
                "FRLEH",
                date(2021, 12, 30),
                ExpectedRoute.of(
                        date(2022, 1, 1),
                        date(2022, 1, 14),
                        // Just stay on route 1 will get you there
                        "NLRTM", "GBLGP", "DEHAM", "BEANR", "FRLEH"
                ));

        verifyRoute(TIMETABLE_1,
                "NLRTM",
                "FRLEH",
                date(2021, 10, 15),
                ExpectedRoute.of(
                        date(2021, 11, 1),
                        date(2022, 1, 7),
                        // Here we can use the bonus routes
                        "NLRTM", "GBLGP", "FRLEH"
                ));

        verifyRoute(TIMETABLE_1,
                "NLRTM",
                "DKCPH",
                date(2021, 12, 30),
                ExpectedRoute.of(
                        date(2022, 1, 1),
                        date(2022, 1, 17),
                        // Route 1 until DEHAM and then change to Route 2
                        "NLRTM", "GBLGP", "DEHAM", "DKAAL", "DKFRC", "DKCPH"
                ));

        verifyRoute(TIMETABLE_1,
                "NLRTM",
                "DKCPH",
                date(2021, 10, 15),
                ExpectedRoute.of(
                        date(2021, 11, 1),
                        date(2022, 1, 7),
                        // Bonus routes
                        "NLRTM", "GBLGP", "DKCPH"
                ));

        assertNoRoute(TIMETABLE_1,
                "DKCPH",
                "NLRTM",
                date(2021, 12, 31)
                // Route 2 cannot make DEHAM in time for the change in DEHAM to route 1 (and since there is no future
                // rotations in the timetable, there is no route)
        );
    }

    @Test
    public void testRoutesTimetable2() {
        verifyRoute(TIMETABLE_2,
                "NLRTM",
                "DOCAU",
                date(2021, 12, 30),
                ExpectedRoute.of(
                        date(2022, 1, 1),
                        date(2022, 1, 27),
                        // We should not have to loop around DEHAM - just unload and wait for route 2
                        "NLRTM", "GBLGP", "DEHAM", "DOCAU"
                ));

    }

    @Test
    public void testInvalidArguments() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> connectionScanAlgorithm.findRoute(TIMETABLE_1, "DEHAM", "DEHAM", ZonedDateTime.now())
        );
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> connectionScanAlgorithm.findRoute(TIMETABLE_1, "DEHAM", "NTRLM", ConnectionScanAlgorithm.MAX_DATE_TIME)
        );
    }

    private void verifyRoute(List<CSAConnection> timetable,
                             String departingFrom,
                             String destination,
                             ZonedDateTime readyToDepartAt,
                             ExpectedRoute expectedRoute) {
        List<CSAConnection> route = connectionScanAlgorithm.findRoute(timetable, departingFrom, destination, readyToDepartAt);
        Assertions.assertFalse(route.isEmpty(), "Expected to find a route");
        ListIterator<CSAConnection> routeIter = route.listIterator();
        List<String> actualStops = new ArrayList<>(route.size() + 1);
        while (routeIter.hasNext()) {
            boolean isFirst = !routeIter.hasPrevious();
            CSAConnection c = routeIter.next();
            boolean isLast = !routeIter.hasNext();

            if (isFirst) {
                Assertions.assertEquals(expectedRoute.getDepartureFromFirstHop(), c.getDepartureDateTime());
                actualStops.add(c.getDepartureUNLocationCode());
            }
            // Special-case: isFirst and isLast can be true at the same time for single hop routes.
            if (isLast) {
                Assertions.assertEquals(expectedRoute.getArrivalAtDestination(), c.getArrivalDateTime());
            }
            actualStops.add(c.getArrivalUNLocationCode());
        }
        Assertions.assertEquals(expectedRoute.getStops(), actualStops);
    }

    private void assertNoRoute(List<CSAConnection> timetable,
                               String departingFrom,
                               String destination,
                               ZonedDateTime readyToDepartAt) {
        List<CSAConnection> route = connectionScanAlgorithm.findRoute(timetable, departingFrom, destination, readyToDepartAt);
        Assertions.assertTrue(route.isEmpty(), "Expected there not to be a route");
    }

    private static List<CSAConnection> asTimeTable(CSAConnection... connectionTOs) {
        Arrays.sort(connectionTOs, ConnectionScanAlgorithm.BY_DEPARTURE_DATE_TIME);
        return List.of(connectionTOs);
    }

    private static ZonedDateTime date(int year, int mon, int day) {
        // For the test, we use day precision (the actual precision is largely irrelevant, so it is mostly
        // a question of cognitive load when )
        return ZonedDateTime.of(year, mon, day, 0, 0, 0, 0, ZoneOffset.UTC);
    }

    @Data(staticConstructor = "of")
    private static class ExpectedRoute {
        final ZonedDateTime departureFromFirstHop;
        final ZonedDateTime arrivalAtDestination;
        final List<String> stops;

        static ExpectedRoute of(ZonedDateTime departureFromFirstHop, ZonedDateTime arrivalAtDestination, String ... stops) {
            return of(departureFromFirstHop, arrivalAtDestination, List.of(stops));
        }
    }

    private static Vessel vessel(String vesselIMONumber, String name, String operator) {
        Vessel v = new Vessel();
        v.setVesselIMONumber(vesselIMONumber);
        v.setVesselName(name);
        v.setVesselOperatorCarrierCode(operator);
        v.setVesselOperatorCarrierCodeListProvider(CarrierCodeListProvider.SMDG);
        return v;
    }
}
