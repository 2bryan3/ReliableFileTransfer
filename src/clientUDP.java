import java.io.*;
import java.net.*;

public class clientUDP {
    public static void main(String[] args) throws IOException {
        String inputAddress = "";
        int port = 0;

        if (args.length == 2) {
            inputAddress = args[0];
            port = Integer.parseInt(args[1]);
        } else {
            System.out.println("Please enter both IP and port");
            return;
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        DatagramSocket socket = new DatagramSocket();
        InetAddress IP = InetAddress.getByName(inputAddress);


        System.out.println("Connected to server: " + IP.getHostAddress() + ":" + port);

        while (true) {
            System.out.print("Enter command: ");
            String command = in.readLine();

            if ((command.startsWith("put ")) || (command.startsWith("Put "))) {
                File file = new File(command.split(" ", 2)[1]);
                if (!file.exists()) {
                    System.out.println("File does not exist");
                    continue;
                }
                byte[] commandData = command.getBytes();
                DatagramPacket packet = new DatagramPacket(commandData, commandData.length, IP, port);
                socket.send(packet);

                sendFile(file, IP, port, socket, command);

            } else if (command.startsWith("get ") || (command.startsWith("Get "))) {

                byte[] commandData = command.getBytes();
                DatagramPacket packet = new DatagramPacket(commandData, commandData.length, IP, port);
                socket.send(packet);

                String fileName = new File(command.split(" ", 2)[1]).getName();
                File directory = new File("downloads");
                directory.mkdirs();
                File file = new File(directory, fileName);

                receiveFile(file, IP, port, socket, command);

            } else if ((command.equals("quit")) || (command.equals("Quit"))) {
                byte[] commandData = command.getBytes();
                DatagramPacket packet = new DatagramPacket(commandData, commandData.length, IP, port);
                socket.send(packet);
                socket.close();
                break;
            } else {
                System.out.println("Unknown command: " + command + " Try again");
            }
        }
    }

    static void receiveFile(File file, InetAddress senderAddr, int senderPort, DatagramSocket socket, String command) throws IOException {

        byte[] buffer = new byte[1024];
        DatagramPacket receivePkt = new DatagramPacket(buffer, buffer.length);
        socket.receive(receivePkt);

        String message = new String(receivePkt.getData(), 0, receivePkt.getLength());

        if(!message.startsWith("LEN:")) {
            System.out.println(message);
            return;
        }

        long fileSize = Long.parseLong(message.substring(4));

        FileOutputStream fileOutput = new FileOutputStream(file);

        long total = 0;
        byte[] data = new byte[1100];

        socket.setSoTimeout(1000);

        try {
            while (total < fileSize) {
                DatagramPacket dataPacket = new DatagramPacket(data, data.length);
                try {
                    socket.receive(dataPacket);
                } catch (SocketTimeoutException e) {
                    fileOutput.close();
                    if (total == 0) {
                        System.out.println("Did not receive data. Terminating");
                    } else {
                        System.out.println("Data transmission terminated prematurely");
                    }
                    System.exit(1);
                }
                int filePartSize = dataPacket.getLength();
                fileOutput.write(dataPacket.getData(), 0, filePartSize);
                total += filePartSize;

                String ack = "ACK";
                byte[] ackData = ack.getBytes();
                DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, senderAddr, senderPort);
                socket.send(ackPacket);
            }

            fileOutput.close();

            String fin = "FIN";
            byte[] finData = fin.getBytes();
            DatagramPacket finPacket = new DatagramPacket(finData, finData.length, senderAddr, senderPort);
            socket.send(finPacket);

            if (command.startsWith("put ") || (command.startsWith("Put "))) {
                System.out.println("File successfully uploaded");
            } else if (command.startsWith("get ") || (command.startsWith("Get "))) {
                System.out.println("File delivered from server");
            }
        } finally {
            socket.setSoTimeout(0);
        }
    }

    static void sendFile(File file, InetAddress serverAddr, int serverPort, DatagramSocket socket, String command) throws IOException {
        long fileSize = file.length();
        String message = "LEN:" + fileSize;
        byte[] data = message.getBytes();
        DatagramPacket lenPacket = new DatagramPacket(data, data.length, serverAddr, serverPort);
        socket.send(lenPacket);

        FileInputStream fileInput = new FileInputStream(file);

        byte[] buffer = new byte[1000];
        int read;

        socket.setSoTimeout(1000);

        try {
            while ((read = fileInput.read(buffer)) != -1) {
                DatagramPacket dataPacket = new DatagramPacket(buffer, read, serverAddr, serverPort);
                socket.send(dataPacket);

                byte[] ackBuffer = new byte[1024];
                DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
                try {
                    socket.receive(ackPacket);
                } catch (SocketTimeoutException e) {
                    fileInput.close();
                    System.out.println("Did not receive ACK. Terminating");
                    System.exit(1);
                }

                String ack = new String(ackPacket.getData(), 0, ackPacket.getLength());
                if (!ack.equals("ACK")) {
                    System.out.println("Did not receive ACK");
                    fileInput.close();
                    return;
                }
            }

            fileInput.close();

            byte[] finBuffer = new byte[1024];
            DatagramPacket finPacket = new DatagramPacket(finBuffer, finBuffer.length);
            socket.receive(finPacket);

            String fin = new String(finPacket.getData(), 0, finPacket.getLength());
            if (fin.equals("FIN")) {
                if (command.startsWith("put ") || (command.startsWith("Put "))) {
                    System.out.println("File successfully uploaded");
                } else if (command.startsWith("get ") || (command.startsWith("Get "))) {
                    System.out.println("File delivered from server");
                }
            }
        } finally {
            socket.setSoTimeout(0);
        }

    }
}