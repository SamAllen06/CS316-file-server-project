import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class Client {
    public static void main(String[] args) throws IOException{
        if(args.length != 2){
            System.out.println("Need two arguments: serverIP & serverPort");
            return;
        } //End if

        //MultiThreading 
        ExecutorService es = Executors.newFixedThreadPool(10);
        

        String serverIP = args[0];
        int serverPort = Integer.parseInt(args[1]);

        boolean keepGoing = true;
        Scanner scanner = new Scanner(System.in);
        while(keepGoing){
            //Menu block
            printMenuBlock();
            System.out.println("Type a command: ");

            String userResponse = scanner.nextLine().trim().toUpperCase();
            byte[] bytes = userResponse.getBytes();
            switch(userResponse){
                case "L":
                    displayList(serverIP, serverPort, bytes);
                    break;
                case "D":
                    deleteFile(scanner, serverIP, serverPort, bytes);
                    break;
                case "R":
                    sendAndReceiveRenamingInformation(scanner, serverIP, serverPort, bytes);
                    break;
                case "W":
                    String fileName = downloadFromServer(scanner);
                    es.submit(new DownloadTask(serverIP, serverPort, fileName));
                    break;
                case "U":
                    String uploadFile = uploadFile(scanner);
                    es.submit(new UploadTask(serverIP, serverPort, uploadFile));
                    break;
                case "Q":
                    System.out.println("Exiting server. Goodbye!");
                    scanner.close();
                    keepGoing = false;
                    es.shutdown();
                    break;
            } //End switch

        } //End while
        
    } //End main

    public static void displayList(String serverIP, int serverPort, byte[] bytes) throws IOException{
        try (Socket socket = new Socket(serverIP, serverPort)) {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            out.write(bytes); //Send data to server
            socket.shutdownOutput();

            String reply = "";
            int bytesRead;
            byte[] buffer = new byte[1024];
            while((bytesRead = in.read(buffer)) != -1){
                reply = new String(buffer, 0, bytesRead);
                System.out.println(reply);
            } //End while
            return;
        } //End try
        catch(SocketException e){
            e.printStackTrace();
        } //End catch
    } //End displayList

    public static void sendAndReceiveRenamingInformation(Scanner scanner, String serverIP, int serverPort, byte[] menuChoice) throws IOException{
        try (Socket socket = new Socket(serverIP, serverPort)) {

            System.out.println("Enter original file name: ");
            String originalFileName = scanner.nextLine().trim();

            System.out.println("Enter new file name: ");
            String newFileName = scanner.nextLine().trim();

            String fileInformation = originalFileName + "?" + newFileName;
            byte[] fileBytes = fileInformation.getBytes();

            byte[] clientRequest = createClientRequest(menuChoice, fileBytes);

            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            out.write(clientRequest);
            socket.shutdownOutput();

            int response = in.read();

            if (response == 'S') {
                System.out.println("Operation successful!");
            } else if (response == 'F') {
                System.out.println("Operation failed.");
            } else {
                System.out.println("Unknown error occurred.");
            }//End if block
        } //End try
        catch(SocketException e){
            e.printStackTrace();
        } //End catch
    }//End sendAndReceiveNaming

    public static void deleteFile(Scanner scanner, String serverIP, int serverPort, byte[] menuChoice) throws IOException{
        try (Socket socket = new Socket(serverIP, serverPort)) {
            System.out.println("Enter filename to delete: ");
            String fileToDelete = scanner.nextLine().trim();

            byte[] fileName = fileToDelete.getBytes();

            byte[] clientRequest = createClientRequest(menuChoice, fileName);

            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            out.write(clientRequest);
            socket.shutdownOutput();

            int response = in.read();

            if (response == 'S') {
                System.out.println("Operation successful!");
            } else if (response == 'F') {
                System.out.println("Operation failed.");
            } else {
                System.out.println("Unknown error occurred.");
            } //End if
        } catch(SocketException e){
            e.printStackTrace();
        } //End catch
    }// end deleteFile

    public static String downloadFromServer(Scanner scanner){
        System.out.println("Enter filename to download: ");
        String fileToDownload = scanner.nextLine().trim();
        return fileToDownload;
    } //End download

    public static String uploadFile(Scanner scanner){
        System.out.println("Enter filename to upload: ");
        String fileToUpload = scanner.nextLine().trim();
        return fileToUpload;
    } //End upload

    private static byte[] createClientRequest(byte[] menuChoice, byte[] fileBytes){
        byte[] clientRequest = new byte[menuChoice.length + fileBytes.length];

        System.arraycopy(menuChoice, 0, clientRequest, 0, menuChoice.length);
        System.arraycopy(fileBytes, 0, clientRequest, menuChoice.length, fileBytes.length);

        return clientRequest;
    } //End clientRequest

    private static void printMenuBlock(){
        System.out.println("---------------------");
        System.out.println("L to display the names of all files on server");
        System.out.println("D to delete a file from server");
        System.out.println("R to rename a file");
        System.out.println("W to download a file");
        System.out.println("U to upload a file");
        System.out.println("Q to quit");
        System.out.println("---------------------");
    } //End menublock

    private static class DownloadTask implements Runnable{
        final String serverIP;
        final int serverPort;
        final String fileName;

        public DownloadTask(String serverIP, int serverPort, String fileName) {
            this.serverIP = serverIP;
            this.serverPort = serverPort;
            this.fileName = fileName;
        }

        @Override
        public void run() {
            File downloadFile = new File("./src/ClientFiles", fileName);

            try (Socket socket = new Socket(serverIP, serverPort)) {

                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();
                byte[] fileBytes = fileName.getBytes();
                
             // Construct request
                out.write('W');
                //out.write(length);
                out.write(fileBytes);
                out.flush();
                socket.shutdownOutput();

                int status = in.read();

                if (status == 'F') {
                    System.out.println("File not found on server: " + fileName);
                    return;
                }

                if (status != 'S') {
                    System.out.println("Invalid server response.");
                    return;
                }

                downloadFile.getParentFile().mkdirs();

                try (OutputStream fileOut = new java.io.FileOutputStream(downloadFile)) {

                    byte[] buffer = new byte[1024];
                    int bytesRead;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        fileOut.write(buffer, 0, bytesRead);
                    }
                    System.out.println("Download of " + fileName + " has completed.");
                } catch (IOException e) {
                    System.out.println("Download failed: " + e.getMessage());
                }  
    } catch (IOException e) {
        e.printStackTrace();
    }
}
    }
    public static class UploadTask implements Runnable {
        private String serverIP;
        private int serverPort;
        private String fileName;

        public UploadTask(String serverIP, int serverPort, String fileName) {
            this.serverIP = serverIP;
            this.serverPort = serverPort;
            this.fileName = fileName;
        }

        @Override
        public void run() {
            File file = new File("./src/ClientFiles/" + fileName);

            // Check whether the file exists
            if (!file.exists() || !file.isFile()) {
                System.out.println("File not found: " + fileName);
                return;
            }

            try (Socket socket = new Socket(serverIP, serverPort);
             FileInputStream fileIn = new FileInputStream(file);
             OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream()){

                // Send the upload command
                out.write('U');

                // Send the filename length
                byte[] fileNameBytes = fileName.getBytes();
                ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
                lengthBuffer.putInt(fileNameBytes.length);
                out.write(lengthBuffer.array());

                // Send the filename
                out.write(fileNameBytes);

                // Send the file contents
                byte[] buffer = new byte[1024];
                int bytesRead;

                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }

                out.flush();

                // Indicate that the client has finished sending data
                socket.shutdownOutput();

                // Receive the server's response
                int response = in.read();

                if (response == 'S') {
                    System.out.println("File uploaded successfully: " + fileName);
                } else if (response == 'F') {
                    System.out.println("File upload failed: " + fileName);
                } else {
                    System.out.println("Unexpected server response.");
                }

            } catch (IOException e) {
                System.out.println("Error uploading file: " + e.getMessage());
            }
    }
}
}
