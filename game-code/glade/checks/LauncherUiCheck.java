package vn.dreamhouse.glade;

import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
import java.util.*;

class LauncherUiCheck {
    static void ok(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[]args)throws Exception{
        System.setProperty("dreamhouse.noAudio","true");
        var prefs=new Properties();prefs.setProperty("volume","0.25");prefs.setProperty("lowQuality","true");
        prefs.setProperty("resolution","1280 × 720");PlayerFiles.saveSettings(prefs);
        var log=PlayerFiles.directory("logs").resolve("ui-fixture.log");Files.writeString(log,"test only");
        try(var store=new SessionStore(PlayerFiles.directory("recovery"))){store.save(GardenScene.island()).get();}
        SwingUtilities.invokeAndWait(()->{
            var window=new GladeLauncher.Window();
            try{
                window.addNotify();
                ok(window.quality.getSelectedIndex()==1 && window.volume.getValue()==25
                        && window.resolution.getSelectedItem().equals("1280 × 720"),"preferences populate settings");
                ok(window.getRootPane().getDefaultButton()==window.play,"Enter maps to main play action");
                ok(window.play.getAccessibleContext().getAccessibleName().equals("CHƠI NGAY"),"play label is accessible");
                ok(!window.getIconImages().isEmpty(),"launcher has original app icon");
                ok(LauncherView.class.getResource("launcher-island.png")!=null,"launcher art on class path");
                var view=window.view;
                ok(view.profileButton.getToolTipText().contains("Cá Con"),"legacy profile has a default name");
                var rename=GladeLauncher.Window.class.getDeclaredMethod("savePlayerName",String.class);rename.setAccessible(true);
                rename.invoke(window,"  Bảo Nguyễn  ");
                ok(PlayerFiles.settings().getProperty("playerName").equals("Bảo Nguyễn")
                        && view.profileButton.getAccessibleContext().getAccessibleName().contains("Bảo Nguyễn"),"Unicode name persists and updates the visible profile");
                for(String invalid:java.util.List.of("   ","X".repeat(25),"A\nB","A\u202EB")){
                    try{GladeLauncher.validatePlayerName(invalid);throw new AssertionError("accepted invalid player name");}
                    catch(IllegalArgumentException expected){}
                }
                ok(view.worlds.get(LauncherWorld.GARDEN).isSelected(),"garden selected by default");
                view.worlds.get(LauncherWorld.ISLAND).doClick();
                ok(view.worlds.get(LauncherWorld.ISLAND).isSelected() && !view.worlds.get(LauncherWorld.GARDEN).isSelected(),"world selection is exclusive");
                ok(view.worlds.containsKey(LauncherWorld.RECOVERY),"an existing recovery appears");
                for(var size:java.util.List.of(new Dimension(840,570),new Dimension(760,540),new Dimension(1000,680))){
                    window.setSize(size);window.validate();
                    var bounds=new Rectangle(0,0,view.getWidth(),view.getHeight());
                    for(var control:view.getComponents()){
                        var r=control.getBounds();
                        ok(bounds.contains(r) && r.width>50 && r.height>5,"launcher control stays inside "+size+": "+control.getClass().getSimpleName());
                        if(control instanceof LauncherView.Button button){
                            int reserved=button.world==null?16:r.height+6;
                            ok(r.height>=25 && button.getFontMetrics(button.getFont()).stringWidth(button.getText())<=r.width-reserved,"button label readable at "+size+": "+button.getText());
                        }
                    }
                }
                window.quality.setSelectedIndex(0);ok(PlayerFiles.settings().getProperty("lowQuality").equals("true"),"changing dropdown alone does not write preferences");
                window.volume.setValue(75);window.resolution.setSelectedItem("1600 × 900");
                window.launcherSound.setSelected(false);
                var save=GladeLauncher.Window.class.getDeclaredMethod("savePreferences");save.setAccessible(true);save.invoke(window);
                var saved=PlayerFiles.settings();
                ok(saved.getProperty("volume").equals("0.75") && GladeLauncher.resolution(saved).equals(new Dimension(1600,900)),"confirmed preferences feed the game startup");
                ok(saved.getProperty("launcherSound").equals("false") && saved.getProperty("playerName").equals("Bảo Nguyễn"),"mute persists without replacing the profile");
                var file=PlayerFiles.root().resolve("Đảo của cá.glade").toAbsolutePath();
                ok(file.equals(LauncherWorld.fileFromArgs(new String[]{LauncherWorld.fileArgument(file)})),"file launch preserves accented paths");
                var finish=GladeLauncher.Window.class.getDeclaredMethod("finish",GameLaunchService.Result.class);finish.setAccessible(true);finish.invoke(window,new GameLaunchService.Result(GameLaunchService.State.FAILED,"Không mở được lượt thử.",log,123));ok(window.play.isEnabled() && window.play.getText().equals("Thử mở lại") && window.logs.isEnabled() && !window.progress.isIndeterminate(),"failure enables retry and log access");
            }catch(Exception error){throw new RuntimeException(error);}finally{window.dispose();}
        });
        for(int sound=0;sound<3;sound++){
            byte[] pcm=LauncherAudio.pcm(sound);
            ok(pcm.length>0 && pcm[0]==0 && pcm[1]==0 && pcm[pcm.length-1]==0 && pcm[pcm.length-2]==0,"audio begins and ends cleanly");
            int peak=0;
            for(int i=0;i<pcm.length;i+=2)peak=Math.max(peak,Math.abs((short)((pcm[i]&255)|(pcm[i+1]<<8))));
            ok(peak>200 && peak<32767,"PCM is audible without clipping");
        }
        System.out.println("LAUNCHER_UI_PASS: layout, Unicode profile, audio preferences and PCM, keyboard, retry and logs; no screenshots");
    }
}
