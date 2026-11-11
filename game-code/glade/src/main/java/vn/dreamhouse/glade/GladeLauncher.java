package vn.dreamhouse.glade;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;

/** Java launcher shipped in the same portable executable as the game. */
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
    static final Color INK=new Color(91,68,44),CREAM=new Color(255,246,220);
    static final class RoundButton extends JButton {
        private final Color top,bottom;
        private float hover;
        private final Timer animation;
        RoundButton(String text,Color top,Color bottom){
            super(text);this.top=top;this.bottom=bottom;setFont(new Font("Segoe UI",Font.BOLD,15));setForeground(INK);setContentAreaFilled(false);setBorderPainted(false);setFocusPainted(false);setOpaque(false);setRolloverEnabled(true);setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));setPreferredSize(new Dimension(220,44));setBorder(new EmptyBorder(8,14,10,14));
            animation=new Timer(16,e->{float target=getModel().isRollover() && isEnabled()?1:0;hover+=(target-hover)*.30f;if(Math.abs(target-hover)<.01f){hover=target;((Timer)e.getSource()).stop();}repaint();});
            addMouseListener(new MouseAdapter(){public void mouseEntered(MouseEvent e){animation.start();}public void mouseExited(MouseEvent e){animation.start();}});addFocusListener(new FocusAdapter(){public void focusGained(FocusEvent e){repaint();}public void focusLost(FocusEvent e){repaint();}});
        }
        @Override protected void paintComponent(Graphics graphics){
            var g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);int w=getWidth(),h=getHeight(),drop=getModel().isPressed()?2:0;
            g.setColor(new Color(87,58,35,50));g.fillRoundRect(1,5,w-2,h-6,18,18);g.setColor(new Color(160,124,77));g.fillRoundRect(0,drop,w,h-5,18,18);
            Color a=isEnabled()?tint(top,hover*.12f):new Color(223,216,195),b=isEnabled()?tint(bottom,hover*.12f):new Color(205,199,180);
            g.setPaint(new GradientPaint(0,drop+3,a,0,h,b));g.fillRoundRect(3,drop+3,w-6,h-11,14,14);g.setColor(new Color(255,255,240,160));g.drawRoundRect(5,drop+5,w-11,h-16,12,12);
            if(isFocusOwner()){g.setStroke(new BasicStroke(2));g.setColor(new Color(80,125,104));g.drawRoundRect(6,drop+6,w-13,h-18,11,11);}
            g.setFont(getFont());g.setColor(isEnabled()?getForeground():new Color(135,127,111));var fm=g.getFontMetrics();g.drawString(getText(),(w-fm.stringWidth(getText()))/2,(h+fm.getAscent()-fm.getDescent())/2-2+drop);g.dispose();
        }
        @Override public void removeNotify(){animation.stop();super.removeNotify();}
        private static Color tint(Color color,float amount){return new Color((int)(color.getRed()+(255-color.getRed())*amount),(int)(color.getGreen()+(255-color.getGreen())*amount),(int)(color.getBlue()+(255-color.getBlue())*amount));}
    }
    static final class Illustration extends JPanel {
        Illustration(){setOpaque(false);setPreferredSize(new Dimension(330,350));getAccessibleContext().setAccessibleName("Minh họa ngôi nhà DreamHouse");}
        @Override protected void paintComponent(Graphics graphics){
            var g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);int w=getWidth(),h=getHeight();
            g.setPaint(new GradientPaint(0,0,new Color(221,237,208),w,h,new Color(162,197,169)));g.fill(new RoundRectangle2D.Float(0,0,w,h,28,28));g.setColor(new Color(255,248,206,150));g.fillOval(w-100,25,65,65);
            g.setColor(new Color(130,166,117));g.fillOval(-30,h-115,w+80,130);g.setColor(new Color(157,187,130));g.fillOval(-40,h-84,w+50,120);
            GladeIcons.paint(g,"TREE",w-130,h-235,140);GladeIcons.paint(g,"TREE",5,h-185,120);GladeIcons.paint(g,"HOUSE",w/2-117,h-267,230);GladeIcons.paint(g,"FLOWERS",30,h-90,64);GladeIcons.paint(g,"FLOWERS",w-94,h-88,62);
            g.setColor(new Color(255,250,230,210));g.fillRoundRect(18,h-42,w-36,26,12,12);g.setColor(INK);g.setFont(new Font("Segoe UI",Font.PLAIN,12));String caption="Một thung lũng, theo cách của bạn";g.drawString(caption,(w-g.getFontMetrics().stringWidth(caption))/2,h-24);g.dispose();
        }
    }
    static final class Window extends JFrame {
        final RoundButton play=new RoundButton("Vào thung lũng",new Color(215,237,159),new Color(149,188,102));
        final RoundButton help=new RoundButton("Hướng dẫn nhanh",new Color(254,241,203),new Color(226,201,157));
        final RoundButton logs=new RoundButton("Xem nhật ký",new Color(239,230,207),new Color(215,199,168));
        final RoundButton cancel=new RoundButton("Hủy mở",new Color(251,215,193),new Color(221,168,143));
        final JComboBox<String> quality=new JComboBox<>(new String[]{"Đẹp — bóng mềm","Nhẹ — ưu tiên mượt"});
        final JTextArea status=new JTextArea("Sẵn sàng. Thiết kế và mẫu đã lưu của bạn được giữ trong thư mục dữ liệu riêng.");
        final JProgressBar progress=new JProgressBar();
        private final GameLaunchService service=new GameLaunchService();
        private boolean launching;
        Window(){
            super("DreamHouse · Khởi động");setIconImage(LauncherIcon.image(64));setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);setMinimumSize(new Dimension(760,540));setSize(840,570);setLocationRelativeTo(null);
            var root=new JPanel(new BorderLayout(22,16));root.setBackground(CREAM);root.setBorder(new EmptyBorder(24,26,20,26));setContentPane(root);
            var heading=new JLabel("DreamHouse");heading.setFont(new Font("Segoe UI",Font.BOLD,32));heading.setForeground(INK);root.add(heading,BorderLayout.NORTH);
            var body=new JPanel(new GridLayout(1,2,24,0));body.setOpaque(false);body.add(new Illustration());root.add(body,BorderLayout.CENTER);
            var controls=new JPanel();controls.setLayout(new BoxLayout(controls,BoxLayout.Y_AXIS));controls.setOpaque(false);body.add(controls);
            var title=new JLabel("Xây nên góc bình yên của bạn");title.setFont(new Font("Segoe UI",Font.BOLD,17));title.setForeground(INK);title.setAlignmentX(LEFT_ALIGNMENT);controls.add(title);controls.add(Box.createVerticalStrut(15));
            var label=new JLabel("Đồ họa khi vào game");label.setForeground(INK);label.setLabelFor(quality);controls.add(label);controls.add(Box.createVerticalStrut(6));
            quality.setFont(new Font("Segoe UI",Font.PLAIN,14));quality.setBackground(new Color(255,250,232));quality.setForeground(INK);quality.setMaximumSize(new Dimension(Integer.MAX_VALUE,38));quality.setAlignmentX(LEFT_ALIGNMENT);quality.setSelectedIndex(Boolean.parseBoolean(PlayerFiles.settings().getProperty("lowQuality","false"))?1:0);quality.setToolTipText("Bạn có thể đổi lại trong phần Cài đặt của game.");controls.add(quality);controls.add(Box.createVerticalStrut(17));
            for(var button:List.of(play,help)){button.setAlignmentX(LEFT_ALIGNMENT);button.setMaximumSize(new Dimension(Integer.MAX_VALUE,48));controls.add(button);controls.add(Box.createVerticalStrut(7));}
            status.setEditable(false);status.setLineWrap(true);status.setWrapStyleWord(true);status.setOpaque(false);status.setFont(new Font("Segoe UI",Font.PLAIN,13));status.setForeground(INK);status.setAlignmentX(LEFT_ALIGNMENT);status.setFocusable(false);status.setMaximumSize(new Dimension(Integer.MAX_VALUE,72));controls.add(status);controls.add(Box.createVerticalStrut(8));
            progress.setIndeterminate(false);progress.setStringPainted(false);progress.setForeground(new Color(133,170,102));progress.setBackground(new Color(231,220,194));progress.setMaximumSize(new Dimension(Integer.MAX_VALUE,9));progress.setAlignmentX(LEFT_ALIGNMENT);controls.add(progress);controls.add(Box.createVerticalStrut(8));
            var actions=new JPanel(new GridLayout(1,2,8,0));actions.setOpaque(false);actions.setMaximumSize(new Dimension(Integer.MAX_VALUE,43));actions.setAlignmentX(LEFT_ALIGNMENT);actions.add(logs);actions.add(cancel);controls.add(actions);logs.setEnabled(false);cancel.setEnabled(false);
            var footer=new JLabel("Giữ chuột phải để xoay cảnh · F1 để học qua thao tác",SwingConstants.CENTER);footer.setFont(new Font("Segoe UI",Font.PLAIN,12));footer.setForeground(new Color(126,108,79));root.add(footer,BorderLayout.SOUTH);
            getRootPane().setDefaultButton(play);play.addActionListener(e->launch());cancel.addActionListener(e->cancelLaunch());help.addActionListener(e->JOptionPane.showMessageDialog(this,"1. Xây dựng → chọn khối → bấm hoặc kéo trên đất.\n2. Chọn nhà rồi kéo mũi tên/điểm trắng để sửa.\n3. Bảng màu → bấm khối để đổi vật liệu.\n4. Hộp bản thiết kế để lưu cảnh hoặc cất mẫu.\n\nGiữ phải: xoay · Giữ giữa: kéo cảnh · WASD: di chuyển\nCtrl+Z/Y: hoàn tác/làm lại · Esc: hủy thao tác\nF1 trong game để bắt đầu học nhanh.","Làm quen với DreamHouse",JOptionPane.PLAIN_MESSAGE));logs.addActionListener(e->openLog());
            addWindowListener(new WindowAdapter(){@Override public void windowClosed(WindowEvent e){releaseLauncher();}@Override public void windowClosing(WindowEvent e){if(launching || service.hasLiveChild()){int result=JOptionPane.showConfirmDialog(Window.this,"Dừng lượt mở game này và đóng trình khởi động?","Đang mở DreamHouse",JOptionPane.YES_NO_OPTION);if(result!=JOptionPane.YES_OPTION)return;new Thread(()->{try{service.cancel();}catch(Exception ignored){}SwingUtilities.invokeLater(()->dispose());},"dreamhouse-cancel").start();}else dispose();}});
        }
        void launch(){
            if(launching)return;
            try{if(!service.hasLiveChild() && GameSessionLock.isRunning()){status.setText("Game đã mở. Hãy quay lại cửa sổ DreamHouse đang chạy.");return;}}
            catch(IOException error){status.setText("Không truy cập được thư mục dữ liệu của game.");PlayerFiles.error("Kiểm tra phiên chơi",error);return;}
            try{var preferences=PlayerFiles.settings();preferences.setProperty("lowQuality",Boolean.toString(quality.getSelectedIndex()==1));PlayerFiles.saveSettings(preferences);}
            catch(IOException error){status.setText("Chưa lưu được lựa chọn. Hãy kiểm tra quyền truy cập thư mục dữ liệu rồi thử lại.");PlayerFiles.error("Không lưu được cài đặt ở launcher",error);return;}
            service.prepare();launching=true;play.setEnabled(false);quality.setEnabled(false);help.setEnabled(false);cancel.setEnabled(true);progress.setIndeterminate(true);status.setText("Đang mở cửa sổ game…");
            var worker=new Thread(()->{
                try{var result=service.start(text->SwingUtilities.invokeLater(()->status.setText(text)));SwingUtilities.invokeLater(()->finish(result));}
                catch(Exception error){PlayerFiles.error("Khởi động game thất bại",error);SwingUtilities.invokeLater(()->finish(new GameLaunchService.Result(GameLaunchService.State.FAILED,"Chưa mở được game. Hãy kiểm tra bản chơi đầy đủ hoặc xem nhật ký.",service.log(),-1)));}
            },"dreamhouse-launch");worker.setDaemon(false);worker.start();
        }
        private void finish(GameLaunchService.Result result){
            launching=false;progress.setIndeterminate(false);status.setText(result.message());quality.setEnabled(!service.hasLiveChild());help.setEnabled(true);play.setEnabled(true);logs.setEnabled(result.log()!=null && Files.isRegularFile(result.log()));cancel.setEnabled(service.hasLiveChild());
            play.setText(switch(result.state()){case WAITING->"Tiếp tục chờ";case FAILED->"Thử mở lại";default->"Vào thung lũng";});
            if(result.state()==GameLaunchService.State.READY){
                play.setEnabled(false);progress.setValue(100);
                if(Boolean.getBoolean("dreamhouse.launcherProbe")){new Thread(()->{try{int exit=service.awaitExit(45000);boolean ready=Files.readString(result.log()).contains("PACKAGED_READY");System.out.println("LAUNCHER_READY pid="+result.pid()+" childExit="+exit+" gameProbe="+ready+" log="+result.log());SwingUtilities.invokeLater(()->dispose());if(exit!=0 || !ready)System.exit(1);}catch(Exception error){error.printStackTrace();System.exit(1);}},"launcher-probe-exit").start();}
                else{var close=new Timer(350,e->{((Timer)e.getSource()).stop();dispose();});close.setRepeats(false);close.start();}
            }else if(Boolean.getBoolean("dreamhouse.launcherProbe")){System.err.println("LAUNCHER_PROBE_FAILED "+result.state()+" "+result.message());dispose();System.exit(1);}
        }
        private void cancelLaunch(){cancel.setEnabled(false);new Thread(()->{try{service.cancel();}catch(Exception error){PlayerFiles.error("Hủy khởi động",error);}},"dreamhouse-cancel").start();}
        private void openLog(){try{if(service.log()!=null && Desktop.isDesktopSupported())Desktop.getDesktop().open(service.log().toFile());}catch(Exception error){status.setText("Nhật ký nằm tại: "+service.log());}}
    }
}
