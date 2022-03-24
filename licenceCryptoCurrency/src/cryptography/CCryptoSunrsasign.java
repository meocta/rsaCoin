package cryptography;

import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;

import main.CConfiguration;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/*
 *check if the Provider is installed on the current machine *
 */
public class CCryptoSunrsasign {
	static final String SunRsaSign    = "SunRsaSign";
	static final String SHA256withRSA = "SHA256withRSA";
	static final String SHA384withRSA = "SHA384withRSA";
	static final String SHA512withRSA = "SHA512withRSA";
	static final int 	keySize       = 4096;

	public static final int	keyHashSize   = 256;
	public static final int transactionHashSize = 512;

	static KeyPair key = null; //remove

	/*
	 * signs the data
	 * params:
	 *  dataUnsigned- raw data to be signed
	 *  alias - the alias under which the keys are stored
	 *  algSize - size of SHA
	 * return:
	 *  signed data 
	 */
	static public byte[] mSignRSADS( byte[] dataUnsigned, String alias, int algSize ){
		KeyPair keys = mGetKeyFromAlias(alias);
		byte[] dataSigned = null;
		String algorithm = null;
		
		switch( algSize ){
		case 256:
			algorithm = SHA256withRSA;
			break;
		case 384:
			algorithm = SHA384withRSA;
			break;
		default:
			algorithm = SHA512withRSA;
		}
		//todo: move to constructor. 
		try{
			mCheckValid( SunRsaSign );
		}
		catch( CCryptoException e ){
			e.printStackTrace();
		}
		try{
			Signature sgnObj = Signature.getInstance( algorithm );
			sgnObj.initSign( keys.getPrivate() );
			sgnObj.update( dataUnsigned );
			dataSigned = sgnObj.sign(); 
		}
		catch( NoSuchAlgorithmException | InvalidKeyException | SignatureException e ){
			e.printStackTrace();
		}
		return dataSigned;
	}

	/*
	 * verifies the signed data
	 * params:
	 *  dataUnsigned- raw data to be signed
	 *  dataSigned - data resulted after the signing operation.
	 *  verificationKey - the public key used for signing
	 *  algSize - size of SHA
	 * return:
	 *  the status of the operation 
	 */
	static public boolean mVerifyRSADS( byte[] dataUnsigned, byte[] dataSigned, PublicKey verificationKey, int algSize ){
		boolean status = false;
		String algorithm = null;

		switch( algSize ){
		case 256:
			algorithm = SHA256withRSA;
			break;
		case 384:
			algorithm = SHA512withRSA;
			break;
		default:
			algorithm = SHA384withRSA;
		}
		//move to constructor
		try{
			mCheckValid( SunRsaSign );
		}
		catch( CCryptoException e ){
			e.printStackTrace();
		}
		try{
			Signature sgnObj = Signature.getInstance( algorithm );
			sgnObj.initVerify( verificationKey );
			sgnObj.update( dataUnsigned );
			status = sgnObj.verify( dataSigned );
		}
		catch( NoSuchAlgorithmException | InvalidKeyException | SignatureException e ){
			e.printStackTrace();
		}
		return status;
	}

	/*
	 * computes the secure hash algorithm SHA of the input
	 * params:
	 *  alg - int taking values : 256, 384 or 512
	 *  input - the byte array of which hash is computed
	 * return:
	 * a byte array representing the digest of the input
	 */
	static public byte[] mMessageDigest( int alg, byte[] input ) throws NoSuchAlgorithmException{		
		String digestAlgorithm;		
		switch(alg){
		case 256:
			digestAlgorithm = "SHA-256";
			break;
		case 384:
			digestAlgorithm = "SHA-384";
			break;
		case 512:
			digestAlgorithm = "SHA-512";
			break;
		default:
			digestAlgorithm = "SHA-256";
			System.out.println( "only SHA 256, 384 and 512 are supported. Current defaulted to 256" );
		}
		MessageDigest md = MessageDigest.getInstance( digestAlgorithm );
		md.update(input);
		return md.digest();		
	}

