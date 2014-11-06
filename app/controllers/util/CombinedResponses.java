package controllers.util;

import java.util.List;

import play.libs.ws.WSResponse;

/**
 * Helper class to combine multiple WsResponses into a single Promise
 * @author Alexander C. Mueller
 *
 */
public  class CombinedResponses {
	private List<WSResponse> responses;

	public CombinedResponses(List<WSResponse> responses) {
		super();
		this.responses = responses;
	}

	public List<WSResponse> getResponses() {
		return responses;
	}

}