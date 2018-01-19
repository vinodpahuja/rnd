package rnd.ai.nlp.opennlp;

import java.io.FileInputStream;
import java.io.InputStream;

import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import rnd.util.Utils;

public class ChunkerTest {

	public static void main(String[] args) throws Exception {

		InputStream modelIn = new FileInputStream(Constants.BASE_PATH + "model/en-chunker.bin");
		ChunkerModel model = new ChunkerModel(modelIn);

		ChunkerME chunker = new ChunkerME(model);

		String s = "Victor Smith is very popular. He is very smart too.";
		System.out.println(s);

		String tokens[] = TokenizerTest.getTokenizer().tokenize(s);
		// Utils.printArray(tokens);

		String pos[] = POSTaggerTest.getPOSTagger().tag(tokens);
		// Utils.printArray(pos);

		String tags[] = chunker.chunk(tokens, pos);
		Utils.printArray(tags);

	}
}
