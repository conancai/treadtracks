package com.example.treadtracksproto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;

public class SongBpmRetriever extends AsyncTask<String, Integer, Integer> {
	
	private String API_KEY = "0SU5PIXAMC7BHFFLK";
	private RunningActivity runningActivity;
	
	public void getBpm(String song, String artist, RunningActivity runActivity) {
		runningActivity = runActivity;
		this.execute(song, artist);
	}
	
	@Override
	protected Integer doInBackground(String... params) {
		String song = params[0];
		String artist = params[1];
		try {
			return getBpmTask(song, artist);
		} catch (UnsupportedEncodingException e) {
			return -1;
		}
	}

	@Override
	protected void onPostExecute(Integer integer) {
		super.onPostExecute(integer);
		if (runningActivity != null) {
			runningActivity.setCurrentSongBpm(integer);
		}
	}

	private JSONObject getJSON(String url) throws JSONException {
		final StringBuilder builder = new StringBuilder();
		final HttpClient client = new DefaultHttpClient();
		final HttpGet httpGet = new HttpGet(url);
		try {
			HttpResponse response = client.execute(httpGet);
			HttpEntity entity = response.getEntity();
			InputStream content = entity.getContent();
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(content));
			String line;
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new JSONObject(builder.toString());
	}

	private Integer getBpmTask(String song, String artist)
			throws UnsupportedEncodingException {
		String base = "http://developer.echonest.com/api/v4/song/";
		String url1 = base + "search?api_key=" + API_KEY + "&artist="
				+ URLEncoder.encode(artist, "UTF-8") + "&title="
				+ URLEncoder.encode(song, "UTF-8");
		try {
			JSONArray songsArray1 = getJSON(url1).getJSONObject("response")
					.getJSONArray("songs");
			if (songsArray1.length() > 0) {
				String songID = songsArray1.getJSONObject(0)
						.getString("id");
				String url2 = base + "profile?api_key=" + API_KEY + "&id="
						+ songID + "&bucket=audio_summary";
				JSONArray songsArray2 = getJSON(url2).getJSONObject(
						"response").getJSONArray("songs");
				if (songsArray2.length() > 0) {
					String tempo = songsArray2.getJSONObject(0)
							.getJSONObject("audio_summary")
							.getString("tempo");
					return Math.round(Float.parseFloat(tempo));
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return -1;
	}
}
