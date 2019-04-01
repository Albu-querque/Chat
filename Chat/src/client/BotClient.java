package client;

import com.ConsoleHelper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class BotClient extends Client {

    public static void main(String[] args) {
        BotClient botClient = new BotClient();
        botClient.run();
    }

    @Override
    protected SocketThread getSocketThread() {
        return new BotSocketThread();
    }

    @Override
    protected boolean shouldSendTextFromConsole() {
        return false;
    }

    @Override
    protected String getUserName() {
        return "date_bot_" + (int) (Math.random() * 100);
    }

    public class BotSocketThread extends SocketThread {

        @Override
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            sendTextMessage("Привет чатику. Я бот. Понимаю команды: дата, день, месяц, год, время, час, минуты, секунды.");
            super.clientMainLoop();
        }

        @Override
        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
                if (message.contains(": ")) {
                    String[] strings = message.split(": ");
                    Calendar time = Calendar.getInstance();
                    SimpleDateFormat sdf = null;
                    if (strings[1].equals("дата")) sdf = new SimpleDateFormat("d.MM.YYYY");
                    else if (strings[1].equals("день")) sdf = new SimpleDateFormat("d");
                    else if (strings[1].equals("месяц")) sdf = new SimpleDateFormat("MMMM");
                    else if (strings[1].equals("год")) sdf = new SimpleDateFormat("YYYY");
                    else if (strings[1].equals("время")) sdf = new SimpleDateFormat("H:mm:ss");
                    else if (strings[1].equals("час")) sdf = new SimpleDateFormat("H");
                    else if (strings[1].equals("минуты")) sdf = new SimpleDateFormat("m");
                    else if (strings[1].equals("секунды")) sdf = new SimpleDateFormat("s");
                    else return;
                    sendTextMessage("Информация для " + strings[0] + ": " + sdf.format(time.getTime()));
                }

        }
    }
}
