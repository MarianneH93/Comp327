import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import com.google.gson.*;
import com.google.gson.stream.JsonWriter;
import com.google.gson.stream.MalformedJsonException;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;

public class EC2MainProcess {
	static String bucketName = "commoncrawl";
	static ArrayList jsonArray = new ArrayList();
	static String searchURL = "youtube.com";
	
	static JsonWriter out;
	
	// Sample sizes
	static int numberFound = 0;
	static int maxNumber = 1000;

	public static void main(String[] args) {
		try {
			// Load list of .wat keys
			S3KeyList watKeys = new S3KeyList("wat.paths");

			// Set up common crawl S3 client
			AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
					.withCredentials(new ProfileCredentialsProvider())
					.build();
			
			out = new JsonWriter(new FileWriter("out"));
			out.beginArray();

			String currentKey;
			while ((currentKey = watKeys.nextKey()) != null && numberFound < maxNumber) {
				System.out.println("Obtaining new WAT file: " + currentKey);
				try {
					S3Object s3object = s3Client.getObject(new GetObjectRequest(
							bucketName, currentKey));

					// Handle object stream
					InputStream in = s3object.getObjectContent();
					GZIPInputStream gzip = new GZIPInputStream(in);
					processWATStream(gzip);

				} catch (AmazonServiceException ase) {
					System.out.println("Caught an AmazonServiceException, which" +
							" means your request made it " +
							"to Amazon S3, but was rejected with an error response" +
							" for some reason.");
					System.out.println("Error Message:    " + ase.getMessage());
					System.out.println("HTTP Status Code: " + ase.getStatusCode());
					System.out.println("AWS Error Code:   " + ase.getErrorCode());
					System.out.println("Error Type:       " + ase.getErrorType());
					System.out.println("Request ID:       " + ase.getRequestId());
				} catch (AmazonClientException ace) {
					System.out.println("Caught an AmazonClientException, which means"+
							" the client encountered " +
							"an internal error while trying to " +
							"communicate with S3, " +
							"such as not being able to access the network.");
					System.out.println("Error Message: " + ace.getMessage());
				}
			}
			
			// Close JSON file properly
			out.endArray();
			out.close();


		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void processWATStream(InputStream input) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));

		while (true) {
			String line = reader.readLine();
			if (line == null) {
				break;
			}

			try {

				// Parse lines as JSON elements
				JsonElement topJsonElement = new JsonParser().parse(line);

				if (!topJsonElement.isJsonNull()) {
					JsonObject jsonPayload = topJsonElement.getAsJsonObject()
							.getAsJsonObject("Envelope")
							.getAsJsonObject("Payload-Metadata");

					// Don't continue if no payload included.
					// Experience tells me next JSON elements are sometimes not included.
					if (jsonPayload == null) {
						continue;
					}

					JsonObject jsonHttp = jsonPayload.getAsJsonObject("HTTP-Response-Metadata");

					if (jsonHttp == null) {
						continue;
					}

					JsonObject jsonHtml = jsonHttp.getAsJsonObject("HTML-Metadata");

					if (jsonHtml == null) {
						continue;
					}

					// Get array of links
					JsonArray jsonLinks = jsonHtml.getAsJsonArray("Links");

					if (jsonLinks == null) {
						continue;
					}

					for (int i = 0; i < jsonLinks.size(); i++) {
						String link;
						JsonPrimitive linkPrimitive = jsonLinks.get(i).getAsJsonObject().
								getAsJsonPrimitive("url");
						
						if (linkPrimitive != null) {
							link = linkPrimitive.getAsString();

							// Check each link url for our domain
							if (link.contains(searchURL)) {

								// Get the other data we want if we found the URL
								String pageURL = topJsonElement.getAsJsonObject()
										.getAsJsonObject("Envelope")
										.getAsJsonObject("WARC-Header-Metadata")
										.getAsJsonPrimitive("WARC-Target-URI").getAsString();

								String pageIP = topJsonElement.getAsJsonObject()
										.getAsJsonObject("Envelope")
										.getAsJsonObject("WARC-Header-Metadata")
										.getAsJsonPrimitive("WARC-IP-Address").getAsString();

								DatabaseEntry dbentry = new DatabaseEntry(pageURL, pageIP, link);

								JsonPrimitive linkTextPrimitive = jsonLinks.get(i).getAsJsonObject().getAsJsonPrimitive("text");
								if (linkTextPrimitive != null) {
									dbentry.setLinkText(linkTextPrimitive.getAsString());
									System.out.println("Link text: " + linkTextPrimitive.getAsString());
								}

								System.out.println("Found link at: " + pageURL);
								System.out.println("IP: " + pageIP);
								System.out.println("Links to: " + link);
								
								dbentry.createJSON(out);
								
								// Check if enough samples were found.
								numberFound++;
								System.out.println(numberFound);
								if (numberFound > maxNumber) {
									return;
								}
								
							}
						}
					}
				}
			}

			catch (JsonSyntaxException e) {
			}


			/*String[] array = line.split(":");
            jsonArray.clear();
            for (String s : array) {
                String[] temp = s.split(",");
                for(String t : temp) {
                    jsonArray.add(t);
                }
            }
			 */

			//String primaryKey = jsonArray.get(9) + ":" + jsonArray.get(10);
			//String timestamp = jsonArray.get(5).toString();
			//String urlKey = jsonArray.get(1) + "," + jsonArray.get(2) + "," + jsonArray.get(3);
			/*Item item = new Item()
                    .withPrimaryKey("url", primaryKey)
                    .withString("timestamp", timestamp)
                    .withString("urlkey", urlKey)
                    .withString("status", jsonArray.get(7).toString())
                    .withString("filename", jsonArray.get(12).toString())
                    .withString("length", jsonArray.get(14).toString())
                    .withString("mime", jsonArray.get(16).toString())
                    .withString("mime-detected", jsonArray.get(18).toString())
                    .withString("offset", jsonArray.get(20).toString())
                    .withString("digest", jsonArray.get(22).toString());
                PutItemOutcome outcome = table.putItem(item);
			 */

		}

	}
	/*
        int size = jsonArray.size();
        for(int i =0; i <size; i++) {
            System.out.println("**********************************************");
            System.out.println("**********************************************");
            System.out.println(jsonArray.get(i));
            System.out.println("**********************************************");
            System.out.println("**********************************************");
        }
	 */
}
