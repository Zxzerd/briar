package org.briarproject;

import org.briarproject.crypto.CryptoModule;
import org.briarproject.sync.SyncModule;
import org.briarproject.transport.TransportModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
		TestSeedProviderModule.class,
		CryptoModule.class,
		SyncModule.class,
		TransportModule.class
})
public interface SyncIntegrationTestComponent {
	void inject(SyncIntegrationTest testCase);
}