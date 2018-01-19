package rnd.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class Utils {

	public static void printArray(double[] array) {
		for (int i = 0; i < array.length; i++) {
			System.out.println(i + ": " + array[i]);
		}
	}
	
	public static void printArray(Object[] array) {
		for (int i = 0; i < array.length; i++) {
			System.out.println(i + ": " + array[i]);
		}
	}

	public static String readContent(String fileName) {
		String content = "";
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
			String line = null;
			while ((line = br.readLine()) != null) {
				content += line + "\n";
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return content;

	}
}
