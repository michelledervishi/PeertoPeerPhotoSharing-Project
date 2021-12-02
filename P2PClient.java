import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import javax.imageio.*;

public class P2PClient {

    public static void main(String[] args) throws Exception {
        int portNum;
        int serverPortNum;
        if (args[0].equals("1"))
            {portNum = 20158;
            serverPortNum = 20159;}
        else 
            {portNum = 20159;
            serverPortNum = 20158;}
        String fileName;

    
        P2PServer server = new P2PServer(serverPortNum);
        new Thread(server).start();
    
        //createInputStream
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        //ask user for desired file, append .jpg
        fileName = inFromUser.readLine() + ".jpg";
        
        getFile(fileName, portNum);

    }

    public static void getFile(String fileName, int portNum) {
        String path = "C:\\Users\\Nick\\cps706\\NetworksProj\\imgdstn\\";
        String hostName = "localhost";
        byte[] imageBytes;
        int imageSize;
        BufferedImage image;   
        
        try {
        //create client socket, connect to server
        Socket clientSocket = new Socket(hostName, portNum);

        //create output stream attached to socket
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());

        //create input stream attached to socket
        DataInputStream inFromServer = new DataInputStream(clientSocket.getInputStream());
    
        //send filename to server
        outToServer.writeBytes(fileName + "\n");

        //read image size as byte array and image as byte array
        byte[] imgSizeBytes = new byte[4];
        inFromServer.read(imgSizeBytes);
        imageSize = ByteBuffer.wrap(imgSizeBytes).asIntBuffer().get(); 
        imageBytes = new byte[imageSize];
        inFromServer.read(imageBytes);
        
        //convert byte array to image and save image
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(imageBytes);
        image = ImageIO.read(byteArrayInputStream);
        ImageIO.write(image, "jpg", new File(path+fileName));

        clientSocket.close();
        }catch(Exception e) {throw new RuntimeException("Error",e);}

    }
    
}