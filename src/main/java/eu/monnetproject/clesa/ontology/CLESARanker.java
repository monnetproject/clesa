package eu.monnetproject.clesa.ontology;

import java.util.Properties;

import eu.monnetproject.clesa.CLESA;
import eu.monnetproject.clesa.commons.utils.Pair;
import eu.monnetproject.clesa.commons.utils.Vector;
import eu.monnetproject.lang.Language;
import eu.monnetproject.ontology.Entity;
import eu.monnetproject.translation.PhraseTableEntry;
import eu.monnetproject.translation.TranslationRanker;

public class CLESARanker implements TranslationRanker {

	private CLESA clesa;	
	private Language srcLang;
	private Language trgLang;
	private String ontoDoc;
	private Properties config;
	private Vector<String> ontoVector;
	private Boolean nearByTermsAsContext = false;
	private boolean onlyChunkAsContext = false;
	private boolean sourceLabelAsContext = false;
		
	public CLESARanker(CLESA clesa, String ontoDoc, Language srcLang, Language trgLang, Properties config) {
		this.clesa = clesa;
		this.ontoDoc = ontoDoc;
		this.srcLang = srcLang;
		this.trgLang = trgLang;
		this.config = config;
		nearByTermsAsContext = Boolean.parseBoolean(this.config.getProperty("nearByTermsAsContext"));
		onlyChunkAsContext = Boolean.parseBoolean(this.config.getProperty("onlyChunkAsContext"));
		sourceLabelAsContext = Boolean.parseBoolean(this.config.getProperty("sourceLabelAsContext"));		
		if(ontoDoc!=null) 
			setOntoDocVector();
	}

	private void setOntoDocVector() {
		ontoVector = clesa.getVector(new Pair<String, Language>(ontoDoc, srcLang));	
	}
	
	@Override
	public double score(PhraseTableEntry entry, Entity entity) {
		String candidate = entry.getTranslation().asString();
		String srcString = null;
	
		if(ontoDoc!=null)
			srcString = ontoDoc;

		if(nearByTermsAsContext) 
			srcString = entity.toString() ;				
		if(onlyChunkAsContext) 
			srcString = entry.getForeign().asString();				
		if(sourceLabelAsContext) 
			srcString = entity.toString();				
	
		
		if(srcString.equalsIgnoreCase(ontoDoc))		
			return clesa.scoreAgainstVector(new Pair<String, Language>(candidate, trgLang), ontoVector);
		else 
			return clesa.score(new Pair<String, Language>(candidate, trgLang), 
					new Pair<String, Language>(srcString, srcLang));
	}

	@Override
	public String getName() {	
		return "CLESA";
	}

	@Override
	public void close() {
		clesa.close();		
	}

}
