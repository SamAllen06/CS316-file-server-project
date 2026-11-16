import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    static void main(String[] args){
        int serverPort = 3000;
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        try(ServerSocket listenSocket = new ServerSocket(serverPort)){
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
                // Path folder = FileSystems.getDefault().getPath("serverFiles");
                File folder = new File("./src/serverFiles");
                File[] files = folder.listFiles();
                //define how many bytes can be read at a time
                byte[] buffer = new byte[1024];
                int bytesRead;

                //read command from user
                char command = (char) in.read();

                switch(command){
                    case 'L':
                        //get a list of all the files
                        String stringFiles = "";
                        for(File file: files){
                            stringFiles += file.getName()+"\n";
                        }
                        out.write(stringFiles.getBytes());
                        clientSocket.close();
                    case 'R':
                        //rename a file
                    case 'W':
                        //download a file
                    case 'U':
                        //upload a file
                    case 'D':
                        //delete a file
                    default:
                        System.out.println("Invalid command: "+command);
                        //return failed and explanation
                }


                // while((bytesRead = in.read(buffer)) != -1){

                // }
            } catch(IOException e){
                e.printStackTrace();
            }

            // File fileToSend = new File(folder, fileName);

            // if(fileToSend.exists() && !fileToSend.isDirectory()){
            //     out.write('S');
            //     try(FileInputStream fis = new FileInputStream(fileToSend)){
            //         while((bytesRead = fis.read(buffer)) != -1){
            //             out.write(buffer, o, bytesRead);
            //         }
            //     }
            // } else{
            //     System.out.println("Error: File not found on server.");
            //     out.write('F');
            // }
        }
    }
}
