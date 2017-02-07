import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;

public class TokenizerTest {

	public static void main(String[] args) throws IOException {

		String s = "An input sample sentence.";

		Tokenizer tokenizer = getTokenizer();
		
		String tokens[] = tokenizer.tokenize(s);
		Utils.printArray(tokens);

		Span tokenSpans[] = tokenizer.tokenizePos(s);
		Utils.printArray(tokenSpans);
		
	}

	public static Tokenizer getTokenizer() throws FileNotFoundException, IOException {
		InputStream modelIn = new FileInputStream("model/en-token.bin");
		TokenizerModel model = new TokenizerModel(modelIn);
		Tokenizer tokenizer = new TokenizerME(model);
		return tokenizer;
	}

}
