package vn.dreamhouse.glade;

import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

class LauncherDataCheck {
    static void ok(boolean value,String message){if(!value)throw new AssertionError(message);}
    static List<String> fixture(String token,Path root,String mode){return List.of(Path.of(System.getProperty("java.home"),"bin","javaw.exe").toString(),"-cp",GameLaunchService.absoluteClassPath(),LauncherChildProbe.class.getName(),token,Base64.getUrlEncoder().withoutPadding().encodeToString(root.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)),mode);}
    public static void main(String[]args)throws Exception{
        var folder=PlayerFiles.directory("launcher có dấu và khoảng trắng");String oldRoot=System.getProperty("dreamhouse.userDir");
        System.setProperty("dreamhouse.userDir",folder.toString());
        try{
            Files.writeString(folder.resolve("game-session.lock"),"stale file");ok(!GameSessionLock.isRunning(),"stale lock file does not block launch");
            try(var held=GameSessionLock.acquire()){
                ok(held!=null && GameSessionLock.isRunning(),"live OS lock detected");
                var blocked=new GameLaunchService((t,r)->{throw new AssertionError("must not spawn");},1000);ok(blocked.start(s->{}).state()==GameLaunchService.State.ALREADY_RUNNING,"another session is observed without spawning or killing it");
            }
            ok(!GameSessionLock.isRunning(),"closed session releases lock");
            try(var launcher=GameSessionLock.acquireLauncher()){ok(launcher!=null && GameSessionLock.acquireLauncher()==null && !GameSessionLock.isRunning(),"launcher lock is exclusive and separate from game session");}
            var cancelled=new GameLaunchService((t,r)->{throw new AssertionError("cancelled start must not spawn");},1000);cancelled.prepare();cancelled.cancel();ok(cancelled.start(s->{}).state()==GameLaunchService.State.CANCELLED,"cancel before process creation is respected");
            var progress=new ArrayList<String>();var ready=new GameLaunchService((t,r)->fixture(t,r,"ready"),5000);ready.prepare();var result=ready.start(progress::add);ok(result.state()==GameLaunchService.State.READY && ready.awaitExit(5000)==0,"readiness receipt and child exit are real");ok(Files.readString(result.log()).contains("CONTROLLED_GAME_READY"),"child logs are captured");ok(progress.size()>=2,"progress uses child stages");
            var failed=new GameLaunchService((t,r)->fixture(t,r,"exit"),5000);failed.prepare();var failure=failed.start(s->{});ok(failure.state()==GameLaunchService.State.FAILED && failed.awaitExit(5000)==7,"early nonzero exit is not reported ready");ok(Files.readString(failure.log()).contains("EXPECTED_TEST_EXIT"),"failure log available");
            var attempts=new java.util.concurrent.atomic.AtomicInteger();var retry=new GameLaunchService((t,r)->fixture(t,r,attempts.getAndIncrement()==0?"failedsignal":"ready"),5000);retry.prepare();ok(retry.start(s->{}).state()==GameLaunchService.State.FAILED && !retry.hasLiveChild(),"failed pre-ready child is stopped before retry");retry.prepare();ok(retry.start(s->{}).state()==GameLaunchService.State.READY && retry.awaitExit(5000)==0,"retry after a signalled startup failure starts cleanly");
            var late=new GameLaunchService((t,r)->fixture(t,r,"late"),200);late.prepare();var waiting=late.start(s->{});ok(waiting.state()==GameLaunchService.State.WAITING && late.hasLiveChild(),"slow child is kept alive for explicit wait/cancel");var resumed=late.monitor(s->{},5000);ok(resumed.state()==GameLaunchService.State.READY && resumed.pid()==waiting.pid(),"waiting resumes same child rather than spawning again");ok(late.awaitExit(5000)==0,"late child exits cleanly");
            var hold=new GameLaunchService((t,r)->fixture(t,r,"hold"),5000);hold.prepare();var future=CompletableFuture.supplyAsync(()->{try{return hold.start(s->{});}catch(Exception e){throw new CompletionException(e);}});
            long deadline=System.nanoTime()+5_000_000_000L;while(!hold.hasLiveChild() && System.nanoTime()<deadline)Thread.sleep(20);ok(hold.hasLiveChild(),"owned startup process exists");hold.cancel();ok(future.get(5,TimeUnit.SECONDS).state()==GameLaunchService.State.CANCELLED && !hold.hasLiveChild(),"cancel stops only owned pre-ready process");
            Files.writeString(folder.resolve("settings.properties"),"broken=\\uGGGG\n");ok(PlayerFiles.settings().isEmpty(),"malformed settings fall back without breaking launcher");var prefs=new Properties();prefs.setProperty("volume","0.25");prefs.setProperty("lowQuality","true");PlayerFiles.saveSettings(prefs);ok(PlayerFiles.settings().getProperty("volume").equals("0.25"),"quality storage preserves other valid settings");
            String token=UUID.randomUUID().toString();System.setProperty(LaunchSignals.TOKEN_PROPERTY,token);LaunchSignals.publish("FIRST_FRAME");ok(LaunchSignals.read(token).equals("FIRST_FRAME"),"status files handle unicode player path");System.clearProperty(LaunchSignals.TOKEN_PROPERTY);
            try{LaunchSignals.path("../outside");throw new AssertionError("unsafe token accepted");}catch(IllegalArgumentException expected){}
            var command=GameLaunchService.command(token,folder);String encoded=command.stream().filter(a->a.startsWith("--player-root64=")).findFirst().orElseThrow().substring(16);ok(new String(Base64.getUrlDecoder().decode(encoded),java.nio.charset.StandardCharsets.UTF_8).equals(folder.toAbsolutePath().toString()) && command.contains("--launch-token="+token),"arguments preserve full unicode path through ASCII transport");
            var icon=folder.resolve("icon.ico");LauncherIcon.main(new String[]{icon.toString()});var bytes=ByteBuffer.wrap(Files.readAllBytes(icon)).order(ByteOrder.LITTLE_ENDIAN);ok(bytes.getShort()==0 && bytes.getShort()==1 && bytes.getShort()==6,"ICO contains six original icon sizes");ok((LauncherIcon.image(32).getRGB(0,0)>>>24)==0,"icon preserves transparent corner");
            System.out.println("LAUNCHER_DATA_PASS: OS session lock, stale-file recovery, no duplicate spawn, early cancel, real readiness/stages, nonzero exit/logs, timeout/resume same child, owned-process cancellation, unicode paths, settings fallback/preservation, safe receipts and six-size icon; no screenshots");
        }finally{System.clearProperty(LaunchSignals.TOKEN_PROPERTY);if(oldRoot==null)System.clearProperty("dreamhouse.userDir");else System.setProperty("dreamhouse.userDir",oldRoot);}
    }
}
