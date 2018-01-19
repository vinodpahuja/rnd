package rnd.ai.nlp.corenlp;

import java.util.List;
import java.util.Properties;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class ParserTest {

	public static void main(String[] args) throws Exception {

		String s = "Charlie is working as Software Engineer in CenturyLink India Pvt. Ltd., Bangalore from October, 2014 to till date.";
		System.out.println(s);

		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma, ner");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

		Annotation document = new Annotation(s);
		pipeline.annotate(document);

		List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

		boolean inEntity = false;
		String currentEntity = null;
		String currentEntityType = null;

		for (CoreMap sentence : sentences) {
			for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
				String ne = token.get(CoreAnnotations.NamedEntityTagAnnotation.class);
				
				if (!inEntity) {
					if (!"O".equals(ne)) {
						inEntity = true;
						currentEntity = "";
						currentEntityType = ne;
					}
				}
				if (inEntity) {
					if ("O".equals(ne)) {
						inEntity = false;
						switch (currentEntityType) {
						case "PERSON":
							System.out.println("Extracted Person - " + currentEntity.trim());
							break;
						case "ORGANIZATION":
							System.out.println("Extracted Organization - " + currentEntity.trim());
							break;
						case "LOCATION":
							System.out.println("Extracted Location - " + currentEntity.trim());
							break;
						case "DATE":
							System.out.println("Extracted Date " + currentEntity.trim());
							break;
						}
					} else {
						currentEntity += " " + token.originalText();
					}
				}
			}
		}
	}

}
