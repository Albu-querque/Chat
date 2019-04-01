package com;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        ConsoleHelper.writeMessage("Введите порт сервера: ");
        try (ServerSocket server = new ServerSocket(ConsoleHelper.readInt())){
            ConsoleHelper.writeMessage("Server alive!");
            while(true) new Handler(server.accept()).start();
        }catch (IOException e) { ConsoleHelper.writeMessage(e.getMessage()); }
        
    }

    public static void sendBroadcastMessage(Message message) {
        try {
            for (Map.Entry<String, Connection> entry : connectionMap.entrySet()) {
                if (entry.getValue() != null)
                    entry.getValue().send(message);
            }
        } catch(IOException ex) {
            System.out.println("Сервер не смог отправить сообщение.");
        }
    }

    private static class Handler extends Thread {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            ConsoleHelper.writeMessage("Соединение установлено: " + socket.getRemoteSocketAddress().toString());
            Connection connection = null;
            String userName = null;
            try {
                connection = new Connection(socket);
                userName = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                sendListOfUsers(connection, userName);
                serverMainLoop(connection, userName);
            }catch(IOException | ClassNotFoundException ex) {
                ConsoleHelper.writeMessage("Ошибка при обмене данными с удаленным пользователем.");
                try {
                    if(connection != null) connection.close();
                } catch (IOException e) {}
            } finally {
                if(userName != null) {
                    connectionMap.remove(userName);
                    sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
                    ConsoleHelper.writeMessage("Соединение с удаленным адресом закрыто.");
                }
            }
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            while (true) {
                connection.send(new Message(MessageType.NAME_REQUEST));
                Message message = connection.receive();
                if(message.getType() == MessageType.USER_NAME) {
                    String name = message.getData();
                    if (!name.isEmpty() && !connectionMap.containsKey(name)) {
                        connectionMap.put(name, connection);
                        connection.send(new Message(MessageType.NAME_ACCEPTED));
                        return name;
                    }
                }
            }
        }

        private void sendListOfUsers(Connection connection, String userName) throws IOException{
            for (Map.Entry<String, Connection> m : connectionMap.entrySet()){
                if (!m.getKey().equals(userName)){
                    connection.send(new Message(MessageType.USER_ADDED, m.getKey()));
                }
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            while(true) {
                Message message = connection.receive();
                if(message.getData() != null && message.getType().equals(MessageType.TEXT)) {
                    String result = userName + ": " + message.getData();
                    sendBroadcastMessage(new Message(MessageType.TEXT, result));
                } else {
                    ConsoleHelper.writeMessage("Ошибка!");
                }
            }
        }

    }
}