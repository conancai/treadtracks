package com.example.treadtracksproto;

import com.parse.Parse;
import com.parse.ParseObject;

public class Application extends android.app.Application {

	public Application() {
	}

	@Override
	public void onCreate() {
		super.onCreate();
		ParseObject.registerSubclass(StatsPost.class);
		Parse.initialize(this, "tD1btrqGO7hWJrjE0ThswtnPYXKrvohWQSpoktHI",
				"U8tZwhqiwY2NOsGLkOdrWZFytRMouW6Eqi2sQVtq");
		ParseObject testObject = new ParseObject("TestObject");
		testObject.put("foo", "bar");
		StatsPost post1 = new StatsPost();
		post1.setDate("2-4-14");
		post1.setDistance("5");
		post1.setPace("10:05 Mi.");
		post1.setTime("30 Minutes");
		StatsPost post2 = new StatsPost();
		post2.setDate("2-5-14");
		post2.setDistance("6");
		post2.setPace("14:02 Mi.");
		post2.setTime("53 Minutes");
		post1.saveInBackground();
		post2.saveInBackground();
		testObject.saveInBackground();
	}

}
