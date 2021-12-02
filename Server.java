// Java program to illustrate Server side
// Implementation using DatagramSocket
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;

public class Server
{
  public static DatagramSocket ds;

  public static byte[] receive;
  public static InetAddress clientIP;
  public static int clientPort;
  public static InetAddress nextServerIP;
  public static String nextServerName;

  public static DatagramPacket DpReceive;
  public static DatagramPacket response;

  public static InetAddress myIP;
  public static int myPort;
  public static int serverNum;
  
  public static int numberOfUsers;

    static HashMap<String, byte[]> localTable;
    public static void main(String[] args) throws IOException
    {
		numberOfUsers = 4;
		serverNum = Integer.parseInt(args[0]);
		myPort = 20153+serverNum;
		nextServerName = args[1];
		

        myIP = InetAddress.getLocalHost();
        

    		localTable = new HashMap();
        ds = new DatagramSocket(myPort);

        receive = new byte[65535];

        DpReceive = null;
        response = null;

        //populate for testing
        localTable.put("bunny", mergeIPPort(InetAddress.getLocalHost(), 20155));



        //Server 1 doesn't need to listen to other servers
        //due to direct communication with client
	
		if (serverNum>1) serverListen();    
        clientListen();
    }

    static void serverListen() throws IOException, SocketException{
      ServerSocket welcomeSocket = new ServerSocket(myPort);
	  Socket connectionSocket;
        InputStream in;
        DataInputStream dataStream;
		int length;
		byte[] dataBytes;
		byte[] myInfo;
		System.out.println("Listening for server requests...");
      
	  
      for(int initCounter = 0; initCounter<numberOfUsers; initCounter++){
        //Receive forwarded init
		connectionSocket = welcomeSocket.accept();
		in = connectionSocket.getInputStream();
		dataStream = new DataInputStream(in);
        length = dataStream.readInt();
        dataBytes = new byte[length];
        dataStream.read(dataBytes, 0, length);

        if(data(dataBytes).length()>4 &&
           data(dataBytes).toString().substring(0, 4).equals("init")){

          //respond to user
          clientIP = InetAddress.getByAddress(Arrays.copyOfRange(dataBytes, 4, 8));
          clientPort = Integer.parseInt(data(Arrays.copyOfRange(dataBytes, 8, dataBytes.length)).toString());

          //make package
          myInfo = mergeIPPort(myIP, myPort);
          response = new DatagramPacket(myInfo, myInfo.length, clientIP, clientPort);
          ds.send(response);
		 System.out.println("Replied to user!");
		 

          //Forward to next server
		  if (serverNum<4){
			nextServerIP = InetAddress.getByName(nextServerName);
			Socket clientSocket = new Socket(nextServerIP, myPort+1);
			DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
			outToServer.writeInt(receive.length);
			outToServer.write(dataBytes, 0, dataBytes.length);
		  }
        }


      }
    }

    static void clientListen() throws IOException, SocketException{
      while (true)
      {
		System.out.println("Listening for user request...");
          DpReceive = new DatagramPacket(receive, receive.length);

          ds.receive(DpReceive);
          clientPort = DpReceive.getPort();
          clientIP = DpReceive.getAddress();
          //Extract these two from the package

          System.out.println("Client message:- " + data(receive) + " Client Port:-" + clientPort);

          //Exit the server if the client sends "exit"
          if (data(receive).toString().equals("exit")) {
              //Tell other servers to exit over TCP
			  nextServerIP = InetAddress.getByName(nextServerName); //make it getbyname
				
				if(serverNum<4){
					Socket clientSocket = new Socket(nextServerIP, myPort+1);
				DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
				outToServer.writeInt(receive.length);
				outToServer.write(receive, 0, receive.length);
				}
				

              //Exit myself
              System.out.println("Exiting, goodbye!");

              break;
          }
          //return all server IPs to user
          else  if (data(receive).length()>4 &&
                    data(receive).toString().substring(0, 4).equals("init")) {
              System.out.println("Fetching all IPs");

              //Instead of localhost, get actual hostname
              //Forward the init
			  if(serverNum<4){ // Don't open up socket with next server if last server
				nextServerIP = InetAddress.getByName(nextServerName); //make it getbyname
				
				Socket clientSocket = new Socket(nextServerIP, myPort+1);
				DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
				outToServer.writeInt(receive.length);
				outToServer.write(receive, 0, receive.length);
  
			  }
              
              //return my own IP to client
              //explicitly get clientIP and port from received message
              //instead of taking it using getPort and getAddress
              clientIP = InetAddress.getByAddress(Arrays.copyOfRange(receive, 4, 8));
              clientPort = Integer.parseInt(data(Arrays.copyOfRange(receive, 8, receive.length)).toString());

              byte[] myInfo = mergeIPPort(myIP, myPort);
              response = new DatagramPacket(myInfo, myInfo.length, clientIP, clientPort);
              ds.send(response);
          }
          //Uploading new image into directory
          else if (data(receive).length()>6 &&
                   data(receive).toString().substring(0, 6).equals("upload")) {
              String filename = data(Arrays.copyOfRange(receive, 6, receive.length)).toString();
              System.out.println("Uploading file " + filename);
              localTable.put(filename, mergeIPPort(clientIP, clientPort));
              System.out.println("Local table is now \n" + localTable);
          }
          //Retrieving image and sending IP&port of user who has that image to the Client
          else if (localTable.containsKey(data(receive).toString())) {
              System.out.println("Fetching user who has image " + data(receive).toString());
              byte[] requestInfo = localTable.get(data(receive).toString());
              response = new DatagramPacket(requestInfo, requestInfo.length, clientIP, clientPort);
              ds.send(response);
          }
          else {
              System.out.println("Sorry, unknown command");
          }
          // Clear the buffer after every message.
          receive = new byte[65535];
      }
    }
    // A utility method to convert the byte array data into a string representation.
    public static StringBuilder data(byte[] a){
        if (a == null)
            return null;
        StringBuilder ret = new StringBuilder();
        int i = 0;
        while (a[i] != 0)
        {
            ret.append((char) a[i]);
            i++;
        }
        return ret;
    }

    // A utility method to merge a given ip and port into a single byte array
    public static byte[] mergeIPPort(InetAddress ip, int port){
      byte[] ipBytes = ip.getAddress();
      byte[] portBytes = Integer.toString(port).getBytes();
      byte[] result = new byte[ipBytes.length + portBytes.length];
      System.arraycopy(ipBytes, 0, result, 0, ipBytes.length);
      System.arraycopy(portBytes, 0, result, ipBytes.length, portBytes.length);
      return result;
    }

    // A utility method to extract IP out of a byte array containing IP and port
    // Exctracts first 4 bytes
    public static InetAddress extractIP(byte[] bytes)throws UnknownHostException{
      return InetAddress.getByAddress(Arrays.copyOfRange(bytes, 0, 4));
    }

    // A utility method to extract Port out of a byte array containing IP and port
    // Exctracts the bytes after the first 4 bytes
    public static int extractPort(byte[] bytes){
      return Integer.parseInt(data(Arrays.copyOfRange(bytes, 4, bytes.length)).toString());
    }
}
