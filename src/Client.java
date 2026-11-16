import java.util.Scanner;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Client {
    public static void main(String[] args) throws IOException{
        if(args.length != 2){
            System.out.println("Need two arguments: serverIP & serverPort");
            return;
        }

        String serverIP = args[0];
        int serverPort = Integer.parseInt(args[1]);

        Socket socket = new Socket(serverIP, serverPort);

        boolean keepGoing = true;
        while(keepGoing){
            Scanner scanner = new Scanner(System.in);

            //Menu block
            printMenuBlock();
            System.out.println("Type a command: ");

            String userResponse = scanner.nextLine().trim().toUpperCase();
            byte[] bytes = userResponse.getBytes();
            switch(userResponse){
                case "L":
                    displayList(socket, bytes);
                case "D":
                    //deleteFile(socket, bytes);
                case "R":
                  //  sendRenamingInformation();
                case "W":
                  //  downloadFromServer();
                case "U":
                  //  sendUploadInformation();
                case "Q":
                    System.out.println("Exiting server. Goodbye!");
                    keepGoing = false;
            } //End switch

        } //End while
        
    } //End main

    public static void displayList(Socket socket, byte[] bytes) throws IOException{
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();
        out.write(bytes); //Send data to server
        socket.shutdownOutput();

        int bytesRead;

        byte[] buffer = new byte[1024];
        while((bytesRead = in.read(buffer)) != -1){
            String reply = new String(buffer, 0, bytesRead);
            System.out.println(reply);
        }
    }
/* 
    public static void deleteFile(Socket socket, byte[] bytes){

    }
    */

    private static void printMenuBlock(){
        System.out.println("---------------------");
        System.out.println("L to display the names of all files on server");
        System.out.println("D to delete a file from server");
        System.out.println("R to rename a file");
        System.out.println("W to download a file");
        System.out.println("U to upload a file");
        System.out.println("Q to quit");
        System.out.println("---------------------");
    }
}
