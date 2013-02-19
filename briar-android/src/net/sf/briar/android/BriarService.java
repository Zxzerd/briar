package net.sf.briar.android;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;

import java.io.IOException;
import java.util.logging.Logger;

import net.sf.briar.R;
import net.sf.briar.api.crypto.KeyManager;
import net.sf.briar.api.db.DatabaseComponent;
import net.sf.briar.api.db.DbException;
import net.sf.briar.api.plugins.PluginManager;
import roboguice.service.RoboService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.google.inject.Inject;

public class BriarService extends RoboService {

	private static final Logger LOG =
			Logger.getLogger(BriarService.class.getName());

	private final Binder binder = new BriarBinder();

	@Inject private DatabaseComponent db;
	@Inject private KeyManager keyManager;
	@Inject private PluginManager pluginManager;

	@Override
	public void onCreate() {
		super.onCreate();
		if(LOG.isLoggable(INFO)) LOG.info("Created");
		NotificationCompat.Builder b = new NotificationCompat.Builder(this);
		b.setSmallIcon(R.drawable.notification_icon);
		b.setContentTitle(getText(R.string.notification_title));
		b.setContentText(getText(R.string.notification_text));
		b.setOngoing(true);
		startForeground(1, b.build());
		if(LOG.isLoggable(INFO)) LOG.info("Running in the foreground");
		new Thread() {
			@Override
			public void run() {
				startServices();
			}
		}.start();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if(LOG.isLoggable(INFO)) LOG.info("Started");
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(LOG.isLoggable(INFO)) LOG.info("Destroyed");
		new Thread() {
			@Override
			public void run() {
				stopServices();
			}
		}.start();
	}

	private void startServices() {
		try {
			if(LOG.isLoggable(INFO)) LOG.info("Starting");
			db.open(false);
			if(LOG.isLoggable(INFO)) LOG.info("Database opened");
			keyManager.start();
			if(LOG.isLoggable(INFO)) LOG.info("Key manager started");
			int pluginsStarted = pluginManager.start(this);
			if(LOG.isLoggable(INFO))
				LOG.info(pluginsStarted + " plugins started");
		} catch(DbException e) {
			if(LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
		} catch(IOException e) {
			if(LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
		}
	}

	private void stopServices() {
		try {
			if(LOG.isLoggable(INFO)) LOG.info("Shutting down");
			int pluginsStopped = pluginManager.stop();
			if(LOG.isLoggable(INFO))
				LOG.info(pluginsStopped + " plugins stopped");
			keyManager.stop();
			if(LOG.isLoggable(INFO)) LOG.info("Key manager stopped");
			db.close();
			if(LOG.isLoggable(INFO)) LOG.info("Database closed");
		} catch(DbException e) {
			if(LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
		} catch(IOException e) {
			if(LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
		}
	}

	public class BriarBinder extends Binder {

		BriarService getService() {
			return BriarService.this;
		}
	}
}