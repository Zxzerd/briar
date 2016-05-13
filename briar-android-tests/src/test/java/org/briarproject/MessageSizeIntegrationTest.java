package org.briarproject;

import org.briarproject.api.UniqueId;
import org.briarproject.api.crypto.CryptoComponent;
import org.briarproject.api.identity.AuthorFactory;
import org.briarproject.api.messaging.MessagingConstants;
import org.briarproject.api.messaging.PrivateMessage;
import org.briarproject.api.messaging.PrivateMessageFactory;
import org.briarproject.api.sync.GroupId;
import org.briarproject.api.sync.MessageId;
import org.briarproject.system.SystemModule;
import org.junit.Test;

import javax.inject.Inject;

import static org.briarproject.api.messaging.MessagingConstants.MAX_PRIVATE_MESSAGE_BODY_LENGTH;
import static org.briarproject.api.sync.SyncConstants.MAX_PACKET_PAYLOAD_LENGTH;
import static org.junit.Assert.assertTrue;

public class MessageSizeIntegrationTest extends BriarTestCase {

	@Inject
	CryptoComponent crypto;
	@Inject
	AuthorFactory authorFactory;
	@Inject
	PrivateMessageFactory privateMessageFactory;

	public MessageSizeIntegrationTest() throws Exception {
		MessageSizeIntegrationTestComponent component =
				DaggerMessageSizeIntegrationTestComponent.builder().build();
		component.inject(this);
		component.inject(new SystemModule.EagerSingletons());
	}

	@Test
	public void testPrivateMessageFitsIntoPacket() throws Exception {
		// Create a maximum-length private message
		GroupId groupId = new GroupId(TestUtils.getRandomId());
		long timestamp = Long.MAX_VALUE;
		MessageId parent = new MessageId(TestUtils.getRandomId());
		String contentType = TestUtils.getRandomString(
				MessagingConstants.MAX_CONTENT_TYPE_LENGTH);
		byte[] body = new byte[MAX_PRIVATE_MESSAGE_BODY_LENGTH];
		PrivateMessage message = privateMessageFactory.createPrivateMessage(
				groupId, timestamp, parent, contentType, body);
		// Check the size of the serialised message
		int length = message.getMessage().getRaw().length;
		assertTrue(length > UniqueId.LENGTH + 8 + UniqueId.LENGTH
				+ MessagingConstants.MAX_CONTENT_TYPE_LENGTH
				+ MAX_PRIVATE_MESSAGE_BODY_LENGTH);
		assertTrue(length <= MAX_PACKET_PAYLOAD_LENGTH);
	}
}