package com.example.dex.second;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

/**
 * @file SecondaryActivity.java
 * @author VinsonCai
 * @brief
 * @date 2014-10-3 create
 * 
 */
public class SecondaryActivity extends Activity {

	private Activity mOtherActivity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
		Toast.makeText(mOtherActivity, "this another activity", Toast.LENGTH_SHORT).show();
	}

	public void setActivity(Activity otherActivity) {
		mOtherActivity = otherActivity;
	}
}
