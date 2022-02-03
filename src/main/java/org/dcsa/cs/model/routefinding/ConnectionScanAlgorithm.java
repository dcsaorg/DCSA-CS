package org.dcsa.cs.model.routefinding;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.Year;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

// Based on CSA as defined by https://arxiv.org/pdf/1703.05997.pdf
public class ConnectionScanAlgorithm {

    // It is close enough to count as "never" and that is sufficient for our usage.
    static final ZonedDateTime MAX_DATE_TIME = ZonedDateTime.of(Year.MAX_VALUE, 12, 31, 23, 59, 59, 99999, ZoneOffset.UTC);
    static final Comparator<CSAConnection> BY_DEPARTURE_DATE_TIME = Comparator.comparing(CSAConnection::getDepartureDateTime);

    /**
     * Find a route between two locations given a start location, end location, a departure time and a timetable
     * of all transport means available.
     *
     *
     * @param timetable A list of possible connections between all locations sorted by departure date.  The caller
     *                  can pruning out connections with a departure time before departureFromStartDateTime for
     *                  optimal performance. The timetable is deliberately bounded as the caller should pick a
     *                  "reasonable" time horizont for the delivery (e.g., "within 6 months"). With an infinite
     *                  timetable, the algorithm would never terminate if there is no route.
     * @param startUNLocationCode The UN Location Code of where you want the cargo to be picked up.
     * @param destinationUNLocationCode The UN Location Code of where you want the cargo to go to.
     * @param departureFromStartDateTime Time of departure from startUNLocationCode.  This decides the earliest
     *                                  connection you can take from the starting location.
     * @return A list of connections used to arrive at the destination.  If the list is empty, then no route could be found.
     * @throws IllegalArgumentException If startUNLocationCode is equal to destinationUNLocationCode.
     */
    public List<CSAConnection> findRoute(List<CSAConnection> timetable, String startUNLocationCode, String destinationUNLocationCode, ZonedDateTime departureFromStartDateTime) {
        // Trivial base-case - getting it out of the way makes it easier to keep track of the general solution
        // because we can now assume a solution will have at least one hop after this is dealt with.
        if (Objects.equals(startUNLocationCode, destinationUNLocationCode)) {
            throw new IllegalArgumentException("Start and End location is the same.  This is most likely a mistake");
        }
        if (!departureFromStartDateTime.isBefore(MAX_DATE_TIME)) {
            throw new IllegalArgumentException("The departure time must be before " + MAX_DATE_TIME);
        }
        assert isSorted(timetable);
        Map<String, ConnectionState> stops = new HashMap<>();
        stops.put(startUNLocationCode, ConnectionState.of(departureFromStartDateTime));
        ZonedDateTime bestArrivalTime = MAX_DATE_TIME;

        for (CSAConnection connectionTO : timetable) {
            // We could avoid this condition if we implemented the "Starting criterion" optimization from the paper.
            // (using binary search to find the first entry in the list).  However, except for test cases, we will
            // be pulling the timetable from the database where we can let the database filter based on departure date,
            // which is how we will be implementing that optimization.
            if (connectionTO.getDepartureDateTime().isBefore(departureFromStartDateTime)) {
                continue;
            }
            if (connectionTO.getDepartureDateTime().isAfter(bestArrivalTime)) {
                // Since connections are sorted, we can assume nothing better will come along now.
                // This is the "Stopping criterion" optimization from the paper.
                break;
            }
            ConnectionState departureState = stops.get(connectionTO.getDepartureUNLocationCode());
            if (departureState == null
                    || departureState.getEarliestArrivalDateTime() == MAX_DATE_TIME
                    || !departureState.getEarliestArrivalDateTime().isBefore(connectionTO.getDepartureDateTime())) {
                // We cannot reach this connection in time.  The X == MAX_DATE_TIME is strictly speaking redundant, but
                // it is a very common case and computationally cheaper than a proper comparison.
                continue;
            }
            ConnectionState arrivalState = stops.computeIfAbsent(connectionTO.getArrivalUNLocationCode(), (l) -> ConnectionState.of());
            if (connectionTO.getArrivalDateTime().isBefore(arrivalState.getEarliestArrivalDateTime())) {
                // We have found a faster route to this stop.
                arrivalState.setConnectionToHere(connectionTO);
                arrivalState.setEarliestArrivalDateTime(connectionTO.getArrivalDateTime());
                if (connectionTO.getArrivalUNLocationCode().equals(destinationUNLocationCode) && bestArrivalTime.isAfter(connectionTO.getArrivalDateTime())) {
                    bestArrivalTime = connectionTO.getArrivalDateTime();
                }
            }
        }
        if (bestArrivalTime != MAX_DATE_TIME) {
            return buildRoute(stops, destinationUNLocationCode);
        }
        return Collections.emptyList();
    }

    private static List<CSAConnection> buildRoute(Map<String, ConnectionState> stops, String destination) {
        List<CSAConnection> route = new ArrayList<>();
        CSAConnection connectionTO = stops.computeIfAbsent(destination, s -> { throw new AssertionError(s); })
                .getConnectionToHere();
        while (connectionTO != null) {
            route.add(connectionTO);
            ConnectionState state = stops.get(connectionTO.getDepartureUNLocationCode());
            assert state != null;
            connectionTO = state.getConnectionToHere();
        }
        Collections.reverse(route);
        return route;
    }

    private static boolean isSorted(List<? extends CSAConnection> data) {
        CSAConnection previous = null;
        for (CSAConnection t : data) {
            assert t != null;
            if (previous == null) {
                previous = t;
            } else if (ConnectionScanAlgorithm.BY_DEPARTURE_DATE_TIME.compare(previous, t) > 0) {
                return false;
            }
        }
        return true;
    }

    @Data(staticConstructor = "of")
    @RequiredArgsConstructor(staticName = "of")
    @AllArgsConstructor(staticName = "of")
    private static class ConnectionState {
        private ZonedDateTime earliestArrivalDateTime = MAX_DATE_TIME;
        private CSAConnection connectionToHere;

        public static ConnectionState of(ZonedDateTime earliestArrivalDateTime) {
            return of(earliestArrivalDateTime, null);
        }
    }
}
