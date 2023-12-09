package anti.sus;

public final class Main {

    private static final DiscordChatFilter discordChatFilter;

    static {
        Runtime.getRuntime().addShutdownHook(getShutdownThread());
        discordChatFilter = new DiscordChatFilter();
    }

    public static void main(final String[] args) {
        discordChatFilter.heartbeat();
    }

    private static void shutdown() {
        System.out.println("Shutdown initiated!");
        discordChatFilter.shutdown();
        System.out.println("Shutdown complete!");
    }

    private static Thread getShutdownThread() {
        return new Thread(Main::shutdown);
    }

    public static void runSync(final Runnable task) {
        discordChatFilter.runSync(task);
    }
}