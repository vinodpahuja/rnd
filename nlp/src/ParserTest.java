import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.cmdline.parser.ParserTool;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;

public class ParserTest {

	public static void main(String[] args) throws Exception {
		
		Parser parser = getParser();
		
		String s = "Victor Smith is very popular. He is very smart too.";
		System.out.println(s);
		
		Parse topParses[] = ParserTool.parseLine(s, parser, 1);
		Utils.printArray(topParses);
		
	}

	private static Parser getParser() throws FileNotFoundException, IOException {
		InputStream modelIn = new FileInputStream(Constants.BASE_PATH + "model/en-parser-chunking.bin");
		ParserModel model = new ParserModel(modelIn);
		Parser parser = ParserFactory.create(model);
		return parser;
	}
}
