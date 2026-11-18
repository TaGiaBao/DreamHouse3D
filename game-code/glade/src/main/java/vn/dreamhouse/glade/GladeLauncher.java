package vn.dreamhouse.glade;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;

public final class GladeLauncher {
    private static GameSessionLock session;
    private static GameSessionLock launcherSession;
    public static void main(String[] args){
        boolean game=Arrays.asList(args).contains("--game") || Boolean.getBoolean("dreamhouse.probe") && !Boolean.getBoolean("dreamhouse.launcherProbe");
        for(String arg:args){if(arg.startsWith("--player-root64="))System.setProperty("dreamhouse.userDir",new String(java.util.Base64.getUrlDecoder().decode(arg.substring(16)),java.nio.charset.StandardCharsets.UTF_8));if(arg.startsWith("--launch-token="))System.setProperty(LaunchSignals.TOKEN_PROPERTY,arg.substring(15));if(arg.equals("--probe"))System.setProperty("dreamhouse.probe","true");if(arg.equals("--no-audio"))System.setProperty("dreamhouse.noAudio","true");}
        if(game){
            try{session=GameSessionLock.acquire();if(session==null){LaunchSignals.publish("ALREADY_RUNNING");System.out.println("GAME_ALREADY_RUNNING");return;}Runtime.getRuntime().addShutdownHook(new Thread(()->{try{session.close();}catch(IOException ignored){}},"dreamhouse-session-unlock"));GladeApp.main(args);}
            catch(Exception error){PlayerFiles.error("Không mở được game",error);LaunchSignals.publish("FAILED");System.err.println("GAME_START_FAILED "+error.getMessage());System.exit(1);}return;
        }
        try{launcherSession=GameSessionLock.acquireLauncher();if(launcherSession==null){System.out.println("LAUNCHER_ALREADY_OPEN");return;}Runtime.getRuntime().addShutdownHook(new Thread(GladeLauncher::releaseLauncher,"dreamhouse-launcher-unlock"));}
        catch(IOException error){PlayerFiles.error("Không truy cập được thư mục dữ liệu",error);if(Boolean.getBoolean("dreamhouse.launcherProbe")){System.exit(1);return;}}
        SwingUtilities.invokeLater(()->{
            try{var window=new Window();boolean probe=Boolean.getBoolean("dreamhouse.launcherProbe");window.setVisible(!probe);if(probe)window.launch();}
            catch(Exception error){PlayerFiles.error("Không mở được trình khởi động",error);JOptionPane.showMessageDialog(null,"Không mở được DreamHouse. Hãy kiểm tra quyền truy cập thư mục dữ liệu và mở lại bản đầy đủ.","DreamHouse",JOptionPane.ERROR_MESSAGE);if(Boolean.getBoolean("dreamhouse.launcherProbe"))System.exit(1);}
        });
    }
    private static synchronized void releaseLauncher(){var held=launcherSession;launcherSession=null;if(held!=null)try{held.close();}catch(IOException error){PlayerFiles.error("Đóng trình khởi động",error);}}
    static final String VERSION=System.getProperty("jpackage.app-version","1.0.0");
    static final String DEFAULT_PLAYER_NAME="Cá Con";
    private static final List<String> RESOLUTIONS=List.of("960 × 640","1280 × 720","1360 × 860","1600 × 900","1920 × 1080");
    private static final String DEFAULT_RESOLUTION="1360 × 860";

    static Dimension resolution(java.util.Properties preferences) {
        String selected=preferences.getProperty("resolution",DEFAULT_RESOLUTION);
        String[] dimensions=(RESOLUTIONS.contains(selected)?selected:DEFAULT_RESOLUTION).split(" × ");
        return new Dimension(Integer.parseInt(dimensions[0]),Integer.parseInt(dimensions[1]));
    }

    static String nextResolution(String selected) {
        return RESOLUTIONS.get((RESOLUTIONS.indexOf(selected)+1)%RESOLUTIONS.size());
    }

    static String validatePlayerName(String input) {
        String name=java.text.Normalizer.normalize(input.strip(),java.text.Normalizer.Form.NFC);
        if(name.codePoints().allMatch(ch->Character.isWhitespace(ch) || Character.isSpaceChar(ch))
                || name.codePointCount(0,name.length())>24
                || name.codePoints().anyMatch(ch->Character.isISOControl(ch) || Character.getType(ch)==Character.FORMAT
                        || Character.getType(ch)==Character.LINE_SEPARATOR || Character.getType(ch)==Character.PARAGRAPH_SEPARATOR))
            throw new IllegalArgumentException("Tên cần từ 1 đến 24 ký tự trên một dòng.");
        return name;
    }

