import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Scanner;
import java.util.Random;
import java.net.*;
import java.lang.Integer;

public class ClientSide
{
	private static final int MAX_ATTEMPTS = 4;
	
	public static void main(String[] args) throws Exception
	{
	
		//1. Read the file containing measurement IDs
		FileReader file = new FileReader("C:/Users/Parag/Desktop/clientdata.txt");
		BufferedReader reader = new BufferedReader(file);
		
		String[] text = new String[100];
		String line = reader.readLine();
		int[] inp = new int[100];
		for(int i=0; line != null; i++)
		{
			text[i] = line;                             //Storing the contents of the file in a String array
			inp[i] = Integer.parseInt(text[i]) ;        //Converting the String array to an array of integers
			inp[i] = inp[i] & 0xffff;                   //Convert to 16-bit unsigned integer
			line  = reader.readLine();
		}
	
		while(true)
		{
		
			Random randGen1 = new Random();
			int measIndex = randGen1.nextInt(100);      //Generate a random index to choose the measurement ID
			int measurementID = inp[measIndex];         //Choose the measurement ID corresponding to the random index
			
			int y=0, z=1;
			String recdString1, recdString2;
			
			Scanner input = new Scanner(System.in);
			String status="n";
			
			do
			{
			
				do
				{
					//2. Generate a random Request ID
				
					Random randGen = new Random();                           
					int requestID = (randGen.nextInt(Short.MAX_VALUE)+1) & 0xffff;      //Generate a 16-bit random Request ID
				
					//Creating the request message string object
					String sendString = "<request><id>"+requestID+"</id><measurement>"+measurementID+"</measurement></request>";
					sendString = sendString.replaceAll("\\s","");                  //removes whitespaces  
				
				   
					//2.a Integrity Check
					int s = IntegrityCheck(sendString);                            //Integrity Check values
					String sendString2 = String.valueOf(s);                        //Convert the checksum values to string
					
					String sendStringFinal = sendString+sendString2;               //Appending Checksum value at the end of the request message
					
					byte[] sendBytesFinal = sendStringFinal.getBytes();            //Byte array to send the data
					
				    
					
				   //2.b Creating UDP packet and sending byte[] array
				
					InetAddress hostname = InetAddress.getLocalHost();
					DatagramPacket data = new DatagramPacket(sendBytesFinal, sendBytesFinal.length, hostname, 9477);
					DatagramSocket sock = new DatagramSocket();
					
					byte[] recdata = new byte[1024];
					DatagramPacket data1 = new DatagramPacket(recdata, recdata.length);
					
					int time = 1000, a = 1;                            //Initial Timeout value and timeout counter
				
				
					while(a<=MAX_ATTEMPTS)
					{
						sock.send(data);                               //Sending the packet
						sock.setSoTimeout(time);                       
						time = time*2;                                 //Double the timeout in every iteration
						try
						{
							sock.receive(data1);
							a=5;                                       //To come out of the loop if the data is received;
					 	}
					 	catch(SocketTimeoutException e)
					 	{
						 	if(a==4)
						 		System.out.println("Couldn't establish a connection");
						 	a++;
					 	}
					}
					time = 1000;                                        //reset the timeout value
					a=1;                                                //reset the timeout counter
				
					byte[] recd = data1.getData();                       
					String recdString = new String(recd);               //Convert the received bytes to string
					recdString = recdString.trim();
					int x = recdString.lastIndexOf('>');                //Index of the last character of the message, before the checksum value
					recdString1 = recdString.substring(0, x+1);         //First part: Actual content of the response message
					
	
					y = IntegrityCheck(recdString1);                               //Calculate the checksum value
					
					
					recdString2 = recdString.substring(x+1, recdString.length());  //Checksum value sent by the server
					z = (Integer.parseInt(recdString2)) & 0xffff;                  //Convert to 16-bit unsigned integer
				
				}
				while(y != z);                       //Resend the request if the checksum doesn't match
		
			
			
				int indexCode = recdString1.indexOf("</code>");
				int code = recdString1.charAt(indexCode-1);          
				String ReceivedMeasVal = "";
				if(code=='0')                                    //Extract the temperature value
				{
					ReceivedMeasVal = recdString1.substring(recdString1.indexOf("ue>")+3, (recdString1.indexOf("</v"))-1);
				}
				
				switch(code)
				{
					case '0': ReceivedMeasVal = recdString1.substring(recdString1.indexOf("ue>")+3, (recdString1.indexOf("</v"))); 
							System.out.println("The temperature is: "+ReceivedMeasVal+" F");
							break;
					case '1': System.out.println("Integrity Check failure at the server.");
							System.out.println("Do you want to send the request again? (y/n)");
							status = input.next();
							break;
					case '2': System.out.println("The syntax of the request message is not correct.");
							break;
					case '3': System.out.println("The measurement with the requested Measurement ID does not exist.");
							break;
				}
			}
			while(status.charAt(0)=='y');
		}
	} //main
	
	
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
				lsb = 0;                                           //If odd number of characters then last byte should be 0
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
		return s;                             //Return 16 bit unsigned integer checksum value
	}
}