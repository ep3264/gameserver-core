package protection.crypt;



/**
 * Developers: Redist Team<br>
 * <br>
 * <br>
 * Author: Redist<br>
 * Date: 16 февр. 2016 г.<br>
 * Time: 0:03:40<br>
 * <br>
 */
public class RC5
{
	final int w  =     32   ;          /* word size in bits                 */
	final int r   =     12    ;         /* number of rounds                  */  
	final int b    =    16     ;        /* number of bytes in key            */
	final int c     =    4      ;       /* number  words in key = ceil(8*b/w)*/
	final int t      =  26     ;
	final int P = 0xb7e15163, Q = 0x9e3779b9; 
	int S[];
	final byte K[]= { 83, 61, 33, 68, 6, 71, 108, 18, -12, 18, 104, 91, 7, -85, -115, 101 };
	private static class SingletonHolder
	{
		protected static final RC5 _instance = new RC5();
	}
	
	public static final RC5 getInstance()
	{
		return SingletonHolder._instance;
	}
	RC5()
	{
		S= new int[t];	
		RC5_SETUP();
		
	}
	
	byte[] toByteArray(int value) {
	    return new byte[] { 
	        (byte)(value >> 24),
	        (byte)(value >> 16),
	        (byte)(value >> 8),
	        (byte)value };
	}	
	int fromByteArray(byte[] bytes, int index) {
	     return bytes[index+3] << 24 | (bytes[index+2] & 0xFF) << 16 | (bytes[index+1] & 0xFF) << 8 | (bytes[index] & 0xFF);
	}
	void toByteArray(int value, byte[] arr, int index) {	   
	       arr[index+3] =(byte)(value >> 24);
	       arr[index+2]= (byte)(value >> 16);
	       arr[index+1]=(byte)(value >> 8);
	       arr[index]=(byte)value; 
	}
	
	public void ndecrypt(byte[] raw, int size, int offset, int iv1, int iv2)
	{
		int iv[] = new int[2];
		iv[0]= iv1 ;
		iv[1]=  iv2;		
		byte op=raw[offset];	
		int ot;
	    ot=	0 << 24 | (0 & 0xFF) << 16 | (0 & 0xFF) << 8 | (op & 0xFF);
		RC5_ENCRYPT(iv, iv);
		ot ^= iv[0];		
	    raw[offset]= (byte)ot;				
	}
	void RC5_SETUP() /* secret input key K[0...b-1]      */
	{
		 int  i, j, k, u = w / 8;
		 int A, B ;
         int L[] = new int[c];
		/* Initialize L, then S, then mix key into S */
		for (i = b - 1, L[c - 1] = 0; i != -1; i--) L[i / u] = (L[i / u] << 8) + (K[i]& 0xff);
		for (S[0] = P, i = 1; i<t; i++) S[i] = S[i - 1] + Q;
		for (A = B = i = j = k = 0; k<3 * t; k++, i = (i + 1) % t, j = (j + 1) % c)   /* 3*t > 3*c */
		{
			S[i]=A =  (((S[i] + (A + B))<<(3&(w-1))) | ((S[i] + (A + B))>>>(w-(3&(w-1)))));			
			L[j] =B =  (((L[j] + (A + B))<<((A + B)&(w-1))) | ((L[j] + (A + B))>>>(w-((A + B)&(w-1))))); 
		}
	}
	void RC5_DECRYPT( int []ct,  int [] pt) /* 2 unsigned int int input ct/output pt    */
	{
		 int  i;
		 int B = ct[1], A = ct[0];
		for (i = r; i>0; i--)
		{
			B = (((B - S[2 * i + 1])>>>(A&(w-1))) | ((B - S[2 * i + 1])<<(w-(A&(w-1)))))^A; 
			A = (((A - S[2 * i])>>>(B&(w-1))) | ((A - S[2 * i])<<(w-(B&(w-1)))))^B; 
		}
		pt[1] = B - S[1]; pt[0] = A - S[0];
	}
	void RC5_ENCRYPT( int []pt,  int []ct) /* 2 unsigned long int input pt/output ct    */
	{
		int i, A = pt[0] + S[0], B = pt[1] + S[1];
		for (i = 1; i <= r; i++)
		{
			A = (((A^B)<<(B&(w-1))) | ((A^B)>>>(w-(B&(w-1)))))+ S[2 * i]; 
			B =(((B^A)<<(A&(w-1))) | ((B^A)>>>(w-(A&(w-1))))) + S[2 * i + 1];
		}
		ct[0] = A; ct[1] = B;
	}
}
