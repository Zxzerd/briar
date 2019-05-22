package org.briarproject.bramble.contact;

import org.briarproject.bramble.api.FormatException;
import org.briarproject.bramble.api.Pair;
import org.briarproject.bramble.api.contact.Contact;
import org.briarproject.bramble.api.contact.ContactId;
import org.briarproject.bramble.api.contact.ContactManager;
import org.briarproject.bramble.api.contact.PendingContact;
import org.briarproject.bramble.api.contact.PendingContactId;
import org.briarproject.bramble.api.contact.PendingContactState;
import org.briarproject.bramble.api.crypto.SecretKey;
import org.briarproject.bramble.api.db.DatabaseComponent;
import org.briarproject.bramble.api.db.DbException;
import org.briarproject.bramble.api.db.NoSuchContactException;
import org.briarproject.bramble.api.db.Transaction;
import org.briarproject.bramble.api.identity.Author;
import org.briarproject.bramble.api.identity.AuthorId;
import org.briarproject.bramble.api.identity.AuthorInfo;
import org.briarproject.bramble.api.identity.IdentityManager;
import org.briarproject.bramble.api.identity.LocalAuthor;
import org.briarproject.bramble.api.nullsafety.NotNullByDefault;
import org.briarproject.bramble.api.transport.KeyManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.inject.Inject;

import static org.briarproject.bramble.api.contact.HandshakeLinkConstants.BASE32_LINK_BYTES;
import static org.briarproject.bramble.api.contact.PendingContactState.WAITING_FOR_CONNECTION;
import static org.briarproject.bramble.api.identity.AuthorConstants.MAX_AUTHOR_NAME_LENGTH;
import static org.briarproject.bramble.api.identity.AuthorInfo.Status.OURSELVES;
import static org.briarproject.bramble.api.identity.AuthorInfo.Status.UNKNOWN;
import static org.briarproject.bramble.api.identity.AuthorInfo.Status.UNVERIFIED;
import static org.briarproject.bramble.api.identity.AuthorInfo.Status.VERIFIED;
import static org.briarproject.bramble.util.StringUtils.getRandomBase32String;
import static org.briarproject.bramble.util.StringUtils.toUtf8;

@ThreadSafe
@NotNullByDefault
class ContactManagerImpl implements ContactManager {

	private static final String REMOTE_CONTACT_LINK =
			"briar://" + getRandomBase32String(BASE32_LINK_BYTES);

	private final DatabaseComponent db;
	private final KeyManager keyManager;
	private final IdentityManager identityManager;
	private final PendingContactFactory pendingContactFactory;
	private final List<ContactHook> hooks;

	@Inject
	ContactManagerImpl(DatabaseComponent db, KeyManager keyManager,
			IdentityManager identityManager,
			PendingContactFactory pendingContactFactory) {
		this.db = db;
		this.keyManager = keyManager;
		this.identityManager = identityManager;
		this.pendingContactFactory = pendingContactFactory;
		hooks = new CopyOnWriteArrayList<>();
	}

	@Override
	public void registerContactHook(ContactHook hook) {
		hooks.add(hook);
	}

	@Override
	public ContactId addContact(Transaction txn, Author remote, AuthorId local,
			SecretKey rootKey, long timestamp, boolean alice, boolean verified,
			boolean active) throws DbException {
		ContactId c = db.addContact(txn, remote, local, verified);
		keyManager.addContactWithRotationKeys(txn, c, rootKey, timestamp,
				alice, active);
		Contact contact = db.getContact(txn, c);
		for (ContactHook hook : hooks) hook.addingContact(txn, contact);
		return c;
	}

	@Override
	public ContactId addContact(Transaction txn, Author remote, AuthorId local,
			boolean verified) throws DbException {
		ContactId c = db.addContact(txn, remote, local, verified);
		Contact contact = db.getContact(txn, c);
		for (ContactHook hook : hooks) hook.addingContact(txn, contact);
		return c;
	}

	@Override
	public ContactId addContact(Author remote, AuthorId local,
			SecretKey rootKey, long timestamp, boolean alice, boolean verified,
			boolean active) throws DbException {
		return db.transactionWithResult(false, txn ->
				addContact(txn, remote, local, rootKey, timestamp, alice,
						verified, active));
	}

