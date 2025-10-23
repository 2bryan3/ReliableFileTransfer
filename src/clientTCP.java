import java.io.*;
import java.net.*;

public class clientTCP {
    public static void main(String[] args) throws IOException {
        String IP = "";
        int port = 0;
        //takes in IP address and port number from the user
        if (args.length == 2) {
            IP = args[0];
            port = Integer.parseInt(args[1]);
        } else {
            System.out.println("Please enter both IP and port");
            return;
        }

        Socket socket = new Socket(IP, port);
        System.out.println("Connected to server " + IP + ":" + port);

        DataInputStream dataInput = new DataInputStream(socket.getInputStream());
        DataOutputStream dataOutput = new DataOutputStream(socket.getOutputStream());
        BufferedReader inputFromUser = new BufferedReader(new InputStreamReader(System.in));
        //sends the commands
        while (true) {
            System.out.print("Enter command: ");
            String command = inputFromUser.readLine();

            if((command.startsWith("put ")) || (command.startsWith("Put "))){
                File file = new File(command.split(" ",2)[1]);
                if(!file.exists()){
                    System.out.println("File does not exist");
                    continue;
                }

                dataOutput.writeUTF(command);
                dataOutput.writeLong(file.length());
                dataOutput.flush();

                FileInputStream fileInput = new FileInputStream(file);
                byte[] buffer = new byte[1024];
                int read;
                while((read = fileInput.read(buffer)) != -1){
                    dataOutput.write(buffer, 0, read);
                }
                dataOutput.flush();
                fileInput.close();
                System.out.println(dataInput.readUTF());

            } else if(command.startsWith("get ") || (command.startsWith("Get "))){
                dataOutput.writeUTF(command);
                dataOutput.flush();

                String fileName = new File(command.split(" ",2)[1]).getName();
                File directory = new File("downloads");
                directory.mkdirs();
                File file = new File(directory, fileName);

                long fileSize = dataInput.readLong();
                FileOutputStream fileOutput = new FileOutputStream(file);
                byte[] buffer = new byte[1024];
                long remaining = fileSize;

                while(remaining > 0){
                    int read = dataInput.read(buffer, 0, (int)Math.min(remaining, buffer.length));
                    if(read == -1){
                        break;
                    }
                    fileOutput.write(buffer, 0, read);
                    remaining -= read;
                }
                fileOutput.close();
                System.out.println(dataInput.readUTF());

            } else if((command.equals("quit")) || (command.equals("Quit"))){
                dataOutput.writeUTF("quit");
                dataOutput.flush();
                socket.close();
                break;
            } else {
                System.out.println("Unknown command: " + command + " Try again");
            }
        }
    }
}