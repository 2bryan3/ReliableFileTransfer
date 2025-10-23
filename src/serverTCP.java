import java.io.*;
import java.net.*;

public class serverTCP {
    public static void main(String[] args) throws IOException {
        int port = 0;
        // takes in the arguments from the user
        if (args.length == 1) {
            port = Integer.parseInt(args[0]);
        } else {
            System.out.println("Please enter port number");
            return;
        }
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Listening on port " + port);

        Socket socket = serverSocket.accept();
        System.out.println("Accepted connection from " + socket.getInetAddress().getHostAddress());

        DataInputStream dataInput = new DataInputStream(socket.getInputStream());
        DataOutputStream dataOutput = new DataOutputStream(socket.getOutputStream());
        //handles the client's commands
        while(true) {
            String input = dataInput.readUTF();
            String[] tokens = input.split(" ", 2);
            String command = tokens[0];

            if ((command.equals("put")) || (command.equals("Put"))) {
                String fileName = new File(tokens[1]).getName();
                File directory = new File("uploads/" + socket.getInetAddress().getHostAddress());
                directory.mkdirs();
                File file = new File(directory, fileName);

                long fileSize = dataInput.readLong();

                FileOutputStream fileOutput = new FileOutputStream(file);
                byte[] buffer = new byte[1024];
                long remaining = fileSize;

                while (remaining > 0) {
                    int read = dataInput.read(buffer, 0, (int)Math.min(remaining, buffer.length));
                    if (read == -1) {
                        break;
                    }
                    fileOutput.write(buffer, 0, read);
                    remaining -= read;
                }
                fileOutput.close();
                dataOutput.writeUTF("File successfully uploaded");
                dataOutput.flush();

            } else if ((command.equals("get")) || (command.equals("Get"))) {
                File file = new File(tokens[1]);
                if(!file.exists()) {
                    dataOutput.writeUTF("File does not exist");
                    dataOutput.flush();
                    continue;
                }

                long fileSize = file.length();
                dataOutput.writeLong(fileSize);
                dataOutput.flush();

                FileInputStream fileInput = new FileInputStream(file);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = fileInput.read(buffer)) != -1) {
                    dataOutput.write(buffer, 0, read);
                }
                dataOutput.flush();
                fileInput.close();

                dataOutput.writeUTF("File delivered from server");
                dataOutput.flush();

            } else if ((command.equals("quit")) || (command.equals("Quit"))) {
                System.out.println("Client disconnected");
                break;
            } else {
                dataOutput.writeUTF("Invalid command, try again");
                dataOutput.flush();
            }
        }

        socket.close();
        serverSocket.close();
    }
}