import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

public class TestDrive {

	private static String CLIENT_ID = "270823887160-0v01oeg4k8nc5urpseo4c8aimrl35522.apps.googleusercontent.com";
	private static String CLIENT_SECRET = "B1nMqEcYiORuzeqm0YrZnVa2";

	private static String REDIRECT_URI = "https://anonymous-confessions.appspot.com/oauth2callback";

	public static void main(String[] args) throws IOException {

		HttpTransport httpTransport = new NetHttpTransport();
		JsonFactory jsonFactory = new JacksonFactory();

		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				httpTransport, jsonFactory, CLIENT_ID, CLIENT_SECRET,
				Arrays.asList(DriveScopes.DRIVE)).setAccessType("offline")
				.setApprovalPrompt("force").build();

		String url = flow.newAuthorizationUrl().setRedirectUri(REDIRECT_URI)
				.build();
		System.out
				.println("Please open the following URL in your browser then type the authorization code:");
		System.out.println("  " + url);

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String code = br.readLine();

		GoogleTokenResponse response = flow.newTokenRequest(code)
				.setRedirectUri(REDIRECT_URI).execute();

		// GoogleCredential credential = new
		// GoogleCredential().setFromTokenResponse(response);

		Credential credential = flow.createAndStoreCredential(response, null);

		// GoogleCredential credential = new
		// GoogleCredential().setAccessToken(code);

		// Create a new authorized API client
		Drive drive = new Drive.Builder(httpTransport, jsonFactory, credential)
				.setApplicationName("Confessions").build();

		System.out.println("children" + drive.children().get("My Drive", "vinodpahuja.png").execute());

		// // Insert a file
		// File body = new File();
		// body.setTitle("My document");
		// body.setDescription("A test document");
		// body.setMimeType("text/plain");
		//
		// java.io.File fileContent = new java.io.File("document.txt");
		// FileContent mediaContent = new FileContent("text/plain",
		// fileContent);

		// File file = service.files().insert(body, mediaContent).execute();
		// System.out.println("File ID: " + file.getId());
	}
}