import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

public class POSTaggerTest {

	public static void main(String[] args) throws Exception {
		
		String s = "Victor Smith is very popular. He is very smart too.";
		System.out.println(s);
		
		String tokens[] = TokenizerTest.getTokenizer().tokenize(s);
		//Utils.printArray(tokens);
		
		POSTaggerME tagger = getPOSTagger();
		String tags[] = tagger.tag(tokens);
		Utils.printArray(tags);
		
		double[] probs = tagger.probs();
		Utils.printArray(probs);
		
	}

	public static POSTaggerME getPOSTagger() throws FileNotFoundException, IOException {
		InputStream modelIn = new FileInputStream(Constants.BASE_PATH + "model/en-pos-maxent.bin");
		POSModel model = new POSModel(modelIn);
		POSTaggerME tagger = new POSTaggerME(model);
		return tagger;
	}
}
