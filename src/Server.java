import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    static void main(String[] args){
        int serverPort = 3000;
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        try(ServerSocket listenSocket = new ServerSocket(serverPort)){
            executor.submit(new KeyboardMonitor(listenSocket));

            while(true){
                Socket clientSocket = listenSocket.accept();
                executor.submit(new ClientHandler(clientSocket));
            }
        } catch(SocketException e){
            System.out.println("Exiting...");
            executor.shutdown();
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    private static class KeyboardMonitor implements Runnable{
        private final ServerSocket listenSocket;

        public KeyboardMonitor(ServerSocket listenSocket){
            this.listenSocket = listenSocket;
        }

        @Override
        public void run(){
            Scanner keyboard = new Scanner(System.in);
            try{
                while(true){
                    if (keyboard.nextLine().trim().equalsIgnoreCase("q")){
                        this.listenSocket.close();
                        keyboard.close();
                        return;
                    }
                }
            } catch(IOException e){
                e.printStackTrace();
            }
        }
    }
        
    
    private static class ClientHandler implements Runnable{
        private final Socket clientSocket;

        public ClientHandler(Socket clientSocket){
            this.clientSocket = clientSocket;
        }

        @Override
        public void run(){
            System.out.println("Client connected: "+clientSocket.getInetAddress());
            try{
                //define streams
                InputStream in = clientSocket.getInputStream();
                OutputStream out = clientSocket.getOutputStream();
                //define folder info
                File folder = new File("./src/serverFiles");

                //read command from user
                char command = (char) in.read();
                switch(command){
                    case 'L':
                        //get a list of all the files
                        sendFileList(folder, out);
                        break;
                    case 'R':
                        //rename a file
                        renameFile(folder, in, out);
                        break;
                    case 'W':
                        //send a file to be downloaded
                        sendDownloadFile(folder, in, out);
                        break;
                    case 'U':
                        //receive a file to be uploaded
                        getUploadFile(folder, in, out);
                        break;
                    case 'D':
                        //delete a file
                        deleteFile(folder, in, out);
                        break;
                    default:
                        //return failed and explanation
                        System.out.println("Invalid command: "+command);
                        out.write("F".getBytes());
                        break;
                }
            } catch(IOException e){
                e.printStackTrace();
            }
        }

        private void sendFileList(File folder, OutputStream out) throws IOException {
            String stringFiles = "";
            for(File file: folder.listFiles()){
                stringFiles += file.getName()+"\n";
            }
            out.write(stringFiles.getBytes());
            out.flush();
            clientSocket.close();
        }

        private void renameFile(File folder, InputStream in, OutputStream out) throws IOException{
            byte[] buffer = new byte[1024];
            int bytesRead;
            String input = "";
            while((bytesRead = in.read(buffer))!=-1){
                input += new String(buffer, 0, bytesRead);
            }
            String[] fileNames = input.split("\\?",2);
            File toRename = new File(folder, fileNames[0]);
            File renameTo = new File(folder, fileNames[1]);
            if(toRename.exists() && !renameTo.exists()){
                Boolean flag = toRename.renameTo(renameTo);
                if(flag==true){
                    out.write("S".getBytes());
                }else{
                    out.write("F".getBytes());
                }
            }else{
                out.write("F".getBytes());
            }
        }

        private void sendDownloadFile(File folder, InputStream in, OutputStream out) throws IOException{
            byte[] buffer = new byte[1024];
            int bytesRead;
            String fileName = "";
            while((bytesRead = in.read(buffer)) != -1){
                fileName += new String(buffer, 0, bytesRead);
            }
            File fileToSend = new File(folder, fileName);

            if(fileToSend.exists() && !fileToSend.isDirectory()){
                out.write("S".getBytes());
                try(FileInputStream fis = new FileInputStream(fileToSend)){
                    while((bytesRead = fis.read(buffer)) != -1){
                        out.write(buffer, 0, bytesRead);
                    }
                    out.flush();
                    out.close();
                }
            } else{
                out.write("F".getBytes());
            }
        }

        private void getUploadFile(File folder, InputStream in, OutputStream out) throws IOException{
            byte[] buffer = new byte[1024];
            int bytesRead;
            String fileName = "";
            ByteBuffer byteBuffer = ByteBuffer.allocate(4);
            int fileNameLength = byteBuffer.put(in.readNBytes(4)).rewind().getInt();
            bytesRead = in.read(buffer, 0, fileNameLength);
            fileName = new String(buffer, 0, bytesRead);
            // while((bytesRead = in.read(buffer,)) <= fileNameLength){
            //     fileName += new String(buffer, 0, bytesRead);
            // }
            File fileToWrite = new File(folder, fileName);
            if(!fileToWrite.exists() && !fileToWrite.isDirectory()){
                out.write("S".getBytes());
                try(FileOutputStream fos = new FileOutputStream(fileToWrite)){
                    while((bytesRead = in.read(buffer)) != -1){
                        fos.write(buffer, 0, bytesRead);
                    }
                }
            } else{
                System.out.println("Error: File already exists.");
                out.write("F".getBytes());
            }
        }

        private void deleteFile(File folder, InputStream in, OutputStream out) throws IOException{
            byte[] buffer = new byte[1024];
            int bytesRead;
            String fileName = "";
            while((bytesRead = in.read(buffer))!=-1){
                fileName += new String(buffer, 0, bytesRead);
            }
            File toDelete = new File(folder, fileName);
            if(toDelete.exists()){
                Boolean flag = toDelete.delete();
                if(flag==true){
                    out.write("S".getBytes());
                }else{
                    out.write("F".getBytes());
                }
            }else{
                out.write("F".getBytes());
            }
        }
    }
}
