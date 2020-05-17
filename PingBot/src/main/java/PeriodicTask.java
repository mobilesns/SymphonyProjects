import clients.SymBotClient;
import model.InboundMessage;
import model.OutboundMessage;

import java.security.SecureRandom;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.*;
import static java.util.concurrent.TimeUnit.*;

public class PeriodicTask {
    private static final ZoneId HONGKONG_ZONEID = ZoneId.of("Asia/Hong_Kong");
    private static final String MENTION_USERS = " <mention email=\"luodaniel@gmail.com\"/> <mention email=\"hualiang.luo@symphony.com\"/>";
    private static final String ALPHANUM = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int LEN_ALPHANUM = ALPHANUM.length();
    private static final SecureRandom rnd = new SecureRandom();
    private static final int[] MSGNUM = {1000, 5000, 10000, 20000, 40000};
    private static final int LEN_MSGNUM = MSGNUM.length;
    private volatile int  idx = 0;
    private volatile long sequence = 1;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> futureSendMsg = null;
    private ScheduledFuture<?> futurePingUsers = null;

    private String randomString(int len) {
        StringBuilder sb = new StringBuilder(len);
        for( int i = 0; i < len; i++ )
            sb.append(ALPHANUM.charAt(rnd.nextInt(LEN_ALPHANUM)));
        return sb.toString();
    }

    private Long getDelayUntilNextDay() {
        ZonedDateTime hknow = ZonedDateTime.now(HONGKONG_ZONEID);
        LocalDate today = LocalDate.now(HONGKONG_ZONEID);
        ZonedDateTime startOfNextDay = today.plusDays(1).atStartOfDay(HONGKONG_ZONEID);
        Long delay = hknow.until(startOfNextDay, ChronoUnit.SECONDS);
        System.out.println("today:" + today + " startOfNextDay:" + startOfNextDay + " delay:" + delay);
        return delay;
    }

    private void send(SymBotClient botClient, InboundMessage cmd, String msg, int idx, long sequence, boolean needMention) {
        ZonedDateTime hknow = ZonedDateTime.now(HONGKONG_ZONEID);
        String tstamp = hknow.toString().substring(0, 19);
        String randStr = randomString(MSGNUM[idx]);
        String msgPart = tstamp + " [" + String.format("%05d", sequence) + "] " + msg;
        String strCard = "<card iconSrc=\"url\" accent=\"tempo-bg-color--blue\"> <header> Message Size: " + msgPart.length() + " Characters;   Additional Payload Size: " + MSGNUM[idx] + " Characters</header><body>" + randStr + "</body></card>";
        String fullMsg = msgPart + (needMention ? MENTION_USERS : "") + strCard;
        OutboundMessage msgOut = new OutboundMessage(fullMsg);
        botClient.getMessagesClient().sendMessage(cmd.getStream().getStreamId(), msgOut);
    }

    public void execute(SymBotClient botClient, InboundMessage cmd, String msg) {
        idx = 0;
        sequence = 1;

        final Runnable sendMsg = () -> {
            send(botClient, cmd, msg, idx, sequence, false);
            sequence++;
        };
        final Runnable pingUsers = () -> {
            send(botClient, cmd, msg, idx, sequence, true);
            idx = (idx + 1) % LEN_MSGNUM;
            sequence++;
        };

        futureSendMsg = scheduler.scheduleAtFixedRate(sendMsg, 0, 10, SECONDS);
        futurePingUsers = scheduler.scheduleAtFixedRate(pingUsers, getDelayUntilNextDay(), DAYS.toSeconds(1), SECONDS);
        // just for debug or test
        // futurePingUsers = scheduler.scheduleAtFixedRate(pingUsers, 60, 60, SECONDS);
    }

    public void stop() {
        System.out.println("PeriodicTask#stop() was called. futureSendMsg:" + futureSendMsg);
        if (futureSendMsg != null) {
            futureSendMsg.cancel(true);
        }
        if (futurePingUsers != null) {
            futurePingUsers.cancel(true);
        }
    }

    protected void finalize() throws Throwable {
        scheduler.shutdown();
    }
}