import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;

public class serverUDP {
    public static void main(String[] args) throws IOException {

        int port = 0;

        if (args.length == 1) {
            port = Integer.parseInt(args[0]);
        } else {
            System.out.println("Please enter port number");
            return;
        }

        DatagramSocket socket = new DatagramSocket(port);
        System.out.println("Listening on port " + port);


        while (true) {
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);

            String input = new String(receivePacket.getData(), 0, receivePacket.getLength());
            String[] tokens = input.split(" ", 2);
            String command = tokens[0];

            InetAddress clientAddr = receivePacket.getAddress();
            int clientPort = receivePacket.getPort();

            if ((command.equals("put")) || (command.equals("Put"))) {

                String fileName = new File(tokens[1]).getName();
                File directory = new File("uploads/" + clientAddr.getHostAddress());
                directory.mkdirs();
                File file = new File(directory, fileName);

                receiveFile(file, clientAddr, clientPort, socket);

            } else if ((command.equals("get")) || (command.equals("Get"))) {
                File file = new File(tokens[1]);
                if (!file.exists()) {
                    String error = "File does not exist";
                    byte[] errorMessage = error.getBytes();
                    DatagramPacket errorPacket = new DatagramPacket(errorMessage, errorMessage.length, clientAddr, clientPort);
                    socket.send(errorPacket);
                    continue;
                }

                sendFile(file, clientAddr, clientPort, socket);

            } else if ((command.equals("quit")) || (command.equals("Quit"))) {
                System.out.println("Client disconnected");
                break;
            } else {
                System.out.println("Unknown command: " + command);
            }
        }
    }

    static void receiveFile(File file, InetAddress clientAddr, int clientPort, DatagramSocket socket) throws IOException {

        byte[] buffer = new byte[1024];
        DatagramPacket receivePkt = new DatagramPacket(buffer, buffer.length);
        socket.receive(receivePkt);

        String message = new String(receivePkt.getData(), 0, receivePkt.getLength());

        if (!message.startsWith("LEN:")) {
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
                DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, clientAddr, clientPort);
                socket.send(ackPacket);
            }

            fileOutput.close();

            String fin = "FIN";
            byte[] finData = fin.getBytes();
            DatagramPacket finPacket = new DatagramPacket(finData, finData.length, clientAddr, clientPort);
            socket.send(finPacket);

//            System.out.println("File successfully uploaded");
        } finally {
            socket.setSoTimeout(0);
        }
    }

    static void sendFile(File file, InetAddress clientAddr, int clientPort, DatagramSocket socket) throws IOException {
        long fileSize = file.length();
        String message = "LEN:" + fileSize;
        byte[] data = message.getBytes();
        DatagramPacket lenPacket = new DatagramPacket(data, data.length, clientAddr, clientPort);
        socket.send(lenPacket);

        FileInputStream fileInput = new FileInputStream(file);

        byte[] buffer = new byte[1000];
        int read;

        socket.setSoTimeout(1000);

        try {
            while ((read = fileInput.read(buffer)) != -1) {
                DatagramPacket dataPacket = new DatagramPacket(buffer, read, clientAddr, clientPort);
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
//            if (fin.equals("FIN")) {
//                System.out.println("File successfully uploaded");
//            }
        } finally {
            socket.setSoTimeout(0);
        }
    }
}