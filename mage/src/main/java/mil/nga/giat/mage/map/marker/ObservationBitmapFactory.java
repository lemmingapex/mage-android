package mil.nga.giat.mage.map.marker;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Stack;

import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.http.get.MageServerGetRequests;

public class ObservationBitmapFactory {
	
	private static final String LOG_NAME = ObservationBitmapFactory.class.getName();

	private static final String DEFAULT_ASSET = "markers/default.png";
	private static final String TYPE_PROPERTY = "type";

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public static Bitmap bitmap(Context context, Observation observation) {
		InputStream iconStream = getIconStream(context, observation);
		
		// scale the image to a good size
		Bitmap bitmap = BitmapFactory.decodeStream(iconStream);
		Integer maxDimension = Math.max(bitmap.getWidth(), bitmap.getHeight());
		float density = context.getResources().getDisplayMetrics().xdpi; //context.getResources().getDisplayMetrics().densityDpi;
		double scale = (density/3.5) / maxDimension;
		int outWidth = Double.valueOf(scale*Integer.valueOf(bitmap.getWidth()).doubleValue()).intValue();
		int outHeight = Double.valueOf(scale*Integer.valueOf(bitmap.getHeight()).doubleValue()).intValue();
		bitmap = Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, true);

		return bitmap;
	}

	public static BitmapDescriptor bitmapDescriptor(Context context, Observation observation) {
		Bitmap bitmap = bitmap(context, observation);
		return BitmapDescriptorFactory.fromBitmap(bitmap);
	}

	private static final FileFilter fileFilter = new FileFilter() {
	    @Override
	    public boolean accept(File pathname) {
	        return pathname.isFile();
	    }
	};
	
	/**
	 * Figure out which icon to navigate to
	 * 
	 * @param observation
	 * @return
	 */
	private static InputStream getIconStream(Context context, Observation observation) {
		InputStream iconStream = null;
		if (observation != null) {
	
			Map<String, ObservationProperty> properties = observation.getPropertiesMap();
			// get type
			ObservationProperty type = properties.get(TYPE_PROPERTY);

            Event event = observation.getEvent();

			// get variantField
			JsonObject dynamicFormJson = event.getForm();
			
			// get variant
			ObservationProperty variant = null;
			JsonElement variantField = dynamicFormJson.get("variantField");
			if(variantField != null && !variantField.isJsonNull()) {
				variant = properties.get(variantField.getAsString());
			}

            // make path from type and variant
            File path = new File(new File(new File(context.getFilesDir() + MageServerGetRequests.OBSERVATION_ICON_PATH), event.getRemoteId()), "icons");

            Stack<ObservationProperty> iconProperties = new Stack<ObservationProperty>();
            iconProperties.add(variant);
            iconProperties.add(type);

            path = recurseGetIconPath(iconProperties, path, 0);

            if (path != null && path.exists() && path.isFile()) {
                try {
                    iconStream = new FileInputStream(path);
                } catch (FileNotFoundException e) {
                    Log.e(LOG_NAME, "Can find icon.", e);
                }
            }
		}
		if(iconStream == null) {
			try {
				iconStream = context.getAssets().open(DEFAULT_ASSET);
			} catch (IOException e) {
				Log.e(LOG_NAME, "Can find default icon.", e);
			}
		}

		return iconStream;
	}
	
	private static File recurseGetIconPath(Stack<ObservationProperty> iconProperties, File path, int i) {
		if (iconProperties.size() > 0) {
			ObservationProperty property = iconProperties.pop();
			if (property != null && path.exists()) {
				String propertyString = property.getValue().toString();
				if (propertyString != null && !propertyString.trim().isEmpty() && new File(path, propertyString).exists()) {
					return recurseGetIconPath(iconProperties, new File(path, propertyString), i + 1);
				}
			}
		}
		while (path != null && path.listFiles(fileFilter) != null && path.listFiles(fileFilter).length == 0 && i >= 0) {
			path = path.getParentFile();
			i--;
		}
		if (path == null || !path.exists()) return null;
		
		File[] files = path.listFiles(fileFilter);
		return files.length == 0 ? null : files[0];
	}
}