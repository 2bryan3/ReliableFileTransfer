import java.io.*;
import java.net.*;

public class server {
    public static void main(String[] args) throws IOException {
        int port = 0;

        if (args.length == 1) {
            port = Integer.parseInt(args[0]);
        } else {
            System.out.println("Please enter port number");
            return;
        }
        String input;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Listening on port " + port);

        Socket socket = serverSocket.accept();
        System.out.println("Accepted connection from " + socket.getInetAddress().getHostAddress());

        DataInputStream DataInput = new DataInputStream(socket.getInputStream());
        DataOutputStream DataOutput = new DataOutputStream(socket.getOutputStream());
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);

        while((input = bufferedReader.readLine()) != null) {
            String[] tokens = input.split(" ", 2);
            String command = tokens[0];

            if ((command.equals("put")) || (command.equals("Put"))) {
                String fileName = new File(tokens[1]).getName();
                File directory = new File("uploads/" + socket.getInetAddress().getHostAddress());
                directory.mkdirs();
                //printWriter.println(directory.getPath());
                File file = new File(directory, fileName);
                //printWriter.println(file.getPath());

                long fileSize = DataInput.readLong();
                FileOutputStream fileOutput = new FileOutputStream(file);
                byte[] buffer = new byte[1024];
                long remaining = fileSize;

                while (remaining > 0) {
                    int read = DataInput.read(buffer, 0, (int)Math.min(remaining, buffer.length));
                    if (read == -1) {
                        break;
                    }
                    fileOutput.write(buffer, 0, read);
                    remaining -= read;
                }
                fileOutput.close();
                printWriter.println("File successfully uploaded");
                //printWriter.println("END");
            } else if ((command.equals("get")) || (command.equals("Get"))) {
                File file = new File(tokens[1]);
                if(!file.exists()) {
                    printWriter.println("File does not exist");
                    continue;
                }

                long fileSize = file.length();
                DataOutput.writeLong(fileSize);
                FileInputStream fileInput = new FileInputStream(file);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = fileInput.read(buffer)) != -1) {
                    DataOutput.write(buffer, 0, read);
                }
                fileInput.close();
                printWriter.println("File delivered from server");
            } else if ((command.equals("quit")) || (command.equals("Quit"))) {
                System.out.println("Client disconnected");
            } else {
                printWriter.println("Invalid command, try again");
            }
        }

        socket.close();
        serverSocket.close();
    }
}
