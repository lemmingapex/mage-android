package mil.nga.giat.mage.event;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.sdk.datastore.user.Event;
import mil.nga.giat.mage.sdk.datastore.user.EventHelper;

/**
 *
 * Banner to hold event name
 *
 * @author wiedemanns
 */
public class EventBannerFragment extends Fragment {

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.event_header, container, false);

		TextView eventTextView = (TextView)rootView.findViewById(R.id.event_header_text);
		Event currentEvent = EventHelper.getInstance(getActivity().getApplicationContext()).getCurrentEvent();
		if(currentEvent != null) {
			eventTextView.setText(currentEvent.getName());
		}

		return rootView;
	}

	void refresh() {
		TextView eventTextView = (TextView)getView().findViewById(R.id.event_header_text);
		eventTextView.setText(EventHelper.getInstance(getActivity().getApplicationContext()).getCurrentEvent().getName());
	}
}
