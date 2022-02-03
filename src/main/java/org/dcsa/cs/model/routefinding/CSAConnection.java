package org.dcsa.cs.model.routefinding;

import org.dcsa.core.events.model.Vessel;
import org.dcsa.core.events.model.enums.DCSATransportType;

import java.time.ZonedDateTime;

/**
 * A connection as defined by the Connection Scan Algorithm
 * (the CSA prefix was chosen to ensure it would not be "Connection", which is used a lot
 * in different contexts).
 *
 * Crucially, connection are always between two direct stops.  I.e., if there is a connection
 * between "DEHAM" and "NTRLM", then it implies that the transport does not have any stops
 * between "DEHAM" and "NTRLM".
 */
public interface CSAConnection {

    /**
     * @return The place where the cargo will be picked up from (or has to be) if this connection is used.
     */
    String getDepartureUNLocationCode();

    /**
     * @return The place where the cargo will move to if this connection is used. Note that
     * it will not necessarily be unloaded there (it can stay on the same transport for
     * several stops)
     */
    String getArrivalUNLocationCode();

    /**
     * @return When will the transport depart (cargo already be loaded in the transport before
     * this time for the connection to be available).
     */
    ZonedDateTime getDepartureDateTime();

    /**
     * @return When will the transport arrive if this connection is used
     */
    ZonedDateTime getArrivalDateTime();

    /**
     * @return The mode of transport for this connection
     */
    DCSATransportType getModeOfTransport();

    /**
     *
     * @return The vessel (if present) for this connection.
     */
    Vessel getVessel();
}
