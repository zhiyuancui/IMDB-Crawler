/**
 * @author Zhiyuan
 * This program crawl the film data from IMDB
 * It get a movie name from input and then query the OMDB 
 * which is a free IMDB api.
 * OMDB offer IMDB ID for a film and some film information.
 * Then this program will crawl the IMDB film page using the IMDB id.
 */


package crawler;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class IMDBcrawler {

	/**
	 * The method use the omdb API to query the omdb to get the movie information
	 * including the imdb id which could be used to generate imdb url.
	 * @param movieName: the movie name you want to query
	 * @return the imdb page url
	 */
	public JSONObject queryOMDB(String movieName,String year)
	{
		//Crawler the movie information
		movieName = movieName.replaceAll(" ", "%20");
		String URL = "http://www.omdbapi.com/?t="
					  +movieName
					  +"&y=&plot=short";
		if(year != null)
		{
			URL = "http://www.omdbapi.com/?t="
					  +movieName
					  +"&y="+year+ "&plot=short";
		}
		//the url for imdb
		JSONObject completeJSON=null;
		
		try{
			//Get the imdb JSON
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(URL);
			HttpResponse response = client.execute(request);
			BufferedReader rd = new BufferedReader(new InputStreamReader(
								  response.getEntity().getContent()));
			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			
			//Parse JSON
			JSONParser jsonParser = new JSONParser();
			Object obj = jsonParser.parse(result.toString());
			JSONObject movieJSONObject = (JSONObject) obj;
			//Go out to fetch IMDB page
			completeJSON = queryIMDB(movieJSONObject);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return completeJSON;
	}
	
	/**
	 * This method is used to merge the result from the omdb and
	 * query the imdb to get the other usefull infomation
	 * @param movieJSON
	 * @return the complete JSON object
	 */
	public JSONObject queryIMDB(JSONObject movieJSON)
	{
		System.out.println("Start crawler IMDB.");
		//using the imdb to create a url.
		String id = (String)movieJSON.get("imdbID");
		if(id == null)
		{
			return null;
		}
		String url = "http://www.imdb.com/title/" + id;
		
		//retreive the html by using the url		
		InputStream is = null;
		BufferedReader br;
		String line;
		String HTMLtext="";
		try {
			URL u = new URL(url);
			is = u.openStream(); // throws an IOException
			br = new BufferedReader(new InputStreamReader(is));
			while ((line = br.readLine()) != null) {
				HTMLtext += line;
			}
		} 
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		//parse the html page
		ParseIMDBPage p = new ParseIMDBPage(HTMLtext);
		movieJSON.remove("Director");
		movieJSON.remove("Writer");
		movieJSON.remove("Actors");
		movieJSON.remove("Released");
		//Get rid of the N/A by touching every node of hash map.
		Iterator it = movieJSON.entrySet().iterator();
		while(it.hasNext())
		{
			Map.Entry<String, String> entry = (Map.Entry<String, String>)it.next();
			
			String val = entry.getValue();
			if(val.equals("N/A"))
			{
				it.remove();
			}
		}
		
		JSONObject completeMovieObject = p.parse(movieJSON);
		return completeMovieObject;
	}
	
	
	/**
	 * read the film name from file
	 * @param filename the file that stores the movie name
	 * @return
	 */
	public ArrayList<String> getMovieList(String filename)
	{
		ArrayList<String> movieName = new ArrayList<String>();
		
		try{
			FileReader reader = new FileReader(filename);
			Scanner scanner = new Scanner(reader);
			while(scanner.hasNext())
			{
				String name = scanner.nextLine();
				movieName.add(name);
			}
			scanner.close();
			reader.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return movieName;
	}
	
	public static void main(String[] args)
	{
		if(args.length != 1)
		{
			System.out.println("Error argument");
			System.out.println("Usage: java IMDBcrawler filename");
			return;
		}
		
		IMDBcrawler c = new IMDBcrawler();
		ArrayList<String> filmName = c.getMovieList(args[0]);
		
		//Iterator the list to get the name
		Iterator<String> it = filmName.iterator();
		while(it.hasNext())
		{
			String name = it.next();
			
			//Output the result
			System.out.println(c.queryOMDB(name,null));
		}
		
	}
	
}
