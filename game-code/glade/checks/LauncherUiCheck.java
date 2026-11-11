package vn.dreamhouse.glade;

import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
import java.util.*;

class LauncherUiCheck {
    static void ok(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[]args)throws Exception{
        var prefs=new Properties();prefs.setProperty("volume","0.25");prefs.setProperty("lowQuality","true");PlayerFiles.saveSettings(prefs);var log=PlayerFiles.directory("logs").resolve("ui-fixture.log");Files.writeString(log,"test only");
        SwingUtilities.invokeAndWait(()->{
            var window=new GladeLauncher.Window();
            try{
                window.addNotify();
                ok(window.quality.getSelectedIndex()==1,"saved quality selected");ok(window.getRootPane().getDefaultButton()==window.play,"Enter maps to main play action");ok(window.play.getAccessibleContext().getAccessibleName().equals("Vào thung lũng"),"primary button has accessible label");ok(!window.getIconImages().isEmpty(),"launcher has original app icon");
                for(var size:java.util.List.of(new Dimension(840,570),new Dimension(760,540),new Dimension(1000,680))){window.setSize(size);window.validate();var content=window.getContentPane();var bounds=new Rectangle(0,0,content.getWidth(),content.getHeight());for(var control:java.util.List.of(window.play,window.help,window.logs,window.cancel,window.quality,window.status)){var r=SwingUtilities.convertRectangle(control.getParent(),control.getBounds(),content);ok(bounds.contains(r) && r.width>50 && r.height>5,"launcher control stays inside "+size+": "+control.getClass().getSimpleName());if(control==window.status)ok(r.height>=32,"status has room for multiple lines at "+size);if(control instanceof JButton button)ok(r.height>=35 && button.getFontMetrics(button.getFont()).stringWidth(button.getText())<=r.width-16,"button label readable at "+size);}}
                window.quality.setSelectedIndex(0);ok(PlayerFiles.settings().getProperty("lowQuality").equals("true"),"changing dropdown alone does not write preferences");
                var finish=GladeLauncher.Window.class.getDeclaredMethod("finish",GameLaunchService.Result.class);finish.setAccessible(true);finish.invoke(window,new GameLaunchService.Result(GameLaunchService.State.FAILED,"Không mở được lượt thử.",log,123));ok(window.play.isEnabled() && window.play.getText().equals("Thử mở lại") && window.logs.isEnabled() && !window.progress.isIndeterminate(),"failure enables retry and log access");
            }catch(Exception error){throw new RuntimeException(error);}finally{window.dispose();}
        });
        System.out.println("LAUNCHER_UI_PASS: hidden Swing component checks at three sizes, keyboard/accessibility, original icon, quality persistence, error/retry/log states; no screenshots");
    }
}
