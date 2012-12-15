/*******************************************************************************
 * Copyright 2011, 2012 Chris Banes.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package uk.co.senab.bitmapcache.samples;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import uk.co.senab.bitmapcache.BitmapLruCache;
import uk.co.senab.bitmapcache.CacheableBitmapWrapper;
import uk.co.senab.bitmapcache.CacheableImageView;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.AttributeSet;

/**
 * Simple extension of CacheableImageView which allows downloading of Images of
 * the Internet.
 * 
 * This code isn't production quality, but works well enough for this sample.s
 * 
 * @author Chris Banes
 * 
 */
public class NetworkedCacheableImageView extends CacheableImageView {

	/**
	 * This task simply fetches an Bitmap from the specified URL and wraps it in
	 * a wrapper. This implementation is NOT 'best practice' or production ready
	 * code.
	 */
	private class ImageUrlAsyncTask extends AsyncTask<String, Void, CacheableBitmapWrapper> {

		private final boolean mFullSize;

		public ImageUrlAsyncTask(boolean fullSize) {
			mFullSize = fullSize;
		}

		@Override
		protected CacheableBitmapWrapper doInBackground(String... params) {
			try {
				String url = params[0];

				HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
				InputStream is = new BufferedInputStream(conn.getInputStream());

				BitmapFactory.Options opts = new BitmapFactory.Options();
				if (!mFullSize) {
					opts.inSampleSize = 2;
				}

				Bitmap bitmap = BitmapFactory.decodeStream(is, null, opts);

				if (null != bitmap) {
					return new CacheableBitmapWrapper(url, bitmap);
				}

			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(CacheableBitmapWrapper result) {
			super.onPostExecute(result);

			// Display the image
			setImageCachedBitmap(result);

			// Add to cache
			mCache.put(result);
		}
	}

	private final BitmapLruCache mCache;
	private ImageUrlAsyncTask mCurrentTask;

	public NetworkedCacheableImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mCache = SampleApplication.getApplication(context).getBitmapCache();
	}

	public boolean loadImage(String url) {
		return loadImage(url, true);
	}

	/**
	 * Loads the Bitmap.
	 * 
	 * @param url - URL of image
	 * @param fullSize - Whether the image should be kept at the original size
	 * @return true if the bitmap was found in the cache
	 */
	public boolean loadImage(String url, final boolean fullSize) {
		// First check whether there's already a task running, if so cancel it
		if (null != mCurrentTask) {
			mCurrentTask.cancel(false);
		}

		// Check to see if the cache already has the bitmap
		CacheableBitmapWrapper wrapper = mCache.get(url);

		if (null != wrapper && wrapper.hasValidBitmap()) {
			// The cache has it, so just display it
			setImageCachedBitmap(wrapper);
			return true;
		} else {
			// Cache doesn't have the URL, do network request...
			setImageCachedBitmap(null);

			mCurrentTask = new ImageUrlAsyncTask(fullSize);
			mCurrentTask.execute(url);
			return false;
		}
	}

}