	@Override
	public String getHandshakeLink() {
		// TODO replace with real implementation
		return REMOTE_CONTACT_LINK;
	}

	@Override
	public PendingContact addPendingContact(String link, String alias)
			throws DbException, FormatException {
		PendingContact p =
				pendingContactFactory.createPendingContact(link, alias);
		db.transaction(false, txn -> db.addPendingContact(txn, p));
		return p;
	}

	@Override
	public Collection<Pair<PendingContact, PendingContactState>> getPendingContacts()
			throws DbException {
		Collection<PendingContact> pendingContacts =
				db.transactionWithResult(true, db::getPendingContacts);
		List<Pair<PendingContact, PendingContactState>> pairs =
				new ArrayList<>(pendingContacts.size());
		for (PendingContact p : pendingContacts) {
			pairs.add(new Pair<>(p, WAITING_FOR_CONNECTION)); // TODO
		}
		return pairs;
	}

	@Override
	public void removePendingContact(PendingContactId p) throws DbException {
		db.transaction(false, txn -> db.removePendingContact(txn, p));
	}

	@Override
	public Contact getContact(ContactId c) throws DbException {
		return db.transactionWithResult(true, txn -> db.getContact(txn, c));
	}

	@Override
	public Contact getContact(AuthorId remoteAuthorId, AuthorId localAuthorId)
			throws DbException {
		return db.transactionWithResult(true, txn ->
				getContact(txn, remoteAuthorId, localAuthorId));
	}

	@Override
	public Contact getContact(Transaction txn, AuthorId remoteAuthorId,
			AuthorId localAuthorId) throws DbException {
		Collection<Contact> contacts =
				db.getContactsByAuthorId(txn, remoteAuthorId);
		for (Contact c : contacts) {
			if (c.getLocalAuthorId().equals(localAuthorId)) {
				return c;
			}
		}
		throw new NoSuchContactException();
	}

	@Override
	public Collection<Contact> getContacts() throws DbException {
		return db.transactionWithResult(true, db::getContacts);
	}

	@Override
	public void removeContact(ContactId c) throws DbException {
		db.transaction(false, txn -> removeContact(txn, c));
	}

	@Override
	public void setContactAlias(Transaction txn, ContactId c,
			@Nullable String alias) throws DbException {
		if (alias != null) {
			int aliasLength = toUtf8(alias).length;
			if (aliasLength == 0 || aliasLength > MAX_AUTHOR_NAME_LENGTH)
				throw new IllegalArgumentException();
		}
		db.setContactAlias(txn, c, alias);
	}

	@Override
	public void setContactAlias(ContactId c, @Nullable String alias)
			throws DbException {
		db.transaction(false, txn -> setContactAlias(txn, c, alias));
	}

	@Override
	public boolean contactExists(Transaction txn, AuthorId remoteAuthorId,
			AuthorId localAuthorId) throws DbException {
		return db.containsContact(txn, remoteAuthorId, localAuthorId);
	}

	@Override
	public boolean contactExists(AuthorId remoteAuthorId,
			AuthorId localAuthorId) throws DbException {
		return db.transactionWithResult(true, txn ->
				contactExists(txn, remoteAuthorId, localAuthorId));
	}

	@Override
	public void removeContact(Transaction txn, ContactId c)
			throws DbException {
		Contact contact = db.getContact(txn, c);
		for (ContactHook hook : hooks) hook.removingContact(txn, contact);
		db.removeContact(txn, c);
	}

	@Override
	public AuthorInfo getAuthorInfo(AuthorId a) throws DbException {
		return db.transactionWithResult(true, txn -> getAuthorInfo(txn, a));
	}

	@Override
	public AuthorInfo getAuthorInfo(Transaction txn, AuthorId authorId)
			throws DbException {
		LocalAuthor localAuthor = identityManager.getLocalAuthor(txn);
		if (localAuthor.getId().equals(authorId))
			return new AuthorInfo(OURSELVES);
		Collection<Contact> contacts = db.getContactsByAuthorId(txn, authorId);
		if (contacts.isEmpty()) return new AuthorInfo(UNKNOWN);
		if (contacts.size() > 1) throw new AssertionError();
		Contact c = contacts.iterator().next();
		if (c.isVerified()) return new AuthorInfo(VERIFIED, c.getAlias());
		else return new AuthorInfo(UNVERIFIED, c.getAlias());
	}

}
