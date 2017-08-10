import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Represents a text file containing a list of keys. These can be downloaded from the
 * common-crawl page at http://commoncrawl.org/2017/07/july-2017-crawl-archive-now-available/
 * 
 * @author marianne
 *
 */

public class S3KeyList {
	
	BufferedReader br;
	
	public S3KeyList(String file) throws FileNotFoundException, IOException {
		br = new BufferedReader(new FileReader(file));
	}
	
	public String nextKey() throws IOException {
		return br.readLine();
	}
}
