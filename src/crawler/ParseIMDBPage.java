package crawler;

import org.jsoup.safety.Whitelist;  
import org.jsoup.select.Elements;  
import org.jsoup.nodes.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

public class ParseIMDBPage {

	String HTMLtext;
	
	/**
	 * Constructor
	 */
	ParseIMDBPage(String text)
	{
		HTMLtext = text;
		
	}
	
	/**
	 * This method is used to parse the html page we retreived from idmb
	 * @param movieJson, the movie json object get from omdb
	 * @return JSONObject, the JSONobject after combine the data from omdb and imdb
	 */
	public JSONObject parse(JSONObject movieJson)
	{
		Document doc = Jsoup.parse(HTMLtext);
		Element infobar = doc.select("div.infobar").first();
		
		//Get Time
		if(!movieJson.containsKey("Runtime")){
			Element time = doc.select("time").first();
			if(time != null)
			{
				movieJson.put("runtime", time.text());
			}
		}
		
		//crawler the Genre and release Date
		Elements test = infobar.select("a[href]");
		Iterator<Element> it = test.listIterator();
		JSONArray GenreJSON = new JSONArray();
		while(it.hasNext())
		{
			Element e = it.next();
			String text = e.text();
			if(text.length() > 10)
			{	//The text is released date	
				if(!movieJson.containsKey("Released")){
					movieJson.put("Released", text);
				}
			}
			else
			{
				if(!movieJson.containsKey("Genre")){
					GenreJSON.add(text);
				}
			}			
		}
		movieJson.put("Genre",GenreJSON);
		
		//
		//Get the rate
		//
		if(!movieJson.containsKey("imdbRating")){
			Element rate = doc.select("div.titlePageSprite.star-box-giga-star").first();
			if(rate != null){
				movieJson.put("imdbRating", rate.text());
			}
		}
		
		//
		//Get the plot
		//
		if(!movieJson.containsKey("Plot")){
			Element plot = doc.select("p[itemprop=description]").first();
			if(plot != null){
				movieJson.put("Plot", plot.text());
			}
		}	
		
		//
		//Get the Full Cast & Crew by request another page
		//
		try{
		Thread.currentThread().sleep(2000);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		JSONObject result = getFullCast(movieJson);
		return result;
	}
	
	/**
	 * 
	 */
	public JSONObject getFullCast(JSONObject movieJSON)
	{
		JSONObject fullInfo = movieJSON;
		String id = (String)movieJSON.get("imdbID");
		InputStream is = null;
		BufferedReader br;
		String line;
		String HTMLtext="";
		try {
			URL u = new URL("http://www.imdb.com/title/" + id +"/fullcredits?ref_=tt_ov_dr#directors");
			is = u.openStream(); // throws an IOException
			br = new BufferedReader(new InputStreamReader(is,"utf-8"));
			while ((line = br.readLine()) != null) {
				HTMLtext += line;
			}
		} 
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		//parse the page to find directors
		Document doc = Jsoup.parse(HTMLtext);
		Elements infoTable = doc.select("table.simpleTable");
		Element directorBar = infoTable.get(0);
		Elements names = directorBar.select("a[href]");
		Iterator<Element> itd = names.listIterator();
		JSONArray directorJSON = new JSONArray();
		while(itd.hasNext())
		{
			Element director = itd.next();
			String url = director.attr("href");
			JSONObject direJSON = parsePeople(url);
			direJSON.put("name", director.text());
			directorJSON.add(direJSON);
		}
		fullInfo.put("Director", directorJSON);
		
		//parse the page to find writers
		Element writerBar = infoTable.get(1);
		Elements wName = directorBar.select("a[href]");
		Iterator<Element> itw = wName.listIterator();
		JSONArray writerJSON = new JSONArray();
		while(itw.hasNext())
		{
			Element writer = itw.next();
			String url = writer.attr("href");
			JSONObject writJSON = parsePeople(url);
			writJSON.put("name", writer.text());
			writerJSON.add(writJSON);
		}
		fullInfo.put("Writer",writerJSON);
		//parse the page to find actors and charactors
		Element castTable = doc.select("table.cast_list").first();
		Elements cast = castTable.select("td[itemprop=actor]");
		cast = cast.select("a[href]");
		Elements character  = castTable.select("td.character");
		Iterator<Element> itca = cast.iterator();
		Iterator<Element> itch = character.iterator();
		String characterList = "";
		int castNumber = 5;
		JSONArray castList = new JSONArray();
		while(itca.hasNext() && itch.hasNext() && castNumber > 0)
		{
			Element oneCast  = itca.next();
			String url = oneCast.attr("href");			
			JSONObject castJSON = parsePeople(url);
			castJSON.put("name", oneCast.text());
			
			String chName = itch.next().text();
			castJSON.put("cast", chName);
			castList.add(castJSON);
			if(characterList == "")
			{
				characterList = chName;
			}
			else
			{
				characterList = characterList + chName;
			}
			castNumber--;
		}
		
		fullInfo.put("Actors",castList);
		
		
		return fullInfo;
	}
	
	/**
	 * The method is used to parse the IMDB people page
	 * @param url
	 * @return the people's job like writer, director or actress
	 */
	public JSONObject parsePeople(String url)
	{
		if(url == null)
		{
			return null;
		}
		//Sleep first
		try
		{
			Thread.currentThread().sleep(2000);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		String job = "";
		JSONObject people = new JSONObject();
		//retreive the html by using the url		
		InputStream is = null;
		BufferedReader br;
		String line;
		String HTMLtext="";
		try {
			URL u = new URL("http://www.imdb.com" + url);
			is = u.openStream(); // throws an IOException
			br = new BufferedReader(new InputStreamReader(is,"utf-8"));
			while ((line = br.readLine()) != null) {
				HTMLtext += line;
			}
		} 
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		//parse the HTML page
		Document doc = Jsoup.parse(HTMLtext);
		Element infobar = doc.select("div.infobar").first();
		Elements test = infobar.select("span.itemprop");
		Iterator<Element> itW = test.listIterator();
		while(itW.hasNext())
		{
			Element e = itW.next();
			String text = e.text();
			//System.out.println(text);
			if(job == "")
			{
				job = text;
			}
			else
			{
				job = job + "," +text;
			}
		}
		people.put("job", job);
		//try to find the birth
		Element born = doc.select("time").first();
		if(born != null){
			String birth = born.attr("datetime");//format is year-month-day
			people.put("birth", birth);
		}
		return people;
	}
	
}