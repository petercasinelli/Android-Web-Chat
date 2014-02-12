package com.example.android_web_chat;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class WebChatActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_web_chat);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.web_chat, menu);
		return true;
	}

}
