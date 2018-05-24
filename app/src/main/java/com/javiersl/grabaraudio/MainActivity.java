package com.javiersl.grabaraudio;

import android.Manifest;
import android.content.ClipData;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity
{
    private static final int REQUEST_CODE = 1;
    private static final String PERMISOS[] = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.RECORD_AUDIO};
    private Button btGrabar, btReproducir;
    private static String nombreAudio;
    private MediaRecorder mediaRecorder;
    private boolean verificacion = true, estado;
    private MediaPlayer mediaPlayer;
    private ImageView eliminarAdio;
    private RelativeLayout linear;
    private LinearLayout  ContenedorEliminar;
    private Long tiempo;
    private float firstTouchX;
    private TextView txtMensajeEliminar;


    //Nuevo Boton

    private boolean estadoBoton =false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int leer = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int grabar = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

        if (leer == PackageManager.PERMISSION_DENIED || grabar == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, PERMISOS, REQUEST_CODE);

        verificacion = true;
        estado = true;

        //Se define el nombre del archivo de audio
        nombreAudio = Environment.getExternalStorageDirectory() + "/audio.3gp";

        btGrabar = (Button)findViewById(R.id.btGrabar);
        btReproducir = (Button)findViewById(R.id.btReproducir);
        eliminarAdio = (ImageView) findViewById(R.id.eliminarGrabacion);
        linear= (RelativeLayout) findViewById(R.id.linear);
        ContenedorEliminar= (LinearLayout) findViewById(R.id.ContenedorEliminar);
        txtMensajeEliminar = (TextView) findViewById(R.id.texteliminar);



        firstTouchX= btGrabar.getX();


        btReproducir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlay(verificacion);
            }
        });

        /*Metodo para escuchar el estado del boton*/
        btGrabar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:

                        if (!estadoBoton){

                            tiempo = System.currentTimeMillis();//Contamos el tiempo qe el boton es precionado
                            btGrabar.setText("Grabando");
                            estadoBoton=true;
                            new GrabarAudio().execute();

                        }

                        break;

                    case MotionEvent.ACTION_MOVE:

                        ClipData data = ClipData.newPlainText(" ", " ");
                        View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                        view.startDrag(data, shadowBuilder, view, 0);
                        ContenedorEliminar.setVisibility(View.VISIBLE);

                        break;

                    case MotionEvent.ACTION_UP:

                        btGrabar.setX(firstTouchX);
                        ContenedorEliminar.setVisibility(View.GONE);
                        if (((Long) System.currentTimeMillis() - tiempo) > 1200) {
                            estadoBoton =false;
                            mediaRecorder.stop();
                            mediaRecorder.release();
                            mediaRecorder = null;
                            btGrabar.setText("Grabar");
                            tiempo = null;
                        } else {

                            Toast.makeText(MainActivity.this, "Deja precionado el boton", Toast.LENGTH_SHORT).show();

                        }

                        break;

                }

                return true;
            }
        });




        ContenedorEliminar.setOnDragListener(dragListener);
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        //Se libera el objeto del audio
        if(mediaRecorder != null)
        {
            mediaRecorder.release();
            mediaRecorder = null;
        }

        if(mediaPlayer != null)
        {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }


    private void EliminarGrabacion() {
        //Detiene la grabación y define a nulo por si se requiere una nueva

        mediaRecorder.stop();
        mediaRecorder.release();



        try {
             new File(nombreAudio).delete();
        } catch (Exception e) {
            Log.e("tag", e.getMessage()); }
        Toast.makeText(this, "Eliminado", Toast.LENGTH_SHORT).show();
    }


    private void comenzarReproduccion()
    {
        mediaPlayer = new MediaPlayer();
        try
        {
            mediaPlayer.setDataSource(nombreAudio);
            mediaPlayer.prepare();
            mediaPlayer.start();
        }catch (IOException e)
        {
            Toast.makeText(this, "Ha ocurrido un error en la reproducción", Toast.LENGTH_SHORT).show();
        }

        verificacion=false;
    }

    private void detenerReproduccion()
    {
        mediaPlayer.release();
        mediaPlayer = null;
        verificacion=true;
    }

    private void onPlay(boolean comenzarRep)
    {
        if(comenzarRep)
            comenzarReproduccion();
        else
            detenerReproduccion();
    }



    public class  GrabarAudio extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            mediaRecorder = new MediaRecorder();

            //Para obtener el audio desde el microfono
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

            //Define el formato del audio
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

            //Guarda el archivo
            mediaRecorder.setOutputFile(nombreAudio);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            try
            {
                //Prepara el audio
                mediaRecorder.prepare();
            }catch (IOException e)
            {
                //Toast.makeText(this, "No se grabará correctamente", Toast.LENGTH_SHORT).show();
            }

            //Comienza la grabación
            mediaRecorder.start();

            return null;
        }
    }



    View.OnDragListener dragListener = new View.OnDragListener() {
        @Override
        public boolean onDrag(View view, DragEvent dragEvent) {

            int dragEventAction = dragEvent.getAction();
            final View viewDrag = (View) dragEvent.getLocalState();

            switch (dragEventAction) {

                case DragEvent.ACTION_DRAG_ENTERED:

                    //ACCIÓN ARRASTRE ENTRADA
                    eliminarAdio.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.colorEliminar));
                    txtMensajeEliminar.setText("Suelta para eliminar");
                   // ContenedorEliminar.setVisibility(View.VISIBLE);
                    //Toast.makeText(MainActivity.this, "ACTION_DRAG_ENTERED", Toast.LENGTH_SHORT).show();
                    break;

                case DragEvent.ACTION_DRAG_EXITED:
                    //Cuando sale del contenedor
                    txtMensajeEliminar.setText("Arraste aqui para eliminar");
                    eliminarAdio.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.colorNegro));
                  //  ContenedorEliminar.setVisibility(View.GONE);
                    break;

                case DragEvent.ACTION_DROP:

                    if (viewDrag.getId() == R.id.btGrabar) {
                        ContenedorEliminar.setVisibility(View.GONE);
                        eliminarAdio.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.colorNegro));
                        EliminarGrabacion();
                        EsconderBote();
                    }

                    break;

                default:
                     //    Toast.makeText(MainActivity.this, "No entro en ningun case", Toast.LENGTH_SHORT).show();


                    break;
            }

            return true;
        }
    };


    private void EsconderBote() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Ejecutar despues de 2s = 2000ms
                ContenedorEliminar.setVisibility(View.GONE);
            }
        }, 2000);


    }

}
