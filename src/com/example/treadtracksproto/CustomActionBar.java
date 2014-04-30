package com.example.treadtracksproto;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

public class CustomActionBar extends TextView {

	public CustomActionBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public CustomActionBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public CustomActionBar(Context context) {
		super(context);
		init();
	}

	private void init() {
		Typeface tf = Typeface.createFromAsset(getContext().getAssets(),
				"fonts/Capture_it 2.ttf");
		setTypeface(tf);
	}

}
