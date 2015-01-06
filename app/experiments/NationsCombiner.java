package experiments;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NationsCombiner {

	public static void main(String[] args) throws IOException {
		final Map<String, String> final_data = new HashMap<String, String>();
		File f1 = new File("public/data/un_nations_iso.txt");

		Files.readAllLines(f1.toPath()).forEach(line -> {
			String[] elements = line.split(";");
			if (elements.length > 1) {
				final_data.put(elements[0], elements[1]);
				//System.out.println(elements[0]);
			}
		});
		
		File f2 = new File("public/data/iso_codes.txt");
		
	
		Files.readAllLines(f2.toPath(),	Charset.forName("UTF-16")).forEach(line -> {
	
			String[] elements = line.split(";");
			if (elements.length > 0) {
				final_data.put(elements[0], elements[1]);
			}
		});
		
		List<String> final_file = new ArrayList<String>();
		
		final_data.forEach((key,value) -> {
		
			final_file.add(key+";"+value);
		});
		
		File combined_file = new File("public/data/combined_iso.txt");
		combined_file.createNewFile();

		Files.write(combined_file.toPath(), final_file, Charset.forName("UTF-8"), StandardOpenOption.TRUNCATE_EXISTING);
		

				
		
	
	}

}
