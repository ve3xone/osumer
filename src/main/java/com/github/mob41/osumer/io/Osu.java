package com.github.mob41.osumer.io;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.github.mob41.osumer.exceptions.OsuException;

public class Osu {
	
	public static final int SUCCESS = 0;
	
	public static final int INVALID_USERNAME_PASSWORD = 1;
	
	private static final String LOGOUT_URL = "http://osu.ppy.sh/forum/ucp.php?mode=logout";
	
	private static final String LOGIN_URL = "http://osu.ppy.sh/forum/ucp.php?mode=login";
	
	private static final String INDEX_LOCATION_URL = "http://osu.ppy.sh/forum/index.php";
	
	private final CookieManager cmgr;
	
	public Osu() {
		cmgr = new CookieManager();
	}
	
	public String getBeatmapDownloadLink(String beatmapLink){
		try {
			URL url = new URL(beatmapLink);
			
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			
			if (cmgr.getCookieStore().getCookies().size() > 0) {
			    // While joining the Cookies, use ',' or ';' as needed. Most of the servers are using ';'
			    conn.setRequestProperty("Cookie",
			    join(";", cmgr.getCookieStore().getCookies()));    
			}
			
			conn.setUseCaches(false);
			conn.setDoOutput(false);
			conn.setDoInput(true);
			conn.setAllowUserInteraction(false);
			conn.setRequestMethod("GET");
			
			//Fake environment from Chrome
			conn.setRequestProperty("Connection", "Keep-alive");
			conn.setRequestProperty("Cache-Control", "max-age=0");
			conn.setRequestProperty("Origin", "https://osu.ppy.sh");
			conn.setRequestProperty("Upgrade-Insecure-Requests", "0");
			conn.setRequestProperty("User-Agent", "User-Agent: Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.87 Safari/537.36");
			conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			conn.setRequestProperty("DNT", "1");
			//conn.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
			conn.setRequestProperty("Accept-Language", "zh-TW,zh;q=0.8,en;q=0.6");
			
			String data = "";
			String line;
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			
			while((line = reader.readLine()) != null){
				data += line;
			}
			
			Map<String, List<String>> headerFields = conn.getHeaderFields();
			
			List<String> cookiesHeader = headerFields.get("Set-Cookie");

			if (cookiesHeader != null) {
			    for (String cookie : cookiesHeader) {
			        cmgr.getCookieStore().add(null,HttpCookie.parse(cookie).get(0));
			    }               
			}
			
			Document doc = Jsoup.parse(data);
			Elements elements = doc.getElementsByClass("beatmap_download_link");
			Element alnk = elements.get(0);
			
			String href = alnk.attr("href");
			
			return href;
		} catch (Exception e) {
			throw new OsuException("Error occurred when getting download link", e);
		}
	}
	
	public int login(String username, String password) throws OsuException{
		try {
			String urlPara = 
					"login=Login&" +
					"username=" + username + "&" + //Username
					"password=" + password + "&" + //Password
					"autologin=" + true + "&" +   //Log me on automatically each visit
					"viewonline=" + true + "&" +   //Hide my online this session
					"redirect=" + "index.php" + "&" + //Redirect (To check whether the login is success)
					"sid="; //Session ID, not distributed once
			
			byte[] postData = urlPara.getBytes(StandardCharsets.UTF_8);
			int postLen = postData.length;
			
			URL url = new URL(LOGIN_URL);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			
			conn.setRequestMethod("POST");
			conn.setInstanceFollowRedirects(false);
			
			if (cmgr.getCookieStore().getCookies().size() > 0) {
			    // While joining the Cookies, use ',' or ';' as needed. Most of the servers are using ';'
			    conn.setRequestProperty("Cookie",
			    join(";", cmgr.getCookieStore().getCookies()));    
			}
			
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			
			//Fake environment from Chrome
			conn.setRequestProperty("Connection", "Keep-alive");
			conn.setRequestProperty("Cache-Control", "max-age=0");
			conn.setRequestProperty("Origin", "https://osu.ppy.sh");
			conn.setRequestProperty("Upgrade-Insecure-Requests", "0");
			conn.setRequestProperty("User-Agent", "User-Agent: Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.87 Safari/537.36");
			conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			conn.setRequestProperty("DNT", "1");
			conn.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
			conn.setRequestProperty("Accept-Language", "zh-TW,zh;q=0.8,en;q=0.6");
			
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			//conn.setRequestProperty("charset", "utf-8");
			conn.setRequestProperty("Content-Length", Integer.toString(postLen));
			
			OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
			wr.write(urlPara);
			wr.close();
			
			String data = "";
			String line;
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			
			while((line = reader.readLine()) != null){
				data += line + "\n";
			}
			
			Map<String, List<String>> headerFields = conn.getHeaderFields();
			
			List<String> locationHeader = headerFields.get("Location");
			
			if (locationHeader == null || locationHeader.size() != 1 || !locationHeader.get(0).equals(INDEX_LOCATION_URL)){
				throw new OsuException("Login failed, redirected to a non-index page");
			}
			
			List<String> cookiesHeader = headerFields.get("Set-Cookie");

			if (cookiesHeader != null) {
			    for (String cookie : cookiesHeader) {
			        cmgr.getCookieStore().add(null,HttpCookie.parse(cookie).get(0));
			    }               
			}
			
			return SUCCESS;
		} catch (Exception e) {
			throw new OsuException("Error occurred when logging in", e);
		}
	}
	
	public int logout(String sid){
		try {
			String urlPara =
					"sid=" + sid; //Session ID
			
			URL url = new URL(LOGOUT_URL + "?" + urlPara);
			URLConnection conn = url.openConnection();
			
			if (cmgr.getCookieStore().getCookies().size() > 0) {
			    // While joining the Cookies, use ',' or ';' as needed. Most of the servers are using ';'
			    conn.setRequestProperty("Cookie",
			    join(";", cmgr.getCookieStore().getCookies()));    
			}
			
			conn.setUseCaches(false);
			
			conn.setRequestProperty("charset", "utf-8");
			conn.setRequestProperty("Content-Length", "0");
			
			Map<String, List<String>> headerFields = conn.getHeaderFields();
			
			List<String> cookiesHeader = headerFields.get("Set-Cookie");

			if (cookiesHeader != null) {
			    for (String cookie : cookiesHeader) {
			        cmgr.getCookieStore().add(null,HttpCookie.parse(cookie).get(0));
			    }               
			}
			
			String data = "";
			String line;
			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			
			while((line = reader.readLine()) != null){
				data += line;
			}
			
			System.out.println(data);
			return SUCCESS;
		} catch (Exception e) {
			throw new OsuException("Error occurred when logging in", e);
		}
	}
	
	protected CookieManager getCookies(){
		return cmgr;
	}
	
	private static void printAllHeaders(Map<String, List<String>> headers){
		Iterator<String> it = headers.keySet().iterator();
		List<String> strs;
		String key;
		while (it.hasNext()){
			key = it.next();
			strs = headers.get(key);
			
			for (int i = 0; i < strs.size(); i++){
				System.out.println(key + " (" + i + "):" + strs.get(i));
			}
		}
		
	}
	
	private static String join(String separator, List<HttpCookie> objs){
		String out = "";
		
		String str;
		for (int i = 0; i < objs.size(); i++){
			str = objs.get(i).toString();
			
			out += str + separator;
			
			if (i != objs.size() - 1){
				out += " ";
			}
		}
		
		return out;
	}

}