	/*
	 * checks the existence of SunRsaSign provider on the local machine.
	 * if provider is not present, an exception is thrown
	 */
	static public void mCheckValid( String providerName ) throws CCryptoException{
		boolean providerFound = false;
		Provider[] providers = Security.getProviders();

		for( Provider prvd : providers){
			if( prvd.getName().contains( providerName ) ){
				providerFound = true;
			}
		}
		if( providerFound == false){
			throw new CCryptoException( providerName + " not installed" );
		} else {
			System.out.println( "provider present" );
		}
	}

	static public KeyPair mGetKeyFromAlias( String alias){
//		Path pathToKeysFolder = Paths.get( CConfiguration.keyFolder );
//		try{
//			createDirectory( pathToKeysFolder );
//		}
//		catch( FileAlreadyExistsException  e ){
//			System.out.println( "folder already in place" );
//			System.out.println( e.toString() );
//		}
//		catch( Exception e ){
//			e.printStackTrace();
//		}
		KeyPair keys = null;
		try{
			File keyFile = new File( CConfiguration.keyFolder + alias + ".key" );
			if( keyFile.exists() ){
				keys = mGetKeyFromStore( keyFile );
			} else {
				keyFile.createNewFile();
				keys = mGenerateKeyPair();
				mInsertKeyInStore(keyFile, keys);
			}
		}
		catch( NullPointerException | IOException | NoSuchAlgorithmException | NoSuchProviderException e){
			e.printStackTrace();
		}
		return keys;
	}

	static public byte[] getKeyHashFromAlias( String alias){
		byte[] keyHash = null;
		try{
			KeyPair keys = CCryptoSunrsasign.mGetKeyFromAlias( alias );
			PublicKey pKey = keys.getPublic();
			byte[] keySerial = CHelper.mGetByteFromSerial( pKey );
			keyHash = CCryptoSunrsasign.mMessageDigest( keyHashSize, keySerial );				
		}
		catch( NoSuchAlgorithmException e ){
			e.printStackTrace();
		}
		return keyHash;
	}

	/* private methods */

	/*
	 * generates the key pair for RSA algorithm, using SunRsaSign provider
	 * return:
	 * KeyPair containing the private/public keys
	 */
	static private KeyPair mGenerateKeyPair( ) throws NoSuchAlgorithmException, 
	NoSuchProviderException{
		KeyPairGenerator keysGenerator = KeyPairGenerator.getInstance( "RSA", SunRsaSign);
		keysGenerator.initialize( keySize );
		return keysGenerator.generateKeyPair();
	}

	/*
	 * retrieves the keys from files named as the key's aliases
	 * params:
	 * alias - the identification (alias) of this keys
	 * keys - the key pair representing the cryptoaddress 
	 */
	static private KeyPair mGetKeyFromStore( File keyFile ){
		KeyPair keys = null;
		try( 	FileInputStream keyInputStream = new FileInputStream( keyFile );
				ObjectInputStream keyInSerial = new ObjectInputStream( keyInputStream ); ){			
			keys = (KeyPair) keyInSerial.readObject();
			keyInSerial.close();
			keyInputStream.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return keys;
	}

	/*
	 * stores the keys in files named as the key's aliases
	 * params:
	 *  alias - the identification (alias) of this keys
	 *  keys - the key pair representing the cryptoaddress 
	 */
	static private void mInsertKeyInStore( File keyFile, KeyPair keys ){
		try( 	FileOutputStream keyOutputStream = new FileOutputStream( keyFile );
				ObjectOutputStream keyOutSerial = new ObjectOutputStream( keyOutputStream ); ){
			keyOutSerial.writeObject( keys );
			keyOutSerial.close();
			keyOutputStream.close();
		} 
		catch(Exception e){
			e.printStackTrace();
		}
	}

	////////////////////////////////////////////////////////////////////

	public static void main(String[] args) // todo: remove
	{
		String test = "aleluiah";

		byte[] sgn = mSignRSADS( test.getBytes(), "test", 384);

		System.out.println( key.getPublic().toString() );

		System.out.println( mVerifyRSADS(test.getBytes(), sgn, key.getPublic(), 384 ) );		
	}
}
