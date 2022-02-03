package org.dcsa.cs.model.routefinding;

import lombok.Data;
import org.dcsa.core.events.model.Vessel;
import org.dcsa.core.events.model.enums.DCSATransportType;

import java.time.ZonedDateTime;

// Used in testing because we do not need to bother with the complexity of Transport Calls.
@Data(staticConstructor = "of")
public class SimpleVesselCSAConnection implements CSAConnection {

    private final String departureUNLocationCode;
    private final String arrivalUNLocationCode;
    private final ZonedDateTime departureDateTime;
    private final ZonedDateTime arrivalDateTime;
    private final Vessel vessel;

    @Override
    public DCSATransportType getModeOfTransport() {
        return DCSATransportType.VESSEL;
    }
}
