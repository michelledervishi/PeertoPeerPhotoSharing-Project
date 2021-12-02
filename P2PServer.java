import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import javax.imageio.*;
import java.io.FileNotFoundException;
import java.util.Date;

public class P2PServer implements Runnable 
{
    private int portNum; 
    private boolean stopped = false;
    private Thread threadRunning = null;
    private ServerSocket welcomeSocket = null;


    public P2PServer(int portNum) {
        this.portNum = portNum;
    }

    public void run() {
        synchronized(this) {
            this.threadRunning = Thread.currentThread();
        }
        try {
            welcomeSocket = new ServerSocket(this.portNum); 
        } catch(IOException e) {
            throw new RuntimeException("Cannot open port "+this.portNum,e);
        }
        
        while (!this.stopped) {
            Socket connectionSocket = null;
            //wait, on welcoming socket for contact by client
            try {
                connectionSocket = welcomeSocket.accept(); 
            } catch(IOException e) {
                throw new RuntimeException("Error accepting client connection",e);
            }
            //create new thread to handle file transfer
            new Thread(new fileTransferThread(connectionSocket)).start();
        }
    }
    public synchronized void stop(){
        this.stopped = true;
        try {
            this.welcomeSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Error closing server", e);
        }
    }
}

class fileTransferThread implements Runnable {
    private Socket connectionSocket = null;
    private String path = "H:\\Documents\\706Proj\\imgsrc\\";
    private Date date = new Date();
    private BufferedImage image;
    private String httpVer = "1.1";

    public fileTransferThread(Socket connectionSocket) {
        this.connectionSocket = connectionSocket;
    }

    public void run() {
        if (httpVer.equals("1.1")) {
            try {
                //create input stream, attached to socket
                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(this.connectionSocket.getInputStream()));
                    
                //create output stream, attached to socket
                DataOutputStream outToClient = new DataOutputStream(this.connectionSocket.getOutputStream());

                //read in name of file from client
                String fileName = inFromClient.readLine();

                //read the file given the file name
                try {
                    image = ImageIO.read(new File(path+fileName));
                } catch(IOException e) {
                    System.out.println("HTTP/1.1 404 Not Found");
                    System.out.println("Connection: TCP");
                    System.out.println(date);
                    throw new RuntimeException(e);
                } 
                //convert the image to a byte array
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(); 
                ImageIO.write(image, "jpg", byteArrayOutputStream);
                byte[] imgSize = ByteBuffer.allocate(4).putInt(byteArrayOutputStream.size()).array();
                byte[] imgBytes = byteArrayOutputStream.toByteArray();
                
                //write out image and image size as byte arrays to client
                outToClient.write(imgSize);
                outToClient.write(imgBytes);
                outToClient.flush();
                System.out.println("HTTP/1.1 200 OK");
                System.out.println("Connection: TCP");
                System.out.println(date);

            } catch(IOException e) {
                System.out.println("HTTP/1.1 400 Bad Request");
                System.out.println("Connection: TCP");
                System.out.println(date);
                throw new RuntimeException(e);
            } 
        }
        else {
            System.out.println("HTTP/1.1 505 HTTP Version Not Supported");
                System.out.println("Connection: TCP");
                System.out.println(date);
        }
    }
} 