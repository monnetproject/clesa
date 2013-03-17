package eu.monnetproject.clesa.ontology;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import eu.monnetproject.clesa.CLESA;
import eu.monnetproject.config.Configurator;
import eu.monnetproject.label.LabelExtractor;
import eu.monnetproject.label.LabelExtractorFactory;
import eu.monnetproject.lang.Language;
import eu.monnetproject.lang.Script;
import eu.monnetproject.ontology.Entity;
import eu.monnetproject.ontology.Ontology;
//import eu.monnetproject.tokenizer.Tokenizer;
//import eu.monnetproject.tokenizer.TokenizerFactory;
//import eu.monnetproject.tokens.Token;
import eu.monnetproject.translation.TranslationRanker;
import eu.monnetproject.translation.TranslationRankerFactory;
import eu.monnetproject.translation.monitor.Messages;
import eu.monnetproject.translation.TokenizerFactory;
import eu.monnetproject.translation.Tokenizer;


public class CLESARankerFactory implements TranslationRankerFactory {

	private CLESA clesa;

	protected final TokenizerFactory tokenizerFactory;
	protected final LabelExtractorFactory labelExtractorFactory;

	public CLESARankerFactory(TokenizerFactory tokenizerFactory, LabelExtractorFactory labelExtractorFactory) {
		this.tokenizerFactory = tokenizerFactory;
		this.labelExtractorFactory = labelExtractorFactory;
	}

	@Override
	public TranslationRanker getRanker(Ontology ontology, Language srcLang,
			Language trgLang) {
		final Properties config = Configurator.getConfig("eu.monnetproject.clesa.ontology.CLESARanker");
		if(config.containsKey(srcLang.getIso639_1()) && config.containsKey(trgLang.getIso639_1())) {
			Boolean srcLangSupported = Boolean.parseBoolean(config.getProperty(srcLang.getIso639_1()));
			Boolean trgLangSupported = Boolean.parseBoolean(config.getProperty(trgLang.getIso639_1()));
			if(srcLangSupported && trgLangSupported) {
				try {
					String ontoDoc = null;
					if(Boolean.parseBoolean(config.getProperty("wholeOntologyAsContext")))
						ontoDoc = onto2doc(ontology, srcLang);
					clesa  = new CLESA(config);
					return new CLESARanker(clesa, ontoDoc, srcLang, trgLang, config);			
				} catch(Exception x) {
					//log.stackTrace(x);
					return null;
				}
			} else {
				Messages.warning("No CLESA ranker available: not for this language pair ("+srcLang+","+trgLang+")");
                        }
		} else {
                    Messages.warning("No CLESA ranker available: not configured");
                }
		return null;
	}

	@Override
	public TranslationRanker getRanker(Ontology ontology, Language srcLang,
			Language trgLang, Set<Language> extraLanguages) {
		return getRanker(ontology, srcLang, trgLang);
	}

	protected String onto2doc(Ontology ontology, Language srcLang) {
		Set<String> ontoDocTokens = new HashSet<String>();
		StringBuffer ontoDoc = new StringBuffer();
		final Tokenizer tokenizer = getTokenizer(srcLang);
		@SuppressWarnings("unchecked")
		final LabelExtractor extractor = labelExtractorFactory.getExtractor(Collections.EMPTY_LIST, true, false);
		if (extractor == null) {
			Messages.severe("No label extractor");
		}
		final HashSet<URI> puns = new HashSet<URI>();
		for (Entity entity : ontology.getEntities()) {
			if (entity.getURI() == null || puns.contains(entity.getURI())) {
				continue;
			}
			puns.add(entity.getURI());
			final Map<Language, Collection<String>> labels = extractor.getLabels(entity);
			if (labels.containsKey(srcLang)) {
				for (String label : labels.get(srcLang)) {
					for (String token : tokenizer.tokenize(label.toLowerCase())) {
						ontoDocTokens.add(token);				
					}
				}
			}
		}	
		int i = 0;
		for(String token : ontoDocTokens) {
			if(i++ != 2500) 
				ontoDoc.append(token + " ");	
		}
		return ontoDoc.toString().trim();
	}

	protected Tokenizer getTokenizer(Language lang) {
		Script script = Script.LATIN;
		final Script[] knownScriptsForLanguage = Script.getKnownScriptsForLanguage(lang);
		if (knownScriptsForLanguage.length > 0) {
			script = knownScriptsForLanguage[0];
		}
		return tokenizerFactory.getTokenizer(script);
	}

}


