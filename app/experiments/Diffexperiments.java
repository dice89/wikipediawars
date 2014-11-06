package experiments;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Diffexperiments {
	public static void main(String args[]) throws FileNotFoundException{
		
		String  html =  new Scanner(new File("test1.html")).useDelimiter("\\Z").next();
		html = html.replace("\\", "");
		Document doc = Jsoup.parse(html);
		Elements elements = doc.select(".diffchange-inline");
		System.out.println(elements.size());
		for (Element element : elements) {
			System.out.println(element.nodeName());
			System.out.println(element.html());
		}
	}
}
