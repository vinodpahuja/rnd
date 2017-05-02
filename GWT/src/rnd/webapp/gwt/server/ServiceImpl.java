package rnd.webapp.gwt.server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import rnd.webapp.gwt.client.Language;
import rnd.webapp.gwt.client.Service;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class ServiceImpl extends RemoteServiceServlet implements Service {

	final public static Language ENGLISH = new Language("en", "English");

	final public static Language HINDI = new Language("hi", "Hindi");

	final static Map langMap = new HashMap();

	static {
		langMap.put(ENGLISH.getCode(), ENGLISH);
		langMap.put(HINDI.getCode(), HINDI);
	}

	public String sayHello(Language lang) {
		System.out.println("lang:" + lang);
		
//		if(lang instanceof PersistenceCapable){
//			System.out.println("lang is PC");
//		}

		if (lang.getLanguage().equalsIgnoreCase("Hindi")) {
			return "Namaskar";
		} else if (lang.getLanguage().equalsIgnoreCase("English")) { return "Hello"; }
		return null;
	}

	public Language getLanguage(String code) {
		Language lang = (Language) langMap.get(code);
		System.out.println("lang:" + lang);
		return lang;
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		System.out.println("req:" + req.getContextPath());
		System.out.println("path:" + req.getMethod());

		System.out.println("resp:" + resp);

		super.service(req, resp);
	}
}
