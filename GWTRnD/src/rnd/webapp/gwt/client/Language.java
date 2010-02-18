package rnd.webapp.gwt.client;

import java.io.Serializable;

public class Language implements Serializable {

	String language;

	String code;

	public Language() {
	}

	public Language(String code, String language) {
		this.code = code;
		this.language = language;
	}

	public String getLanguage() {
		return this.language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getCode() {
		return this.code;
	}

	public void setCode(String code) {
		this.code = code;
	}
}
