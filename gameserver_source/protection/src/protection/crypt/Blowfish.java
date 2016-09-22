package protection.crypt;

import java.io.IOException;






/**
 * Developers: Redist Team<br>
 * <br>
 * <br>
 * Author: Redist<br>
 * Date: 15 февр. 2016 г.<br>
 * Time: 17:48:41<br>
 * <br>
 */
public class Blowfish{
	private static class SingletonHolder
	{
		protected static final Blowfish _instance = new Blowfish();
	}
	
	public static final Blowfish getInstance()
	{
		return SingletonHolder._instance;
	}

	private final byte[] _key = new byte[16];
	public  byte[] getKey(byte[] key)
	{
		byte[] bfkey = { 33, 31, 22, 18, 10, 66, 24, 23, 18, 45, 111, 21, 122, 16, -5, 13 };
		try
		{
			BlowfishEngine bf = new BlowfishEngine();
			bf.init(true, bfkey);
			bf.processBlock(key, 0, _key, 0);
			bf.processBlock(key, 8, _key, 8);
		}
		catch(IOException e)
		{
			
		}
		return _key;
	}
}
