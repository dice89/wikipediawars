package de.w4.analyzer.ipgeoloc;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URISyntaxException;

import org.apache.http.client.ClientProtocolException;

import play.Play;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;

/**
 * 
 * This is a helper class that implements and IP address geo look up, by
 * facilitating GeoLite2 Database by MaxMind
 * 
 * This product includes GeoLite2 data created by MaxMind, available from
 * http://www.maxmind.com
 * 
 * @author Alexander C. Mueller
 *
 */
public class IPLocExtractor {

	private DatabaseReader reader;
	// probably move to s3
	private static final String GEO_DB_LOC = "public/data/GeoLite2-City.mmdb";

	public IPLocExtractor() throws IOException {
		super();
		InputStream stream = Play.application().classloader().getResourceAsStream(GEO_DB_LOC);
		reader = new DatabaseReader.Builder(stream).build();
	}

	/**
	 * 
	 * Returns a geo location for a given IP address, or simply an host name
	 * 
	 * @param ip
	 * @return
	 * @throws URISyntaxException
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws GeoIp2Exception
	 */
	public GeoObject getGeoLocationForIP(String ip) throws URISyntaxException,
			ClientProtocolException, IOException, GeoIp2Exception {

		InetAddress ipAddress = InetAddress.getByName(ip);
		CityResponse response = reader.city(ipAddress);

		String country_code = response.getCountry().getIsoCode();
		
	
		GeoObject geobj = new GeoObject(response.getLocation().getLatitude(),
				response.getLocation().getLongitude(), country_code);

		return geobj;
	}

}
