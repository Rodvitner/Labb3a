package simon.sensortest;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    TextView lagpass, hogpass, vinkel, varning,stdav;
    double lagfiltratX, lagfiltratY, lagfiltratZ, hogfiltratX, hogfiltratY, hogfiltratZ, xSkak;
    double[] xHistorik,yHistorik,zHistorik;
    long skakningBorjade=0;
    private final int FILTERFAKTOR = 95; // hur m}nga procent av nya v{rdet som {r gamla v{rdet.
    private final int HISTORIKLANGD = 10;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        xHistorik = new double[HISTORIKLANGD];
        yHistorik = new double[HISTORIKLANGD];
        zHistorik = new double[HISTORIKLANGD];

        lagpass = (TextView) findViewById(R.id.lagpass);
        hogpass = (TextView) findViewById(R.id.hogpass);
        vinkel = (TextView) findViewById(R.id.vinkel);
        varning = (TextView)findViewById(R.id.varning);
        stdav = (TextView)findViewById(R.id.stdev);

        SensorManager sensorChef = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorChef.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (accelerometer != null) {
            toastText("Hittade en acc!");
            sensorChef.registerListener(new accLyssnare(), accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

    }

    public void toastText(String text) {
        Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    public double angleInDegrees(double vRad) {
        return (vRad * 180 / Math.PI);
    }

    public double stdDev(double[] array) {
        double mean=0;
        for (double d : array) mean+=d;
        mean/=array.length;
        double varTotal=0;
        for (double d : array) varTotal+=(Math.pow(d-mean,2));
        varTotal/=(array.length-1);
        return Math.sqrt(varTotal);
    }

    private class accLyssnare implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            double mattX = sensorEvent.values[0];
            double mattY = sensorEvent.values[1];
            double mattZ = sensorEvent.values[2];

            for (int i=HISTORIKLANGD-1;i>0;i--) {
                xHistorik[i-1]=xHistorik[i];
                yHistorik[i-1]=yHistorik[i];
                zHistorik[i-1]=zHistorik[i];
            }
            xHistorik[HISTORIKLANGD-1]=mattX;
            yHistorik[HISTORIKLANGD-1]=mattY;
            zHistorik[HISTORIKLANGD-1]=mattZ;

            xSkak= Math.round(FILTERFAKTOR*xSkak+(100-FILTERFAKTOR)*stdDev(xHistorik))/100d;
//            xSkak = Math.round(stdDev(xHistorik)*100)/100d;

            stdav.setText(""+xSkak);
            if (xSkak>0.5) {
                if (skakningBorjade>0) {
                    if (sensorEvent.timestamp-skakningBorjade>1000000000) {
                        vinkel.setTextColor(Color.RED);
                    }
                }else {
                    skakningBorjade=sensorEvent.timestamp;
                }
            }else {
                skakningBorjade=0;
                vinkel.setTextColor(Color.BLACK);
            }

            if (lagfiltratX + lagfiltratY + lagfiltratZ == 0) {
                lagfiltratX = mattX;
                lagfiltratY = mattY;
                lagfiltratZ = mattZ;
            } else {
                lagfiltratX = Math.round(FILTERFAKTOR * lagfiltratX + (100 - FILTERFAKTOR) * mattX) / 100d;
                lagfiltratY = Math.round(FILTERFAKTOR * lagfiltratY + (100 - FILTERFAKTOR) * mattY) / 100d;
                lagfiltratZ = Math.round(FILTERFAKTOR * lagfiltratZ + (100 - FILTERFAKTOR) * mattZ) / 100d;
            }
            if (hogfiltratX + hogfiltratY + hogfiltratZ == 0) {
                hogfiltratX = mattX;
                hogfiltratY = mattY;
                hogfiltratZ = mattZ;
            } else {
                hogfiltratX = Math.round((100- FILTERFAKTOR)*hogfiltratX+ FILTERFAKTOR *mattX)/100d;
                hogfiltratY = Math.round((100- FILTERFAKTOR)*hogfiltratY+ FILTERFAKTOR *mattY)/100d;
                hogfiltratZ = Math.round((100- FILTERFAKTOR)*hogfiltratZ+ FILTERFAKTOR *mattZ)/100d;
            }

            vinkel.setText(Math.round(angleInDegrees(Math.atan(lagfiltratX / lagfiltratY))) + "°");
            lagpass.setText(lagfiltratX + " " + lagfiltratY + " " + lagfiltratZ);
            hogpass.setText(hogfiltratX + " " + hogfiltratY + " " + hogfiltratZ);

            if (Math.abs(lagfiltratZ) > Math.abs(lagfiltratX * 5) && Math.abs(lagfiltratZ) > Math.abs(lagfiltratY * 5))
                varning.setText("Varning, telefonen ligger för platt för att mätdata ska vara pålitligt.");
            else varning.setText("Inga varningar.");
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    }
}