    static final class Window extends JFrame {
        final LauncherView view=new LauncherView(SessionStore.hasRecoveryFile(PlayerFiles.root().resolve("recovery")));
        final JButton play=view.play;
        final JButton logs=view.logs;
        final JButton cancel=view.cancel;
        final JTextArea status=view.status;
        final JProgressBar progress=view.progress;
        final JComboBox<String> quality=new JComboBox<>(new String[]{"Đẹp — bóng mềm","Nhẹ — ưu tiên mượt"});
        final JComboBox<String> resolution=new JComboBox<>(RESOLUTIONS.toArray(String[]::new));
        final JSlider volume=new JSlider(0,100);
        final JCheckBox launcherSound=new JCheckBox("Nhạc nền và tiếng nút bấm");
        private final GameLaunchService service=new GameLaunchService();
        private final LauncherAudio audio=new LauncherAudio();
        private String playerName=DEFAULT_PLAYER_NAME;
        private LauncherWorld selectedWorld=LauncherWorld.GARDEN;
        private Path selectedFile;
        private boolean launching;

        Window(){
            super("DreamHouse · Khởi động");
            setIconImage(LauncherIcon.image(64));
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            setMinimumSize(new Dimension(760,540));
            setContentPane(view);
            pack();
            setLocationRelativeTo(null);
            readPreferences();
            getRootPane().setDefaultButton(play);
            play.addActionListener(e->launch());
            cancel.addActionListener(e->{audio.tap();cancelLaunch();});
            logs.addActionListener(e->{audio.tap();openLog();});
            view.settings.addActionListener(e->{audio.tap();showSettings();});
            view.quit.addActionListener(e->{audio.tap();requestClose();});
            view.openFile.addActionListener(e->{audio.tap();chooseWorldFile();});
            view.profileButton.addActionListener(e->{audio.tap();renamePlayer();});
            view.worlds.forEach((world,button)->button.addActionListener(e->{
                audio.tap();
                selectedWorld=world;
                view.select(world);
                status.setText(world.title+" · Bấm Chơi ngay để mở");
            }));
            addWindowListener(new WindowAdapter(){
                @Override public void windowOpened(WindowEvent e){audio.music(true);}
                @Override public void windowClosing(WindowEvent e){requestClose();}
            });
        }

        @Override public void dispose(){
            audio.close();
            super.dispose();
            releaseLauncher();
        }

