package rnd.ai.nlp.opennlp;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.Span;

public class SentenceDetectorTest {

	public static void main(String[] args) throws InvalidFormatException, IOException {

		InputStream modelIn = new FileInputStream(Constants.BASE_PATH + "model/en-sent.bin");
		SentenceModel model = new SentenceModel(modelIn);
		SentenceDetectorME sentenceDetector = new SentenceDetectorME(model);

		String sentences[] = sentenceDetector.sentDetect("  First sentence. Second sentence. ");
		for (int i = 0; i < sentences.length; i++) {
			System.out.println(i + ": " + sentences[i]);
		}

		Span spans[] = sentenceDetector.sentPosDetect("  First sentence. Second sentence. ");
		for (int i = 0; i < spans.length; i++) {
			System.out.println(i + ": " + spans[i]);
		}

	}

	/*
	 * public static void main(String[] args) throws IOException {
	 * 
	 * Charset charset = Charset.forName("UTF-8"); ObjectStream<String> lineStream =
	 * new PlainTextByLineStream(new InputStreamFactory() { public InputStream
	 * createInputStream() throws IOException { return new
	 * FileInputStream(Constants.BASE_PATH + "train/en-sent2.train"); } }, charset);
	 * ObjectStream<SentenceSample> sampleStream = new
	 * SentenceSampleStream(lineStream);
	 * 
	 * SentenceModel model;
	 * 
	 * try { model = SentenceDetectorME.train("en", sampleStream, true, null,
	 * TrainingParameters.defaultParams()); } finally { sampleStream.close(); }
	 * 
	 * OutputStream modelOut = null; try { modelOut = new BufferedOutputStream(new
	 * FileOutputStream(Constants.BASE_PATH + "train/en-sent2.bin"));
	 * model.serialize(modelOut); } finally { if (modelOut != null)
	 * modelOut.close(); }
	 * 
	 * }
	 */
}
