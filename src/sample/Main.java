package sample;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.sound.sampled.*;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import java.io.File;
import java.io.IOException;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;


class PlayerThread extends Thread {

    private File fp;
    private boolean worktime;

    public void init(File f) {
        fp = f;
        worktime = true;
    }
    public void closePlayer() {
        worktime = false;
    }

    @Override
    public void run() {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(fp);
            SourceDataLine player = AudioSystem.getSourceDataLine(ais.getFormat());

            player.open();
            player.start();

            byte[] buf = new byte[4];
            int len;
            while (worktime && ((len = ais.read(buf)) != -1)) {

                if (ais.getFormat().getChannels() == 2) {
                    if (ais.getFormat().getSampleRate() == 16) {
                        Main.put((short) ((buf[1] << 8) | buf[0]));//左声道
                        //Main.put((short) ((buf[3] << 8) | buf[2]));//右声道
                    } else {
                        Main.put(buf[1]);//左声道
                        Main.put(buf[3]);//左声道
                        //Main.put(buf[2]);//右声道
                        //Main.put(buf[4]);//右声道
                    }
                } else {
                    if (ais.getFormat().getSampleRate() == 16) {
                        Main.put((short) ((buf[1] << 8) | buf[0]));
                        Main.put((short) ((buf[3] << 8) | buf[2]));
                    } else {
                        Main.put(buf[0]);
                        Main.put(buf[1]);
                        Main.put(buf[2]);
                        Main.put(buf[3]);
                    }
                }
                player.write(buf, 0, len);
            }
            player.close();
            ais.close();
        } catch (UnsupportedAudioFileException e) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.titleProperty().set("Warning!");
            alert.headerTextProperty().set("请拖入一个wav格式的音频文件！");
            alert.showAndWait();
        } catch (LineUnavailableException le) {
            le.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}

public class Main extends Application {

    private static PlayerThread thread = null;
    private static Deque<Short> deque = new LinkedList<Short>();

