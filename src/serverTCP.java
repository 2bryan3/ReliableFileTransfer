import java.io.*;
import java.net.*;

public class serverTCP {
    public static void main(String[] args) throws IOException {
        int port = 0;

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

        DataInputStream DataInput = new DataInputStream(socket.getInputStream());
        DataOutputStream DataOutput = new DataOutputStream(socket.getOutputStream());

        while(true) {
            String input = DataInput.readUTF();
            String[] tokens = input.split(" ", 2);
            String command = tokens[0];

            if ((command.equals("put")) || (command.equals("Put"))) {
                String fileName = new File(tokens[1]).getName();
                File directory = new File("uploads/" + socket.getInetAddress().getHostAddress());
                directory.mkdirs();
                File file = new File(directory, fileName);

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
                DataOutput.writeUTF("File successfully uploaded");
                DataOutput.flush();

            } else if ((command.equals("get")) || (command.equals("Get"))) {
                File file = new File(tokens[1]);
                if(!file.exists()) {
                    DataOutput.writeUTF("File does not exist");
                    DataOutput.flush();
                    continue;
                }

                long fileSize = file.length();
                DataOutput.writeLong(fileSize);
                DataOutput.flush();

                FileInputStream fileInput = new FileInputStream(file);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = fileInput.read(buffer)) != -1) {
                    DataOutput.write(buffer, 0, read);
                }
                DataOutput.flush();
                fileInput.close();

                DataOutput.writeUTF("File delivered from server");
                DataOutput.flush();

            } else if ((command.equals("quit")) || (command.equals("Quit"))) {
                System.out.println("Client disconnected");
                break;
            } else {
                DataOutput.writeUTF("Invalid command, try again");
                DataOutput.flush();
            }
        }

        socket.close();
        serverSocket.close();
    }
}