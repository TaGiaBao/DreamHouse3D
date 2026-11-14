package vn.dreamhouse.glade;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

final class GameLaunchService {
    enum State { READY, ALREADY_RUNNING, FAILED, WAITING, CANCELLED }
    record Result(State state,String message,Path log,long pid){}
    private final AtomicBoolean watching=new AtomicBoolean();
    private volatile Process child;
    private volatile String token;
    private volatile Path log;
    private volatile boolean cancelled,handedOff;
    private final java.util.function.BiFunction<String,Path,List<String>> commands;
    private final long startupTimeout;
    GameLaunchService(){this(GameLaunchService::command,90000);}
    GameLaunchService(java.util.function.BiFunction<String,Path,List<String>> commands,long startupTimeout){this.commands=commands;this.startupTimeout=startupTimeout;}
    static List<String> command(String token,Path playerRoot){
        var command=new ArrayList<String>();String packaged=System.getProperty("jpackage.app-path","");
        if(packaged.isBlank()){String current=ProcessHandle.current().info().command().orElse("");if(current.toLowerCase(Locale.ROOT).endsWith("dreamhouse.exe"))packaged=current;}
        if(!packaged.isBlank()){
            if(!Files.isRegularFile(Path.of(packaged)))throw new IllegalArgumentException("Không tìm được bản chơi. Hãy mở lại từ thư mục đầy đủ của DreamHouse.");command.add(packaged);
        }else{
            Path java=Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").startsWith("Windows")?"javaw.exe":"java");
            if(!Files.isRegularFile(java))throw new IllegalArgumentException("Thiếu thành phần khởi chạy. Hãy dùng bản DreamHouse đầy đủ.");
            command.add(java.toString());command.add("--enable-native-access=ALL-UNNAMED");command.add("-cp");command.add(absoluteClassPath());command.add(GladeLauncher.class.getName());
        }
        command.add("--game");command.add("--launch-token="+token);command.add("--player-root64="+Base64.getUrlEncoder().withoutPadding().encodeToString(playerRoot.toAbsolutePath().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        if(Boolean.getBoolean("dreamhouse.probe"))command.add("--probe");
        if(Boolean.getBoolean("dreamhouse.noAudio"))command.add("--no-audio");
        return List.copyOf(command);
    }
    static String absoluteClassPath(){
        return Arrays.stream(System.getProperty("java.class.path").split(java.util.regex.Pattern.quote(File.pathSeparator),-1)).map(entry->{
            if(entry.endsWith("*")){String folder=entry.substring(0,entry.length()-1);return Path.of(folder.isEmpty()?".":folder).toAbsolutePath().normalize()+File.separator+"*";}
            return Path.of(entry.isEmpty()?".":entry).toAbsolutePath().normalize().toString();
        }).collect(java.util.stream.Collectors.joining(File.pathSeparator));
    }
    boolean busy(){return watching.get();}
    Path log(){return log;}
    int awaitExit(long timeoutMs)throws InterruptedException{if(child==null || !child.waitFor(timeoutMs,java.util.concurrent.TimeUnit.MILLISECONDS))return -1;return child.exitValue();}
    boolean hasLiveChild(){return child!=null && child.isAlive() && !handedOff;}
    void prepare(){if(!watching.get() && !hasLiveChild()){cancelled=false;handedOff=false;}}
    Result start(Consumer<String> progress)throws IOException,InterruptedException{
        return start(progress,LauncherWorld.MENU,null);
    }
    synchronized Result start(Consumer<String> progress,LauncherWorld world,Path worldFile)throws IOException,InterruptedException{
        if(hasLiveChild())return monitor(progress,startupTimeout);
        if(GameSessionLock.isRunning())return new Result(State.ALREADY_RUNNING,"Game đã mở. Hãy quay lại cửa sổ DreamHouse đang chạy.",null,-1);
        if(cancelled)return new Result(State.CANCELLED,"Đã hủy lượt mở game này.",log,-1);
        handedOff=false;token=UUID.randomUUID().toString();
        var folder=PlayerFiles.directory("logs");log=folder.resolve("launcher-"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))+"-"+token.substring(0,8)+".log");
        progress.accept("Đang mở cửa sổ game…");
        var launchCommand=new ArrayList<>(commands.apply(token,PlayerFiles.root()));
        if(world!=LauncherWorld.MENU)launchCommand.add(world.argument());
        if(world==LauncherWorld.FILE && worldFile!=null)launchCommand.add(LauncherWorld.fileArgument(worldFile));
        var builder=new ProcessBuilder(launchCommand);builder.directory(PlayerFiles.root().toFile());builder.redirectErrorStream(true);builder.redirectOutput(log.toFile());
        child=builder.start();if(cancelled){cancel();return new Result(State.CANCELLED,"Đã hủy lượt mở game này.",log,child.pid());}return monitor(progress,startupTimeout);
    }
    Result monitor(Consumer<String> progress,long timeoutMs)throws IOException,InterruptedException{
        if(!watching.compareAndSet(false,true))throw new IllegalStateException("Đang có một lượt khởi động.");
        long deadline=System.nanoTime()+timeoutMs*1_000_000;String previous="";
        try{
            while(true){
                String status=LaunchSignals.read(token);
                if(status.equals("READY")){handedOff=true;Files.deleteIfExists(LaunchSignals.path(token));return new Result(State.READY,"Thung lũng đã sẵn sàng.",log,child.pid());}
                if(status.equals("ALREADY_RUNNING")){Files.deleteIfExists(LaunchSignals.path(token));return new Result(State.ALREADY_RUNNING,"Game đã mở. Hãy quay lại cửa sổ DreamHouse đang chạy.",log,child.pid());}
                if(cancelled)return new Result(State.CANCELLED,"Đã hủy lượt mở game này.",log,child.pid());
                if(status.equals("FAILED")){
                    cancel();if(handedOff)return new Result(State.READY,"Thung lũng đã sẵn sàng.",log,child.pid());
                    return new Result(State.FAILED,"Game chưa khởi động được. Bạn có thể thử mức Nhẹ hoặc mở nhật ký để xem chi tiết.",log,child.pid());
                }
                if(!child.isAlive())return new Result(State.FAILED,"Cửa sổ game đã đóng trước khi sẵn sàng. Bạn có thể thử lại hoặc xem nhật ký.",log,child.pid());
                if(!previous.equals(status)){previous=status;progress.accept(switch(status){case "GRAPHICS"->"Đang chuẩn bị đồ họa…";case "SCENE"->"Đang dựng thung lũng…";case "FIRST_FRAME"->"Đang hoàn tất khung hình đầu tiên…";default->"Đang mở cửa sổ game…";});}
                if(System.nanoTime()>=deadline)return new Result(State.WAITING,"Máy đang chuẩn bị lâu hơn thường lệ. Bấm Tiếp tục chờ để theo dõi lượt mở hiện tại.",log,child.pid());
                Thread.sleep(100);
            }
        }finally{watching.set(false);}
    }
    void cancel()throws IOException,InterruptedException{
        if(handedOff)return;cancelled=true;if(child==null || !child.isAlive())return;
        if("READY".equals(LaunchSignals.read(token))){handedOff=true;return;}
        child.destroy();if(!child.waitFor(3,java.util.concurrent.TimeUnit.SECONDS))child.destroyForcibly();Files.deleteIfExists(LaunchSignals.path(token));
    }
}
