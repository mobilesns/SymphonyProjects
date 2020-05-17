import clients.SymBotClient;
import listeners.RoomListener;
import model.InboundMessage;
import model.OutboundMessage;
import model.Stream;
import model.events.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class RoomListenerImpl implements RoomListener {
    private static final ZoneId HONGKONG_ZONEID = ZoneId.of("Asia/Hong_Kong");
    private static final String[] ROOM_OWNERS = {"pf.vilquin@symphony.com", "william.ko@symphony.com", "chung.yeoh@symphony.com", "Jonathan.Zhou@symphony.com", "hualiang.luo@symphony.com", "ming.kwong@preview1-symphony.com", "Ben.Manwaring@Symphony.com"};
    private static final String MENTION_USERS = " <mention email=\"luodaniel@gmail.com\"/> <mention email=\"hualiang.luo@symphony.com\"/>";

    private String configurableMsg = "";
    private SymBotClient botClient;
    private PeriodicTask pt = new PeriodicTask();

    public RoomListenerImpl(SymBotClient botClient) {
        this.botClient = botClient;
    }

    private boolean isOwner(String mail) {
        for(String owner : ROOM_OWNERS) {
            if (owner.equalsIgnoreCase(mail)) {
                return true;
            }
        }
        return false;
    }

    public void onRoomMessage(InboundMessage msg) {
        String msgText = msg.getMessageText();
        String email = msg.getUser().getEmail();
        System.out.println("command: " + msgText + " email:" + email);
        boolean isOwner = isOwner(email);

        if (isOwner && msgText.startsWith("/test")) {
            ZonedDateTime zdt = ZonedDateTime.now(HONGKONG_ZONEID);
            String tstamp = zdt.toString().substring(0, 19);
            OutboundMessage msgOut = new OutboundMessage("Starting Symphony Ping bot at " + tstamp + ". Will send message every 5 minutes and @Mention the following users on the start of every hour," + MENTION_USERS);
            botClient.getMessagesClient().sendMessage(msg.getStream().getStreamId(), msgOut);
            pt.execute(botClient, msg, configurableMsg);
        } else if (isOwner && msgText.startsWith("/msg")) {
            configurableMsg = msgText.substring(4);
            System.out.println("message: " + configurableMsg);
        } else if (isOwner && msgText.startsWith("/stop")) {
            pt.stop();
        }
    }

    public void onUserJoinedRoom(UserJoinedRoom userJoinedRoom) {
        OutboundMessage msgOut = new OutboundMessage("Welcome " + userJoinedRoom.getAffectedUser().getFirstName() + "!");
        botClient.getMessagesClient().sendMessage(userJoinedRoom.getStream().getStreamId(), msgOut);
    }

    public void onRoomCreated(RoomCreated roomCreated) {}

    public void onRoomDeactivated(RoomDeactivated roomDeactivated) {}

    public void onRoomMemberDemotedFromOwner(RoomMemberDemotedFromOwner roomMemberDemotedFromOwner) {}

    public void onRoomMemberPromotedToOwner(RoomMemberPromotedToOwner roomMemberPromotedToOwner) {}

    public void onRoomReactivated(Stream stream) {}

    public void onRoomUpdated(RoomUpdated roomUpdated) {}

    public void onUserLeftRoom(UserLeftRoom userLeftRoom) {}
}
