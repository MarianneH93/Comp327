import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.google.gson.stream.JsonWriter;

public class DatabaseEntry {

	String pageUrl;
	String pageIP;
	String linkText;
	String linkUrl;

	/**
	 * Creates a new database entry instance with the URLs and IP address of the
	 * page.
	 * @param pageUrl
	 * @param pageIP
	 * @param linkUrl
	 */
	public DatabaseEntry(String pageUrl, String pageIP, String linkUrl) {
		this.pageUrl = pageUrl;
		this.pageIP = pageIP;
		this.linkUrl = linkUrl;
	}
	
	/**
	 * Set link text. Not all links have text so this was left out of the constructor.
	 * @param linkText
	 */
	public void setLinkText(String linkText) {
		this.linkText = linkText;
	}

	public void storeInDatabase(Table table) {
		Item item = new Item()
				.withPrimaryKey("url", pageUrl)
				.withPrimaryKey("linkurl", linkUrl)
				.withString("pageIP", pageIP)
				.withString("linktext", linkText);
		PutItemOutcome outcome = table.putItem(item);
	}
	
	public String createCSVLine() {
		String line = pageUrl + "," + linkUrl + "," + pageIP + "," + linkText;
		return line;
	}
	
	public void createJSON(JsonWriter writer) throws IOException {
		writer.beginObject();
		writer.name("pageURL").value(pageUrl);
		writer.name("linkURL").value(linkUrl);
		writer.name("pageIP").value(pageIP);
		writer.name("linkText").value(linkText);
		writer.endObject();
	}

}
