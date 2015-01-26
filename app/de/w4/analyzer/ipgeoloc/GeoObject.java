package de.w4.analyzer.ipgeoloc;

/**
 * 
 * Class representing a Geo Object Extracted by the GeoIp Database
 * 
 * @author Alexander C. Mueller
 *
 */
public class GeoObject {
	private double lati;
	private double longi;

	private String countryCode;

	public GeoObject(double lati, double longi, String countryCode) {
		super();
		this.lati = lati;
		this.longi = longi;
		this.countryCode = countryCode;
	}

	public double getLati() {
		return lati;
	}

	public double getLongi() {
		return longi;
	}

	public String getCountryCode() {
		return countryCode;
	}

	@Override
	public String toString() {
		return "GeoObject [lati=" + lati + ", longi=" + longi
				+ ", countryCode=" + countryCode + "]";
	}
	

}
