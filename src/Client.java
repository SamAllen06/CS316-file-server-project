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
                    deleteFile(socket, bytes);
                case "R":
                    sendAndReceiveRenamingInformation(socket, bytes);
                case "W":
                  //  downloadFromServer();
                case "U":
                  //  sendUploadInformation();
                case "Q":
                    System.out.println("Exiting server. Goodbye!");
                    scanner.close();
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

    public static void sendAndReceiveRenamingInformation(Socket socket, byte[] menuChoice) throws IOException{
        Scanner renameScanner = new Scanner(System.in);

        System.out.println("Enter original file name: ");
        String originalFileName = renameScanner.nextLine().trim();

        System.out.println("Enter new file name: ");
        String newFileName = renameScanner.nextLine().trim();

        String fileInformation = originalFileName + "?" + newFileName;
        byte[] fileBytes = fileInformation.getBytes();

        renameScanner.close();

        byte[] clientRequest = new byte[menuChoice.length + fileBytes.length];
    
        System.arraycopy(menuChoice, 0, clientRequest, 0, menuChoice.length);
        System.arraycopy(fileBytes, 0, clientRequest, menuChoice.length, fileBytes.length);

        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();

        out.write(clientRequest);
        socket.shutdownOutput();

        int bytesRead;

        byte[] buffer = new byte[1024];
        while((bytesRead = in.read(buffer)) != -1){
            String reply = new String(buffer, 0, bytesRead);
            if(reply.equals("S")){
                System.out.println("Operation successful!");
            }
            else if(reply.equals("F")){
                System.out.println("Operation failed.");
            }
            else{
                System.out.println("Unknown error occurred.");
            }
        }
    }

    public static void deleteFile(Socket socket, byte[] menuChoice) throws IOException{
        Scanner deleteScanner = new Scanner(System.in);
        String fileToDelete = deleteScanner.nextLine().trim();

        byte[] fileName = fileToDelete.getBytes();
        deleteScanner.close();

        byte[] clientRequest = new byte[menuChoice.length + fileName.length];
        System.arraycopy(menuChoice, 0, clientRequest, 0, menuChoice.length);
        System.arraycopy(fileName, 0, clientRequest, menuChoice.length, fileName.length);

        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();

        out.write(clientRequest);
        socket.shutdownOutput();

        int bytesRead;
        byte[] buffer = new byte[1024];
        while((bytesRead = in.read(buffer)) != -1){
            String reply = new String(buffer, 0, bytesRead);
            if(reply.equals("S")){
                System.out.println("Operation successful!");
            }
            else if(reply.equals("F")){
                System.out.println("Operation failed.");
            }
            else{
                System.out.println("Unknown error occurred.");
            }
        }
    }

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