        private void renamePlayer(){
            var input=new JTextField(playerName,24);
            var label=new JLabel("Tên hiển thị · tối đa 24 ký tự");
            label.setLabelFor(input);
            var options=new JPanel(new BorderLayout(0,7));
            options.add(label,BorderLayout.NORTH);
            options.add(input,BorderLayout.CENTER);
            input.selectAll();
            while(JOptionPane.showConfirmDialog(this,options,"Đổi tên người chơi",
                    JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION){
                try{
                    savePlayerName(input.getText());
                    audio.tap();
                    status.setText("Xin chào, "+playerName+"!");
                    return;
                }catch(IllegalArgumentException error){
                    label.setText(error.getMessage());
                    input.selectAll();
                }catch(IOException error){
                    PlayerFiles.error("Không lưu được tên người chơi",error);
                    status.setText("Chưa lưu được tên người chơi.");
                    return;
                }
            }
        }

        private void savePlayerName(String input)throws IOException{
            String name=validatePlayerName(input);
            var preferences=PlayerFiles.settings();
            preferences.setProperty("playerName",name);
            PlayerFiles.saveSettings(preferences);
            playerName=name;
            view.setPlayerName(name);
        }

        private void chooseWorldFile(){
            var picker=new JFileChooser();
            picker.setDialogTitle("Mở bản thiết kế DreamHouse");
            picker.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Bản thiết kế DreamHouse (.glade)","glade"));
            picker.setAcceptAllFileFilterUsed(false);
            if(selectedFile!=null)picker.setSelectedFile(selectedFile.toFile());
            if(picker.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)return;
            var file=picker.getSelectedFile().toPath();
            if(!Files.isRegularFile(file)){
                status.setText("Không tìm thấy bản thiết kế đã chọn.");
                return;
            }
            selectedFile=file;
            selectedWorld=LauncherWorld.FILE;
            view.select(selectedWorld);
            status.setText(file.getFileName()+" · Bấm Chơi ngay để mở");
        }

        private void requestClose(){
            if(!launching && !service.hasLiveChild()){dispose();return;}
            int choice=JOptionPane.showConfirmDialog(this,"Dừng lượt mở game này và đóng trình khởi động?",
                    "Đang mở DreamHouse",JOptionPane.YES_NO_OPTION);
            if(choice==JOptionPane.YES_OPTION)new Thread(()->{
                try{service.cancel();}catch(Exception error){PlayerFiles.error("Hủy khởi động",error);}
                SwingUtilities.invokeLater(this::dispose);
            },"dreamhouse-cancel").start();
        }

        private void readPreferences(){
            var preferences=PlayerFiles.settings();
            quality.setSelectedIndex(Boolean.parseBoolean(preferences.getProperty("lowQuality","false"))?1:0);
            String size=preferences.getProperty("resolution",DEFAULT_RESOLUTION);
            resolution.setSelectedItem(RESOLUTIONS.contains(size)?size:DEFAULT_RESOLUTION);
            try {volume.setValue(Math.round(Float.parseFloat(preferences.getProperty("volume","0.55"))*100));}
            catch(NumberFormatException error){volume.setValue(55);}
            try{playerName=validatePlayerName(preferences.getProperty("playerName",DEFAULT_PLAYER_NAME));}
            catch(IllegalArgumentException error){playerName=DEFAULT_PLAYER_NAME;}
            view.setPlayerName(playerName);
            launcherSound.setSelected(Boolean.parseBoolean(preferences.getProperty("launcherSound","true")));
            applyLauncherSound();
        }

        private void savePreferences()throws IOException{
            var preferences=PlayerFiles.settings();
            preferences.setProperty("lowQuality",Boolean.toString(quality.getSelectedIndex()==1));
            preferences.setProperty("resolution",(String)resolution.getSelectedItem());
            preferences.setProperty("volume",Float.toString(volume.getValue()/100f));
            preferences.setProperty("launcherSound",Boolean.toString(launcherSound.isSelected()));
            PlayerFiles.saveSettings(preferences);
            applyLauncherSound();
        }

        private void applyLauncherSound(){
            audio.configure(launcherSound.isSelected() && !Boolean.getBoolean("dreamhouse.noAudio")
                    && !Boolean.getBoolean("dreamhouse.launcherProbe"),volume.getValue());
        }

        private void showSettings(){
            readPreferences();
            var options=new JPanel(new GridLayout(0,2,10,9));
            options.setBorder(BorderFactory.createEmptyBorder(8,4,8,4));
            for(var control:List.of(quality,resolution,volume,launcherSound)){
                String name=control==quality?"Đồ họa":control==resolution?"Độ phân giải":control==volume?"Âm lượng":"Âm thanh launcher";
                var label=new JLabel(name);
                label.setLabelFor(control);
                options.add(label);
                options.add(control);
            }
            volume.setMajorTickSpacing(25);
            volume.setPaintLabels(true);
            var help=new JButton("Hướng dẫn nhanh");
            help.addActionListener(e->JOptionPane.showMessageDialog(help,
                    "Chọn Xây dựng rồi bấm hoặc kéo trên đất để đặt khối.\n"
                    +"Giữ chuột phải để xoay góc nhìn. Cuộn chuột để thu phóng.\n"
                    +"Dùng Hộp bản thiết kế để lưu hoặc mở thung lũng. Bấm F1 để học qua thao tác.",
                    "Làm quen với DreamHouse",JOptionPane.PLAIN_MESSAGE));
            options.add(help);
            int result=JOptionPane.showConfirmDialog(this,options,"Cài đặt DreamHouse",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
            if(result!=JOptionPane.OK_OPTION){readPreferences();return;}
            try{savePreferences();status.setText("Đã lưu cài đặt.");}
            catch(IOException error){PlayerFiles.error("Không lưu được cài đặt ở launcher",error);readPreferences();status.setText("Chưa lưu được cài đặt.");}
        }

        void launch(){
            if(launching)return;
            try{if(!service.hasLiveChild() && GameSessionLock.isRunning()){status.setText("Game đã mở. Hãy quay lại cửa sổ DreamHouse đang chạy.");return;}}
            catch(IOException error){status.setText("Không truy cập được thư mục dữ liệu của game.");PlayerFiles.error("Kiểm tra phiên chơi",error);return;}
            audio.music(false);
            if(!service.hasLiveChild())audio.launch();
            service.prepare();launching=true;play.setEnabled(false);setLaunchControls(false);
            cancel.setVisible(true);cancel.setEnabled(true);progress.setIndeterminate(true);status.setText("Đang mở cửa sổ game…");
            var world=selectedWorld;
            var file=selectedFile;
            var worker=new Thread(()->{
                try{var result=service.start(text->SwingUtilities.invokeLater(()->status.setText(text)),world,file);SwingUtilities.invokeLater(()->finish(result));}
                catch(Exception error){PlayerFiles.error("Khởi động game thất bại",error);SwingUtilities.invokeLater(()->finish(new GameLaunchService.Result(GameLaunchService.State.FAILED,"Chưa mở được game. Hãy kiểm tra bản chơi đầy đủ hoặc xem nhật ký.",service.log(),-1)));}
            },"dreamhouse-launch");worker.setDaemon(false);worker.start();
        }
        private void setLaunchControls(boolean enabled){
            view.settings.setEnabled(enabled);
            view.openFile.setEnabled(enabled);
            view.profileButton.setEnabled(enabled);
            view.worlds.values().forEach(button->button.setEnabled(enabled));
        }
        private void finish(GameLaunchService.Result result){
            launching=false;progress.setIndeterminate(false);status.setText(result.message());
            boolean stillLoading=service.hasLiveChild();
            if(!stillLoading && result.state()!=GameLaunchService.State.READY && isShowing())audio.music(true);
            boolean hasLog=result.log()!=null && Files.isRegularFile(result.log());
            setLaunchControls(!stillLoading && result.state()!=GameLaunchService.State.READY);
            play.setEnabled(true);
            logs.setVisible(hasLog && result.state()!=GameLaunchService.State.READY);logs.setEnabled(hasLog);
            cancel.setVisible(stillLoading);cancel.setEnabled(stillLoading);
            progress.setValue(result.state()==GameLaunchService.State.READY || result.state()==GameLaunchService.State.CANCELLED ? 100 : 0);
            play.setText(switch(result.state()){case WAITING->"Tiếp tục chờ";case FAILED->"Thử mở lại";default->"CHƠI NGAY";});
            if(result.state()==GameLaunchService.State.READY){
                play.setEnabled(false);progress.setValue(100);
                if(Boolean.getBoolean("dreamhouse.launcherProbe")){new Thread(()->{try{int exit=service.awaitExit(45000);boolean ready=Files.readString(result.log()).contains("PACKAGED_READY");System.out.println("LAUNCHER_READY pid="+result.pid()+" childExit="+exit+" gameProbe="+ready+" log="+result.log());SwingUtilities.invokeLater(()->dispose());if(exit!=0 || !ready)System.exit(1);}catch(Exception error){error.printStackTrace();System.exit(1);}},"launcher-probe-exit").start();}
                else{var close=new Timer(350,e->{((Timer)e.getSource()).stop();dispose();});close.setRepeats(false);close.start();}
            }else if(Boolean.getBoolean("dreamhouse.launcherProbe")){System.err.println("LAUNCHER_PROBE_FAILED "+result.state()+" "+result.message());dispose();System.exit(1);}
        }
        private void cancelLaunch(){
            cancel.setEnabled(false);
            new Thread(()->{
                try{
                    service.cancel();
                    SwingUtilities.invokeLater(()->{
                        if(!launching && !service.hasLiveChild() && "Tiếp tục chờ".equals(play.getText())){
                            finish(new GameLaunchService.Result(GameLaunchService.State.CANCELLED,"Đã hủy lượt mở game này.",service.log(),-1));
                        }
                    });
                }catch(Exception error){
                    PlayerFiles.error("Hủy khởi động",error);
                    SwingUtilities.invokeLater(()->cancel.setEnabled(service.hasLiveChild()));
                }
            },"dreamhouse-cancel").start();
        }
        private void openLog(){try{if(service.log()!=null && Desktop.isDesktopSupported())Desktop.getDesktop().open(service.log().toFile());}catch(Exception error){status.setText("Nhật ký nằm tại: "+service.log());}}
    }
}
