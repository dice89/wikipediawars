import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.client.ClientProtocolException;
import org.junit.Test;

import com.maxmind.geoip2.exception.GeoIp2Exception;

import de.w4.analyzer.ipgeoloc.IPLocExtractor;


/**
*
* Simple (JUnit) tests that can call all parts of a play app.
* If you are interested in mocking a whole application, see the wiki for more details.
*
*/
public class ApplicationTest {


    @Test
    public void geoIpCheck() throws ClientProtocolException, URISyntaxException, IOException, GeoIp2Exception{
    	IPLocExtractor ip = new IPLocExtractor();
    	
    	String code = ip.getGeoLocationForIP("134.106.98.44").getCountryCode();
    	assertEquals("ENG",code);
    }


}
