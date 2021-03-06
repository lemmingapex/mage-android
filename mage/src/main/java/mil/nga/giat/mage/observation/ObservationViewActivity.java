package mil.nga.giat.mage.observation;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import mil.nga.giat.mage.R;
import mil.nga.giat.mage.event.EventBannerFragment;
import mil.nga.giat.mage.form.LayoutBaker;
import mil.nga.giat.mage.form.LayoutBaker.ControlGenerationType;
import mil.nga.giat.mage.map.marker.ObservationBitmapFactory;
import mil.nga.giat.mage.sdk.datastore.observation.Attachment;
import mil.nga.giat.mage.sdk.datastore.observation.Observation;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationHelper;
import mil.nga.giat.mage.sdk.datastore.observation.ObservationProperty;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.event.IObservationEventListener;
import mil.nga.giat.mage.sdk.utils.DateFormatFactory;

public class ObservationViewActivity extends Activity {

	private static final String LOG_NAME = ObservationViewActivity.class.getName();
	private static final int ATTACHMENT_VIEW_ACTIVITY_REQUEST_CODE = 500;
	public static String OBSERVATION_ID = "OBSERVATION_ID";
	public static String INITIAL_LOCATION = "INITIAL_LOCATION";
	public static String INITIAL_ZOOM = "INITIAL_ZOOM";
	private final DateFormat dateFormat = DateFormatFactory.format("yyyy-MM-dd HH:mm zz", Locale.getDefault());
	private GoogleMap miniMap;
    private AttachmentGallery attachmentGallery;
	private IObservationEventListener observationEventListener;
	private Observation o;
	private Marker marker;
	private DecimalFormat latLngFormat = new DecimalFormat("###.#####");

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				this.finish();
				return true;
			case R.id.observation_edit:
				if(!UserHelper.getInstance(getApplicationContext()).isCurrentUserPartOfCurrentEvent()) {
					new AlertDialog.Builder(this).setTitle("Not a member of this event").setMessage("You are an administrator and not a member of the current event.  You can not edit this observation.").setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
						}
					}).show();
				} else {
					Intent intent = new Intent(this, ObservationEditActivity.class);
					intent.putExtra(ObservationEditActivity.OBSERVATION_ID, o.getId());
					intent.putExtra(ObservationViewActivity.INITIAL_LOCATION, miniMap.getCameraPosition().target);
					intent.putExtra(ObservationViewActivity.INITIAL_ZOOM, miniMap.getCameraPosition().zoom);
					startActivityForResult(intent, 2);
				}

				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.observation_view_menu, menu);
		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.observation_viewer);

		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager.beginTransaction().add(R.id.observation_view_event_holder, new EventBannerFragment()).commit();

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		try {
			LayoutBaker.populateLayoutWithControls((LinearLayout) findViewById(R.id.propertyContainer), LayoutBaker.createControlsFromJson(this, ControlGenerationType.VIEW, ObservationHelper.getInstance(getApplicationContext()).read(getIntent().getLongExtra(OBSERVATION_ID, 0L)).getEvent().getForm()));
		} catch(Exception e) {
			Log.e(LOG_NAME, "Problem getting observation.", e);
		}
  	}

	@Override
	protected void onDestroy() {
		if (observationEventListener != null) {
			ObservationHelper.getInstance(getApplicationContext()).removeListener(observationEventListener);
		}
		super.onDestroy();
	}

	@Override
	public void onResume() {
		super.onResume();
		setupView(true);
	}

	private void setupView(Boolean addListeners) {
		try {
			o = ObservationHelper.getInstance(getApplicationContext()).read(getIntent().getLongExtra(OBSERVATION_ID, 0L));
			if (addListeners) {
				observationEventListener = new IObservationEventListener() {
					@Override
					public void onError(Throwable error) {
					}

					@Override
					public void onObservationUpdated(Observation observation) {
						if (observation.getId().equals(o.getId()) && !observation.isDirty()) {
							ObservationViewActivity.this.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									setupView(false);
								}
							});
							ObservationHelper.getInstance(getApplicationContext()).removeListener(this);
						}
					}

					@Override
					public void onObservationDeleted(Observation observation) {
					}

					@Override
					public void onObservationCreated(Collection<Observation> observations, Boolean sendUserNotifcations) {
						for (Observation observation : observations) {
							if (observation.getId().equals(o.getId()) && !observation.isDirty()) {
								ObservationViewActivity.this.runOnUiThread(new Runnable() {
									@Override
									public void run() {
										setupView(false);
									}
								});
								ObservationHelper.getInstance(getApplicationContext()).removeListener(this);
								break;
							}
						}
					}
				};
				ObservationHelper.getInstance(getApplicationContext()).addListener(observationEventListener);
			}
			o = ObservationHelper.getInstance(getApplicationContext()).read(getIntent().getLongExtra(OBSERVATION_ID, 0L));

			Map<String, ObservationProperty> propertiesMap = o.getPropertiesMap();

			ObservationProperty observationProperty = propertiesMap.get("type");
			if (observationProperty != null) {
				this.setTitle(observationProperty.getValue().toString());
			}
			Geometry geo = o.getGeometry();
			if (geo instanceof Point) {
				Point pointGeo = (Point) geo;
				((TextView) findViewById(R.id.location)).setText(latLngFormat.format(pointGeo.getY()) + ", " + latLngFormat.format(pointGeo.getX()));
				if (propertiesMap.containsKey("provider")) {
					((TextView) findViewById(R.id.location_provider)).setText("(" + propertiesMap.get("provider").getValue() + ")");
				} else {
					findViewById(R.id.location_provider).setVisibility(View.GONE);
				}
				if (propertiesMap.containsKey("accuracy") && Float.parseFloat(propertiesMap.get("accuracy").getValue().toString()) > 0f) {
					((TextView) findViewById(R.id.location_accuracy)).setText("\u00B1" + propertiesMap.get("accuracy").getValue().toString() + "m");
				} else {
					findViewById(R.id.location_accuracy).setVisibility(View.GONE);
				}
				Fragment tempFragment = getFragmentManager().findFragmentById(R.id.mini_map);
				if (tempFragment != null) {
					miniMap = ((MapFragment) tempFragment).getMap();
					miniMap.getUiSettings().setZoomControlsEnabled(false);

					LatLng latLng = getIntent().getParcelableExtra(INITIAL_LOCATION);
					if (latLng == null) {
						latLng = new LatLng(0, 0);
					}

					float zoom = getIntent().getFloatExtra(INITIAL_ZOOM, 0);

					miniMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

					LatLng location = new LatLng(pointGeo.getY(), pointGeo.getX());
					miniMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
					if (marker != null) {
						marker.remove();
						marker = null;
					}
					marker = miniMap.addMarker(new MarkerOptions().position(location).icon(ObservationBitmapFactory.bitmapDescriptor(this, o)));
				}
			}

			LayoutBaker.populateLayoutFromMap((LinearLayout) findViewById(R.id.propertyContainer), ControlGenerationType.VIEW, o.getPropertiesMap());
			LayoutBaker.populateLayoutFromMap((LinearLayout) findViewById(R.id.topPropertyContainer), ControlGenerationType.VIEW, o.getPropertiesMap());

            LinearLayout galleryLayout = (LinearLayout) findViewById(R.id.image_gallery);
            galleryLayout.removeAllViews();
			if (o.getAttachments().size() == 0) {
				findViewById(R.id.image_gallery).setVisibility(View.GONE);
			} else {
                attachmentGallery = new AttachmentGallery(getApplicationContext(), 300, 300);
                attachmentGallery.addOnAttachmentClickListener(new AttachmentGallery.OnAttachmentClickListener() {
                    @Override
                    public void onAttachmentClick(Attachment attachment) {
                        Intent intent = new Intent(getApplicationContext(), AttachmentViewerActivity.class);
                        intent.putExtra(AttachmentViewerActivity.ATTACHMENT, attachment);
                        intent.putExtra(AttachmentViewerActivity.EDITABLE, false);
                        startActivity(intent);
                    }
                });
                attachmentGallery.addAttachments(galleryLayout, o.getAttachments());
			}

			TextView user = (TextView) findViewById(R.id.username);
			String userText = "Unknown User";
			User u = UserHelper.getInstance(this).read(o.getUserId());
			if (u != null) {
				userText = u.getFirstname() + " " + u.getLastname();
			}
			user.setText(userText);

			FrameLayout fl = (FrameLayout) findViewById(R.id.sync_status);
			fl.removeAllViews();
			if (o.isDirty()) {
				View.inflate(getApplicationContext(), R.layout.saved_locally, fl);
			} else {
				View status = View.inflate(getApplicationContext(), R.layout.submitted_on, fl);
				TextView syncDate = (TextView) status.findViewById(R.id.observation_sync_date);
				syncDate.setText(dateFormat.format(o.getLastModified()));
			}
		} catch (Exception e) {
			Log.e(LOG_NAME, e.getMessage(), e);
		}
	}
}