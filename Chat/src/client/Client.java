package client;

import com.Connection;
import com.ConsoleHelper;
import com.Message;
import com.MessageType;

import java.io.IOException;
import java.net.Socket;

public class Client {
    protected Connection connection;
    private volatile boolean clientConnected = false;

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }

    protected String getServerAddress() {
        ConsoleHelper.writeMessage("Введите адрес машины к которой хотите подключиться( ip, если клиент и сервер запущен на разных машинах или 'localhost',\n если клиент и сервер работают на одной машине)");
        return ConsoleHelper.readString();
    }

    protected int getServerPort() {
        ConsoleHelper.writeMessage("Введите порт сервера: ");
        return ConsoleHelper.readInt();
    }

    protected String getUserName() {
        ConsoleHelper.writeMessage("Введите имя пользователя: ");
        return ConsoleHelper.readString();
    }

    protected boolean shouldSendTextFromConsole() { return true; }

    protected SocketThread getSocketThread() {
        return new SocketThread();
    }

    protected void sendTextMessage(String text) {
        try {
            connection.send(new Message(MessageType.TEXT, text));
        }catch(IOException ex) {
            ConsoleHelper.writeMessage("Ошибка во время отправки.");
            clientConnected = false;
        }
    }

    public void run() {
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();
        synchronized (this) {
            try {
                wait();
            }
            catch (InterruptedException ex) {
                ConsoleHelper.writeMessage("Возникла ошибка при получении сообщения.");
                return;
            }
        }

        if (clientConnected) ConsoleHelper.writeMessage("Соединение установлено. Для выхода наберите команду 'exit'.");
        else ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");

        String text;
        while(clientConnected) {
            text = ConsoleHelper.readString();
            if(text.equals("exit"))
                clientConnected = false;
            else if (shouldSendTextFromConsole())
                sendTextMessage(text);
        }
    }

    public class SocketThread extends Thread {

        @Override
        public void run() {
            String address = getServerAddress();
            int port = getServerPort();
            try {
                connection = new Connection(new Socket(address, port));
                clientHandshake();
                clientMainLoop();
            }catch(IOException | ClassNotFoundException ex) {
                notifyConnectionStatusChanged(false);
            }
        }

        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName) {
            ConsoleHelper.writeMessage("Присоединился к чату - " + userName);
        }

        protected void informAboutDeletingNewUser(String userName) {
            ConsoleHelper.writeMessage("Покинул чат - " + userName);
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected) {
            synchronized (Client.this) {
            Client.this.clientConnected = clientConnected;
            Client.this.notify();
            }
        }

        protected  void clientHandshake() throws IOException, ClassNotFoundException {
            String name;
            while(true) {
                Message message = connection.receive();
                Message sending;
                if(message.getType() == MessageType.NAME_REQUEST) {
                    name = getUserName();
                    connection.send(new Message(MessageType.USER_NAME, name));
                } else if(message.getType() == MessageType.NAME_ACCEPTED) {
                    notifyConnectionStatusChanged(true);
                    return;
                } else {
                    throw new IOException("Unexpected MessageType");
                }
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            while(true) {
                Message message = connection.receive();
                if(message.getType() == MessageType.TEXT)
                    processIncomingMessage(message.getData());
                else if(message.getType() == MessageType.USER_ADDED)
                    informAboutAddingNewUser(message.getData());
                else if(message.getType() == MessageType.USER_REMOVED)
                    informAboutDeletingNewUser(message.getData());
                else
                    throw new IOException("Unexpected MessageType");
            }
        }
    }
}
