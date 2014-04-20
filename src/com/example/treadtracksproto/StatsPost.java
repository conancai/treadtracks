package com.example.treadtracksproto;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseQuery;

@ParseClassName("StatsPost")
public class StatsPost extends ParseObject {

	public String getDate() {
		return getString("date");
	}

	public void setDate(String value) {
		put("date", value);
	}

	public String getDistance() {
		return getString("dist");
	}

	public void setDistance(String value) {
		put("dist", value);
	}

	public String getPace() {
		return getString("pace");
	}

	public void setPace(String value) {
		put("pace", value);
	}

	public String getTime() {
		return getString("time");
	}

	public void setTime(String value) {
		put("time", value);
	}

	public static ParseQuery<StatsPost> getQuery() {
		return ParseQuery.getQuery(StatsPost.class);
	}
}
