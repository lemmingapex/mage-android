package mil.nga.giat.mage.map;

import android.content.Context;
import android.os.AsyncTask;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mil.nga.giat.mage.filter.Filter;
import mil.nga.giat.mage.map.marker.PointCollection;
import mil.nga.giat.mage.sdk.Temporal;
import mil.nga.giat.mage.sdk.datastore.DaoStore;
import mil.nga.giat.mage.sdk.datastore.location.Location;
import mil.nga.giat.mage.sdk.datastore.user.User;
import mil.nga.giat.mage.sdk.datastore.user.UserHelper;
import mil.nga.giat.mage.sdk.exceptions.UserException;

public class LocationLoadTask extends AsyncTask<Void, Location, Void> {

	private Context context;
	private Filter<Temporal> filter;
	private final PointCollection<Location> locationCollection;

	public LocationLoadTask(Context context, PointCollection<Location> locationCollection) {
		this.context = context.getApplicationContext();
		this.locationCollection = locationCollection;
	}

	public void setFilter(Filter<Temporal> filter) {
		this.filter = filter;
	}

	@Override
	protected Void doInBackground(Void... params) {
		try {
			List<Location> locations = getQuery().query();
			publishProgress(locations.toArray(new Location[locations.size()]));
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	protected void onProgressUpdate(Location... locations) {
		synchronized (locationCollection) {
			locationCollection.addAll(new ArrayList<Location>(Arrays.asList(locations)));
		}
	}

	private QueryBuilder<Location, Long> getQuery() throws SQLException {
		Dao<Location, Long> dao = DaoStore.getInstance(context).getLocationDao();
		QueryBuilder<Location, Long> query = dao.queryBuilder();
		Where<? extends Temporal, Long> where = query.where().ge("timestamp", locationCollection.getLatestDate());
		User currentUser = null;
		try {
			currentUser = UserHelper.getInstance(context.getApplicationContext()).readCurrentUser();
		} catch (UserException e) {
			e.printStackTrace();
		}
		if (currentUser != null) {
			where.and().ne("user_id", currentUser.getId()).and().eq("event_id", currentUser.getCurrentEvent().getId());
		}
		if (filter != null) {
			where = filter.where(where.and());
		}
		query.orderBy("timestamp", false);

		return query;
	}
}