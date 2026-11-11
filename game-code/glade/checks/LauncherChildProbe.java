package vn.dreamhouse.glade;

/** Controlled subprocess for launch lifecycle checks; never renders or modifies a world. */
class LauncherChildProbe {
    public static void main(String[]args)throws Exception{
        System.setProperty(LaunchSignals.TOKEN_PROPERTY,args[0]);System.setProperty("dreamhouse.userDir",new String(java.util.Base64.getUrlDecoder().decode(args[1]),java.nio.charset.StandardCharsets.UTF_8));String mode=args[2];
        if(mode.equals("exit")){System.err.println("EXPECTED_TEST_EXIT");System.exit(7);}
        try(var session=GameSessionLock.acquire()){
            if(session==null){LaunchSignals.publish("ALREADY_RUNNING");return;}
            LaunchSignals.publish("GRAPHICS");Thread.sleep(120);LaunchSignals.publish("SCENE");
            if(mode.equals("failedsignal")){LaunchSignals.publish("FAILED");Thread.sleep(30000);return;}
            if(mode.equals("hold")){Thread.sleep(30000);return;}
            if(mode.equals("late"))Thread.sleep(800);
            LaunchSignals.publish("FIRST_FRAME");Thread.sleep(120);LaunchSignals.publish("READY");Thread.sleep(250);System.out.println("CONTROLLED_GAME_READY");
        }
    }
}
