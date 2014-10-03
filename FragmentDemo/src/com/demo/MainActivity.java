package com.demo;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {
	private final static int PROGRESS_DIALOG = 1;
	private final static String APP_DIRECTORY = "tmp";
	private ProgressDialog progressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.main);

		Button downloadFragment = (Button) this.findViewById(R.id.download_fragment);
		downloadFragment.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				new FragmentDownloader().download("http://upload.visusnet.de/ECi/fragment.zip");
			}
		});

		Button startDemo = (Button) this.findViewById(R.id.start_demo);
//		startDemo.setEnabled(false);
		startDemo.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				MultiDexClassLoader
						.getInstance()
						.install(
								MainActivity.this,
								Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + APP_DIRECTORY + "/fragment.zip");

				Intent intent = new Intent(MainActivity.this, DynamicFragmentActivity.class);
				intent.putExtra("class_name", "com.demo.DemoFragment");
				startActivity(intent);
			}
		});
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case PROGRESS_DIALOG:
			this.progressDialog = new ProgressDialog(this);
			this.progressDialog.setMessage("Downloading Fragment...");
			this.progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			this.progressDialog.setMax(100);
			return this.progressDialog;
		default:
			return null;
		}
	}

	private class FragmentDownloader extends FileDownloader {
		public void download(String jarUrl) {
			String fragmentDirectory = Environment.getExternalStorageDirectory() + "/" + APP_DIRECTORY;
			this.execute(jarUrl, fragmentDirectory);
		}

		@Override
		protected void onPreExecute() {
			showDialog(PROGRESS_DIALOG);
		}

		@Override
		protected void onProgressUpdate(final Integer... values) {
			progressDialog.setProgress(values[0]);
		}

		@Override
		protected void onPostExecute(String jarPath) {
			if (jarPath != null) {
				// Add the APK/JAR/ZIP file's classes to the class loader.
				MultiDexClassLoader.getInstance().install(MainActivity.this, jarPath);

				Button startDemo = (Button) findViewById(R.id.start_demo);
				startDemo.setEnabled(true);
			}

			dismissDialog(PROGRESS_DIALOG);
		}
	}
}
