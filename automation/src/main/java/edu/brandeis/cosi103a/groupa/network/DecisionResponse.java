package edu.brandeis.cosi103a.groupa.network;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.brandeis.cosi.atg.decisions.Decision;

public record DecisionResponse(@JsonProperty("decision") Decision decision) {
}
