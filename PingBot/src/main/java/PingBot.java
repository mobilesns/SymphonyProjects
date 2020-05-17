import clients.SymBotClient;
import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingBot {
    private static final Logger log = LoggerFactory.getLogger(PingBot.class);

    public static void main(String[] args) {
        BasicConfigurator.configure();

        try {
            SymBotClient botClient = SymBotClient.initBotRsa("config.json");

            botClient.getDatafeedEventsService().addListeners(
                new IMListenerImpl(botClient),
                new RoomListenerImpl(botClient)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}