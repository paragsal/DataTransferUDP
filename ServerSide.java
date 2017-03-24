import java.io.BufferedReader;
import java.io.FileReader;
import java.net.*;


public class RndServer
{
	public static void main(String[] args) throws Exception
	{
		byte[] rcvdData= new byte[1024];
 
		DatagramPacket rcvdPacket = new DatagramPacket(rcvdData, rcvdData.length);   //Create datagram packet for receiving the data
		DatagramSocket sockServer = new DatagramSocket(9477);                        //Create a datagram Socket
		 
		FileReader file = new FileReader("C:/Users/Parag/Desktop/data.txt");
		BufferedReader reader = new BufferedReader(file);
		String line = null;
		int[] mID = new int[100];
		String[] value = new String[100];
		int i = 0;
		while((line = reader.readLine()) != null)
		{
			String[] temp = line.split("	");                    //Separate the two columns for creating two arrays
			mID[i] = (Integer.parseInt(temp[0])) & 0xffff;         //Convret measurement  ID to 16 bit unsigned integer
			value[i] = temp[1];
			i++;
		}
		
		while(true)
		{
			sockServer.receive(rcvdPacket);                       //Receive the datagram packets
			rcvdData = rcvdPacket.getData();
			String rcvdString = new String(rcvdData);
			rcvdString = rcvdString.trim();
			int x = rcvdString.lastIndexOf('>');
			String rcvdString1 = rcvdString.substring(0, x+1);    //First part: Actual content of the request message
			
			int m = IntegrityCheck(rcvdString1);                  //Calculate the checksum values
	
			String rcvdString2 = rcvdString.substring(x+1, rcvdString.length());  //Integrity value sent by the client
			if(rcvdString2.length()>String.valueOf(m).length())
			{
				rcvdString2 = rcvdString2.substring(0,String.valueOf(m).length());
			}
	
			int s = (Integer.parseInt(rcvdString2)) & 0xffff;     //Convert the received checksum values to 16 bit unsigned integer

			int a = rcvdString.indexOf('<',rcvdString.indexOf('>',rcvdString.indexOf('m')));
			String ment = rcvdString.substring(rcvdString.indexOf('>',rcvdString.indexOf('m'))+1,a);
	
			String req = rcvdString.substring(13, rcvdString.indexOf('<',12));
			
			int count = 0, code = 0;
			
			if(m != s)
				  code = 1;                                  //When there are bit errors
			else
			{
				for(int j=0;j<mID.length;j++)
				{
					if((Integer.parseInt(ment) & 0xffff) == (mID[j]))
					{
						code = 0;                            //When there are no errors
						for(i=0;i<mID.length;i++)
						{
							if((Integer.parseInt(ment) & 0xffff) == (mID[i]))
							{
								count = i;
								break;
							}
						}
						break;
					}	
					else code = 3;                      //When the measurement ID sent by the client does not exist
				}
				
				if(code != 3)
				{                                                          //To check if the syntax is correct
					String s1 = "<request><id>";
					String s2 = "</id><measurement>";
					String s3 = "</measurement></request>";
					int b = rcvdString.indexOf('<',s1.length());
					if((!rcvdString.regionMatches(0, s1, 0, s1.length())))
						code =2;
					else if((!rcvdString.regionMatches(b, s2, 0, s2.length())))
							code = 2;
					else if((!rcvdString.regionMatches(a, s3, 0, s3.length())))
							code = 2;
				}
			}
			
			InetAddress cHost = rcvdPacket.getAddress();           //Extract the IP address of the client from the received packet
			int cPort = rcvdPacket.getPort();                      //Extract the port number of the client from the received packet
			
			String sendString = new String();
			if(code == 0)
			{
				sendString = "<response><id>"+req+"</id><code>"+code+"</code><measurement>"+ment+"</measurement><value>"+value[count]+"</value></response>";
			}
			else 
				sendString = "<response><id>"+req+"</id><code>"+code+"</code></response>";
			
			sendString = sendString.replaceAll("\\s","");         //Remove whitespaces
			int l = IntegrityCheck(sendString);                   //Calculate checksum
			String sendString2 = String.valueOf(l);
			String sendStringFinal = sendString+sendString2;      //Append the checksum value to the response message
			System.out.println("Sending string "+sendStringFinal);
			byte[] sendData = sendStringFinal.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, cHost, cPort);
			sockServer.send(sendPacket);                          //Send the response message
		}
	}  //main
	
	public static int IntegrityCheck(String sendString)
	{
		byte msb = 0, lsb = 0;
	    int len = sendString.length();
		int[] sendShort = new int[sendString.length()];
		if((len%2) != 0)
			len++;
		for(int i=0; i<len/2; i++)
		{
			msb = (byte)sendString.charAt(2*i);
			if((sendString.length()%2 != 0) && (i == ((len/2)-1)))
			{
				lsb = 0;                                          //If odd number of characters then last byte should be 0
			}
			else
			{
				lsb = (byte)sendString.charAt(2*i+1);
			}
			sendShort[i] =  ((msb << 8) | (lsb & 0xFF)) & 0xffff;  //Create a 16 bit word of two adjacent 8 bit characters
		}
		
		  // Using the formula
		int s = 0, ind;
		for(int i=0; i<sendShort.length; i++)
		{
			ind = (s^sendShort[i]) & 0xffff;
			s = ((7919*ind)%65536) & 0xffff;
		}
		return s;                                     //Return 16 bit unsigned integer checksum value
	}
}