    @Override
    public void start(Stage primaryStage) throws Exception{
        Group root = new Group();
        Canvas wave = new Canvas(400,200);
        GraphicsContext gc = wave.getGraphicsContext2D();
        gc.setStroke(Color.rgb(150,200,255));
        gc.setLineWidth(1);
        DropShadow ds = new DropShadow();
        ds.setColor(Color.color(0.9f, 0.9f, 1.0f));
        gc.setEffect(ds);

        Image quitimage = new Image(getClass().getResourceAsStream("media\\quit.png"));
        Button quit = new Button();
        quit.setGraphic(new ImageView(quitimage));
        quit.setBackground(new Background(new BackgroundFill(Color.rgb(0,0,0,0),null,null)));
        quit.setLayoutX(370);
        quit.setLayoutY(5);
        quit.addEventHandler(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                primaryStage.close();
                System.exit(0);
            }
        });

        ImageView backimage = new ImageView(new Image(getClass().getResourceAsStream("media\\back.png")));

        /*
            Here is the way to drag files from other places to this app.
         */
        wave.setOnDragOver(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                if (event.getDragboard().hasFiles()) {
                    /* allow for both copying and moving, whatever user chooses */
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                }
                event.consume();
            }
        });
        wave.setOnDragDropped(new EventHandler<DragEvent>() {
            @Override
            public void handle(DragEvent event) {
                Dragboard db = event.getDragboard();
                if(db.hasFiles()) {
                    List<File> files = db.getFiles();
                    File fp = files.get(0);
                    if(thread != null) {
                        thread.closePlayer();
                        deque.clear();
                    }
                    thread = new PlayerThread();
                    thread.init(fp);
                    thread.start();
                }
            }
        });

        root.getChildren().add(backimage);
        root.getChildren().add(wave);
        root.getChildren().add(quit);

        Scene scene = new Scene(root, 400, 200);
        scene.setFill(Color.rgb(0,0,0,0));
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                gc.setEffect(null);
                gc.clearRect(0,0,400,200);
                gc.setEffect(ds);
                synchronized (deque) {
                    float heightRate = 1;
                    if(deque.size() > 1) {
                        Iterator<Short> iter = deque.iterator();
                        Short p = 0;
                        int x = 10;
                        gc.beginPath();
                        gc.moveTo(0,100);
                        gc.lineTo(x,(int)(p*heightRate)+100);
                        while(iter.hasNext()) {
                            p = iter.next();
                            gc.lineTo(++x,(int)(p*heightRate)+100);
                        }
                        gc.lineTo(++x,100);
                        gc.lineTo(400,100);
                        gc.lineTo(0,100);
                    }
                }
                gc.stroke();
            }
        }, 100 , 100);
    }

    public static void printFormat(AudioFormat format) {
        System.out.println(format.getEncoding() + " => "
                + format.getSampleRate()+" hz, "
                + format.getSampleSizeInBits() + " bit, "
                + format.getChannels() + " channel, "
                + format.getFrameRate() + " frames/second, "
                + format.getFrameSize() + " bytes/frame");
    }

    public static void put(short v) {
        synchronized (deque){
            deque.addLast(v);
            if(deque.size() > 380) {
                deque.removeFirst();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}



/*
public class SoundTest {

    public static class WaveformGraph extends JFrame {

        private Deque<Short> deque = new LinkedList<Short>();
        private Timer timer;
        private Image buffered;
        private Image showing;

        public WaveformGraph(int width, int height) {
            setSize(width, height);
            timer = new Timer();
            buffered = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
            timer.schedule(new TimerTask() {
                @Override public void run() {

                    Graphics g = buffered.getGraphics();
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.setColor(Color.BLACK);

                    g.translate(10, getHeight()/2);

                    synchronized (deque) {
                        float heightRate = 1;
                        if(deque.size() > 1) {
                            Iterator<Short> iter = deque.iterator();
                            Short p1 = iter.next();
                            Short p2 = iter.next();
                            int x1 = 0, x2 = 0;
                            while(iter.hasNext()) {
                                g.drawLine(x1, (int)(p1*heightRate), x2, (int)(p2*heightRate));

                                p1 = p2;
                                p2 = iter.next();
                                x1 = x2;
                                x2 += 1;
                            }
                        }
                    }
                    g.dispose();

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override public void run() {
                            showing = buffered;
                            repaint();
                            showing = null;
                        }
                    });
                }
            }, 100, 100);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if(buffered!=null) {
                g.drawImage(buffered, 0, 0, null);
            }
        }

        public void put(short v) {
            synchronized (deque) {
                deque.addLast(v);
                if(deque.size() > 400) {
                    deque.removeFirst();
                }
            }
        }

        public void clear() {
            deque.clear();
        }
    }

    public static void main(String[] args) throws Exception {
//  record();
        WaveformGraph waveformGraph = new WaveformGraph(500, 300);
        waveformGraph.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        waveformGraph.setVisible(true);

        AudioInputStream ais = AudioSystem.getAudioInputStream(new File("D:\\bgm.wav"));
        printFormat(ais.getFormat());


        SourceDataLine player = AudioSystem.getSourceDataLine(ais.getFormat());

        player.open();
        player.start();

        byte[] buf = new byte[4];
        int len;
        while((len=ais.read(buf))!=-1) {

            if(ais.getFormat().getChannels() == 2) {
                if(ais.getFormat().getSampleRate() == 16) {
                    waveformGraph.put((short) ((buf[1] << 8) | buf[0]));//左声道

//     waveformGraph.put((short) ((buf[3] << 8) | buf[2]));//右声道
                } else {
                    waveformGraph.put(buf[1]);//左声道
                    waveformGraph.put(buf[3]);//左声道

//     waveformGraph.put(buf[2]);//右声道
//     waveformGraph.put(buf[4]);//右声道
                }
            } else {
                if(ais.getFormat().getSampleRate() == 16) {
                    waveformGraph.put((short) ((buf[1] << 8) | buf[0]));
                    waveformGraph.put((short) ((buf[3] << 8) | buf[2]));
                } else {
                    waveformGraph.put(buf[1]);
                    waveformGraph.put(buf[2]);
                    waveformGraph.put(buf[3]);
                    //waveformGraph.put(buf[4]);
                }
            }

            player.write(buf, 0, len);
        }

        player.close();
        ais.close();
    }

    public static void printFormat(AudioFormat format) {
        System.out.println(format.getEncoding() + " => "
                + format.getSampleRate()+" hz, "
                + format.getSampleSizeInBits() + " bit, "
                + format.getChannels() + " channel, "
                + format.getFrameRate() + " frames/second, "
                + format.getFrameSize() + " bytes/frame");
    }

// public static void record() throws LineUnavailableException,
//   InterruptedException {
//  AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48000F, 16, 1, 2, 48000F, false);
//  Info recordDevInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
//
//  final TargetDataLine recordLine = (TargetDataLine) AudioSystem.getLine(recordDevInfo);
//  final SourceDataLine playLine = AudioSystem.getSourceDataLine(audioFormat);
//
//  recordLine.open(audioFormat, recordLine.getBufferSize());
//  playLine.open(audioFormat, recordLine.getBufferSize());
//
//  Thread recorder = new Thread() {
//   public void run() {
//    recordLine.start();
//    playLine.start();
//
//    FloatControl fc = (FloatControl) playLine.getControl(FloatControl.Type.MASTER_GAIN);
//    double value = 2;
//    float dB = (float) (Math.log(value == 0.0 ? 0.0001 : value) / Math.log(10.0) * 20.0);
//    fc.setValue(dB);
//
//    try {
//     AudioInputStream in = new AudioInputStream(recordLine);
//     byte[] buf = new byte[recordLine.getBufferSize()];
//     int len;
//     while((len=in.read(buf)) != -1) {
//      playLine.write(buf, 0, len);
//     }
//    } catch (IOException e) {
//     e.printStackTrace();
//    } finally {
//     recordLine.stop();
//     playLine.stop();
//    }
//   };
//  };
//  recorder.start();
//  recorder.join();
// }
}

*/