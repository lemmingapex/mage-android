package mil.nga.giat.mage.map.preference;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.preferences.PreferenceFragmentSummary;
import mil.nga.giat.mage.sdk.datastore.layer.Layer;
import mil.nga.giat.mage.sdk.datastore.layer.LayerHelper;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;
import mil.nga.giat.mage.sdk.exceptions.LayerException;

/**
 * Provides map configuration driven settings that are available to the user.
 * Check mappreferences.xml for the configuration.
 *
 * @author newmanw
 */
public class MapPreferencesActivity extends PreferenceActivity {

	public static String LOG_NAME = MapPreferencesActivity.class.getName();

	public static final int TILE_OVERLAY_ACTIVITY = 0;
	public static final int FEATURE_OVERLAY_ACTIVITY = 1;
	public static final String OVERLAY_EXTENDED_DATA_KEY = "overlay";

	private MapPreferenceFragment preference = new MapPreferenceFragment();

	public static class MapPreferenceFragment extends PreferenceFragmentSummary {

		public MapPreferenceFragment() {
			Bundle bundle = new Bundle();
			bundle.putInt(PreferenceFragmentSummary.xmlResourceClassKey, R.xml.mappreferences);
			setArguments(bundle);
		}

		@Override
		public void onCreate(final Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
		}

		@Override
		public void onResume() {
			super.onResume();

			findPreference(getString(R.string.tileOverlaysKey)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					Intent intent = new Intent(getActivity(), TileOverlayPreferenceActivity.class);
					getActivity().startActivityForResult(intent, TILE_OVERLAY_ACTIVITY);
					return true;
				}
			});

			findPreference(getString(R.string.staticFeatureLayersKey)).setOnPreferenceClickListener(new OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					Intent intent = new Intent(getActivity(), FeatureOverlayPreferenceActivity.class);
					getActivity().startActivityForResult(intent, FEATURE_OVERLAY_ACTIVITY);
					return true;
				}
			});

			// TODO : Remove the below and rework OverlayPreference to have a 'entities' similar to a list preference, these would be the 'display values'
			OverlayPreference p = (OverlayPreference) findPreference(getResources().getString(R.string.staticFeatureLayersKey));
			try {
				Set<String> layerIds = p.getValues();
				Collection<String> values = new ArrayList<String>(layerIds.size());
				for (Layer l : LayerHelper.getInstance(getActivity()).readByEvent(EventHelper.getInstance(getActivity().getApplicationContext()).getCurrentEvent())) {
					if (layerIds.contains(l.getId().toString())) {
						values.add(l.getName());
					}
				}
				p.setSummary(StringUtils.join(values, "\n"));
			} catch (LayerException e) {
				Log.e(LOG_NAME, "Problem setting preference.", e);
			}

		}

		@Override
		public void onPause() {
			super.onPause();

			findPreference(getString(R.string.tileOverlaysKey)).setOnPreferenceClickListener(null);
			findPreference(getString(R.string.staticFeatureLayersKey)).setOnPreferenceClickListener(null);

		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getFragmentManager().beginTransaction().replace(android.R.id.content, preference).commit();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case TILE_OVERLAY_ACTIVITY: {
				if (resultCode == Activity.RESULT_OK) {
					OverlayPreference p = (OverlayPreference) preference.findPreference(getString(R.string.tileOverlaysKey));
					p.setValues(new HashSet<String>(data.getStringArrayListExtra(OVERLAY_EXTENDED_DATA_KEY)));
				}
				break;
			}
			case FEATURE_OVERLAY_ACTIVITY: {
				if (resultCode == Activity.RESULT_OK) {
					OverlayPreference p = (OverlayPreference) preference.findPreference(getString(R.string.staticFeatureLayersKey));
					p.setValues(new HashSet<String>(data.getStringArrayListExtra(OVERLAY_EXTENDED_DATA_KEY)));
				}
				break;
			}
			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
	}
}