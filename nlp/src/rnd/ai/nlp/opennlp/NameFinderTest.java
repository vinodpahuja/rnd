package rnd.ai.nlp.opennlp;

import java.io.FileInputStream;
import java.io.InputStream;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;
import rnd.util.Utils;

public class NameFinderTest {

	public static void main(String[] args) throws Exception {

		InputStream modelIn = new FileInputStream(Constants.BASE_PATH + "train/en-ner-person.bin");
		TokenNameFinderModel model = new TokenNameFinderModel(modelIn);

		NameFinderME nameFinder = new NameFinderME(model);

		String s = "Victor Smith is very popular. He is very smart too.";
		// String s = Utils.readContent(Constants.BASE_PATH + "train/en-sent.train");
		System.out.println(s);
		String tokens[] = TokenizerTest.getTokenizer().tokenize(s);
		Utils.printArray(tokens);

		Span nameSpans[] = nameFinder.find(tokens);
		Utils.printArray(nameSpans);
		nameFinder.clearAdaptiveData();

	}
	/*
	 * public static void main(String[] args) throws IOException {
	 * 
	 * Charset charset = Charset.forName("UTF-8"); ObjectStream<String> lineStream =
	 * new PlainTextByLineStream(new InputStreamFactory() { public InputStream
	 * createInputStream() throws IOException { return new
	 * FileInputStream(Constants.BASE_PATH + "train/en-ner-person.train"); } },
	 * charset); ObjectStream<NameSample> sampleStream = new
	 * NameSampleDataStream(lineStream);
	 * 
	 * TokenNameFinderModel model;
	 * 
	 * try { model = NameFinderME.train("en", "person", sampleStream,
	 * TrainingParameters.defaultParams(), new TokenNameFinderFactory()); } finally
	 * { sampleStream.close(); }
	 * 
	 * OutputStream modelOut = null;
	 * 
	 * try { modelOut = new BufferedOutputStream(new
	 * FileOutputStream(Constants.BASE_PATH + "train/en-ner-person.bin"));
	 * model.serialize(modelOut); } finally { if (modelOut != null)
	 * modelOut.close(); } }
	 */
}
