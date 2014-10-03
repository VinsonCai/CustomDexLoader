/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.dex;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import dalvik.system.DexClassLoader;

public class MainActivity extends Activity {
	private static final String SECONDARY_DEX_NAME = "secondary_dex.jar";

	// Buffer size for file copying. While 8kb is used in this sample, you
	// may want to tweak it based on actual size of the secondary dex file involved.
	private static final int BUF_SIZE = 8 * 1024;

	private Button mToastButton = null;
	private Button mSecondButton;
	private ProgressDialog mProgressDialog = null;
	File dexInternalStoragePath = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mToastButton = (Button) findViewById(R.id.toast_button);

		// Before the secondary dex file can be processed by the DexClassLoader,
		// it has to be first copied from asset resource to a storage location.
		dexInternalStoragePath = new File(getDir("dex", Context.MODE_PRIVATE), SECONDARY_DEX_NAME);
		if (!dexInternalStoragePath.exists()) {
			mProgressDialog =
					ProgressDialog.show(this, getResources().getString(R.string.diag_title),
							getResources().getString(R.string.diag_message), true, false);
			// Perform the file copying in an AsyncTask.
			(new PrepareDexTask()).execute(dexInternalStoragePath);
		} else {
			mToastButton.setEnabled(true);
		}

		mToastButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// Internal storage where the DexClassLoader writes the optimized dex file to.
				final File optimizedDexOutputPath = getDir("outdex", Context.MODE_PRIVATE);

				// Initialize the class loader with the secondary dex file.
				DexClassLoader cl =
						new DexClassLoader(dexInternalStoragePath.getAbsolutePath(), optimizedDexOutputPath
								.getAbsolutePath(), null, getClassLoader());
				Class libProviderClazz = null;

				try {
					// Load the library class from the class loader.
					libProviderClazz = cl.loadClass("com.example.dex.lib.LibraryProvider");

					// Cast the return object to the library interface so that the
					// caller can directly invoke methods in the interface.
					// Alternatively, the caller can invoke methods through reflection,
					// which is more verbose and slow.
					LibraryInterface lib = (LibraryInterface) libProviderClazz.newInstance();

					// Display the toast!
					lib.showAwesomeToast(view.getContext(), "hello");
				} catch (Exception exception) {
					// Handle exception gracefully here.
					exception.printStackTrace();
				}
			}
		});

		mSecondButton = (Button) findViewById(R.id.second_button);
		mSecondButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				try {
					loadApk();
					startRemoteActivity();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

	}

	private void loadApk() {
		ClassLoader localClassLoader = ClassLoader.getSystemClassLoader();
		// Internal storage where the DexClassLoader writes the optimized dex file to.
		final File optimizedDexOutputPath = getDir("outdex", Context.MODE_PRIVATE);

		// Initialize the class loader with the secondary dex file.
		DexClassLoader cl =
				new DexClassLoader(dexInternalStoragePath.getAbsolutePath(), optimizedDexOutputPath.getAbsolutePath(),
						null, localClassLoader);

		try {
			localClass = cl.loadClass("com.example.dex.second.SecondaryActivity");
			Constructor<?> localConstructor = localClass.getConstructor(new Class[] {});

			localInstance = localConstructor.newInstance(new Object[] {});

			Method localMethodSetActivity = localClass.getDeclaredMethod("setActivity", new Class[] { Activity.class });
			localMethodSetActivity.setAccessible(true);
			localMethodSetActivity.invoke(localInstance, new Object[] { MainActivity.this });

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	private Class<?> localClass;
	private Object localInstance;

	private void startRemoteActivity() throws Exception {
		Method onCreateMethod = localClass.getDeclaredMethod("onCreate", new Class[] { Bundle.class });
		onCreateMethod.setAccessible(true);
		onCreateMethod.invoke(localInstance, new Object[] { null });
	}

	// File I/O code to copy the secondary dex file from asset resource to internal storage.
	private boolean prepareDex(File dexInternalStoragePath) {
		BufferedInputStream bis = null;
		OutputStream dexWriter = null;

		try {
			bis = new BufferedInputStream(getAssets().open(SECONDARY_DEX_NAME));
			dexWriter = new BufferedOutputStream(new FileOutputStream(dexInternalStoragePath));
			byte[] buf = new byte[BUF_SIZE];
			int len;
			while ((len = bis.read(buf, 0, BUF_SIZE)) > 0) {
				dexWriter.write(buf, 0, len);
			}
			dexWriter.close();
			bis.close();
			return true;
		} catch (IOException e) {
			if (dexWriter != null) {
				try {
					dexWriter.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
			if (bis != null) {
				try {
					bis.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
			return false;
		}
	}

	private class PrepareDexTask extends AsyncTask<File, Void, Boolean> {

		@Override
		protected void onCancelled() {
			super.onCancelled();
			if (mProgressDialog != null)
				mProgressDialog.cancel();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (mProgressDialog != null)
				mProgressDialog.cancel();
		}

		@Override
		protected Boolean doInBackground(File... dexInternalStoragePaths) {
			prepareDex(dexInternalStoragePaths[0]);
			return null;
		}
	}
}
