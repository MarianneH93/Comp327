import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;
import com.google.gson.*;
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

	public static void main(String[] args) {
		try {
			S3KeyList watKeys = new S3KeyList("wat.paths");
			String key = watKeys.nextKey();
			System.out.println(key);

			AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withCredentials(new ProfileCredentialsProvider()).build();
			try {
				System.out.println("Downloading an object");
				S3Object s3object = s3Client.getObject(new GetObjectRequest(
						bucketName, key));
				System.out.println("Content-Type: "  + 
						s3object.getObjectMetadata().getContentType());

				// TODO: Save object locally
				InputStream in = s3object.getObjectContent();
				GZIPInputStream gzip = new GZIPInputStream(in);
				displayTextInputStream(gzip);
				//displayTextInputStream(s3object.getObjectContent());

				// Get a range of bytes from an object.

				GetObjectRequest rangeObjectRequest = new GetObjectRequest(
						bucketName, key);
				rangeObjectRequest.setRange(0, 10);
				S3Object objectPortion = s3Client.getObject(rangeObjectRequest);

				System.out.println("Printing bytes retrieved.");
				//displayTextInputStream(objectPortion.getObjectContent());

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


		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void displayTextInputStream(InputStream input) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));

		while (true) {
			String line = reader.readLine();
			if (line == null) {
				break;
			}
			//System.out.println(line);

			try {
				JsonElement jelement = new JsonParser().parse(line);
				if (!jelement.isJsonNull()) {
					JsonObject jobject = jelement.getAsJsonObject();
					//System.out.println("Created JSON object");
					JsonObject jsonEnvelope = jobject.getAsJsonObject("Envelope");
					JsonObject jsonPayload = jsonEnvelope.getAsJsonObject("Payload-Metadata");
					if (jsonPayload != null) {
						JsonObject jsonHttp = jsonPayload.getAsJsonObject("HTTP-Response-Metadata");
						if (jsonHttp != null) {
							JsonObject jsonHtml = jsonHttp.getAsJsonObject("HTML-Metadata");
							if (jsonHtml != null) {
								//System.out.println("Found HTML-Metadata");
								JsonArray jsonLinks = jsonHtml.getAsJsonArray("Links");
								if (jsonLinks != null) {
									//System.out.println("Found " + jsonLinks.size() + " links...");
									for (int i = 0; i < jsonLinks.size(); i++) {
										String link;
										JsonPrimitive linkPrimitive = jsonLinks.get(i).getAsJsonObject().getAsJsonPrimitive("url");
										if (linkPrimitive != null) {
											link = linkPrimitive.getAsString();

											if (link.contains(searchURL)) {

												String pageURL = jsonEnvelope.getAsJsonObject("WARC-Header-Metadata")
														.getAsJsonPrimitive("WARC-Target-URI").getAsString();

												String pageIP = jsonEnvelope.getAsJsonObject("WARC-Header-Metadata")
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
											}
										}
										//System.out.println(jsonLinks.get(i).getAsJsonObject().getAsJsonPrimitive("url"));
									}
								}
							}
						}
					}
				}
			}

			catch (JsonSyntaxException e) {
				//System.out.println("Skipping JSON for this line...");
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
