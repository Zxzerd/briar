package org.briarproject.bramble.plugin.file;

import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.bramble.api.plugin.TransportConnectionWriter;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

import static java.util.logging.Level.WARNING;

@NotNullByDefault
class FileTransportWriter implements TransportConnectionWriter {

	private static final Logger LOG =
			Logger.getLogger(FileTransportWriter.class.getName());

	private final File file;
	private final OutputStream out;
	private final FilePlugin plugin;

	FileTransportWriter(File file, OutputStream out, FilePlugin plugin) {
		this.file = file;
		this.out = out;
		this.plugin = plugin;
	}

	@Override
	public int getMaxLatency() {
		return plugin.getMaxLatency();
	}

	@Override
	public int getMaxIdleTime() {
		return plugin.getMaxIdleTime();
	}

	@Override
	public OutputStream getOutputStream() {
		return out;
	}

	@Override
	public void dispose(boolean exception) {
		try {
			out.close();
		} catch (IOException e) {
			if (LOG.isLoggable(WARNING)) LOG.log(WARNING, e.toString(), e);
		}
		plugin.writerFinished(file, exception);
	}
}
