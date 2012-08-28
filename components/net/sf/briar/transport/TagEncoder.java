package net.sf.briar.transport;

import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;
import static net.sf.briar.api.transport.TransportConstants.TAG_LENGTH;

import java.security.GeneralSecurityException;

import javax.crypto.Cipher;

import net.sf.briar.api.crypto.ErasableKey;

class TagEncoder {

	static void encodeTag(byte[] tag, Cipher tagCipher, ErasableKey tagKey) {
		if(tag.length < TAG_LENGTH) throw new IllegalArgumentException();
		// Blank plaintext
		for(int i = 0; i < TAG_LENGTH; i++) tag[i] = 0;
		try {
			tagCipher.init(ENCRYPT_MODE, tagKey);
			int encrypted = tagCipher.doFinal(tag, 0, TAG_LENGTH, tag);
			if(encrypted != TAG_LENGTH) throw new IllegalArgumentException();
		} catch(GeneralSecurityException e) {
			// Unsuitable cipher or key
			throw new IllegalArgumentException(e);
		}
	}

	static boolean decodeTag(byte[] tag, Cipher tagCipher, ErasableKey tagKey) {
		if(tag.length < TAG_LENGTH) throw new IllegalArgumentException();
		try {
			tagCipher.init(DECRYPT_MODE, tagKey);
			int decrypted = tagCipher.doFinal(tag, 0, TAG_LENGTH, tag);
			if(decrypted != TAG_LENGTH) throw new IllegalArgumentException();
			//The plaintext should be blank
			for(int i = 0; i < TAG_LENGTH; i++) if(tag[i] != 0) return false;
			return true;
		} catch(GeneralSecurityException e) {
			// Unsuitable cipher or key
			throw new IllegalArgumentException(e);
		}
	}
}
