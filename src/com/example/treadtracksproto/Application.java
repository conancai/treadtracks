package com.example.treadtracksproto;

import com.parse.Parse;
import com.parse.ParseObject;

public class Application extends android.app.Application {

	public Application() {
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Parse.initialize(this, "tD1btrqGO7hWJrjE0ThswtnPYXKrvohWQSpoktHI",
				"U8tZwhqiwY2NOsGLkOdrWZFytRMouW6Eqi2sQVtq");
		ParseObject testObject = new ParseObject("TestObject");
		testObject.put("foo", "bar");
		testObject.saveInBackground();
	}

}
