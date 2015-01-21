package controllers.util;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

public class CombinedJSONResponse {
	private List<JsonNode> responses;

	public CombinedJSONResponse(List<JsonNode> responses) {
		super();
		this.responses = responses;
	}

	public List<JsonNode> getResponses() {
		return responses;
	}
	
	
}
