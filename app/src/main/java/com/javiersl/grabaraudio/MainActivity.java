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
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {

    //inicializacion de varibles a utilizar

    private static final int REQUEST_CODE = 1;//Respuesta de permisos
    private static final String PERMISOS[] = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};//Permisos especiales requerido
    private Button btnGrabarAudio, btnReproducirAudio;//Botones grabar reprodicir
    private static String nombreAudio;// Esta variabre contendra el nombre y la direccion dende se guardara el audio
    private MediaRecorder mediaRecorder; //variable para guardar aduio
    private boolean verificacion = true;//Verificar reproducir o detener la reproduccion
    private MediaPlayer mediaPlayer;// Para reproducir el archivo creado
    private ImageView eliminarAudio; //Icono de eliminar
    private LinearLayout layoutEliminar; // El contenedor del icono eliminar
    private Long tiempo;//varible para medir el tiempo que es precioando el boton de grabar
    private float primerToqueX;// Variable para guardar la posicion en X del boton guardar
    private TextView txtMensajeEliminar;// TextView oara mostrar indicaciones al usuario
    private boolean estadoBoton = false;// Verificar si el boton grabar fue precionado
    ClipData data = ClipData.newPlainText(" ", " ");

    /*Metodo onCreate*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*guardamoe le valor de los permisos necesarios*/
        int leer = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int grabar = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

        /*Los comparamos para poder continuar con la ejeuccion de la app*/
        if (leer == PackageManager.PERMISSION_DENIED || grabar == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, PERMISOS, REQUEST_CODE);


        /*Añadimos el nombre de nuestro archivo (en este caso siempre sera el mismo) "audio.3gp" y e indicamos se de guardara
        * en la memorria del telefono*/
        nombreAudio = Environment.getExternalStorageDirectory() + "/audio.3gp";

        /*Asignacion de controles*/
        btnGrabarAudio = (Button) findViewById(R.id.btGrabar);
        btnReproducirAudio = (Button) findViewById(R.id.btReproducir);
        eliminarAudio = (ImageView) findViewById(R.id.eliminarGrabacion);
        layoutEliminar = (LinearLayout) findViewById(R.id.ContenedorEliminar);
        txtMensajeEliminar = (TextView) findViewById(R.id.texteliminar);

        /*Obtenemos las coordenadas den X del boton grabar */
        primerToqueX = btnGrabarAudio.getX();

        /*Escuchar cuando el boton reproducir sea precionado*/
        btnReproducirAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlay(verificacion);
            }
        });

        /*Metodo para escuchar el estado del boton*/
        btnGrabarAudio.setOnTouchListener(this);
        layoutEliminar.setOnDragListener(dragListener);//Hacemos un meto al metodo dragListener
    }


    /*Metodo para eliminar el archivo de audio ANTES DE ENVIAR*/
    private void EliminarGrabacion() {

        mediaRecorder.stop();//Detenemos la gracion
        mediaRecorder.release();//Guardamos el archivo

        try {
            new File(nombreAudio).delete(); //Cremos un archivo nuevo, nombreAudio contiene el nombre y la hubicacion del archivo que vamos a eliminar
            estadoBoton = false;//cambiamos e
            Toast.makeText(this, "Eliminado", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {//En caso de error
            Log.e("tag", e.getMessage());//Imprimimos la Exception optenida
        }

    }

    //  ESTOS METODOS SOLOS OCUPARE EN ESTE EJERCICIO
    private void comenzarReproduccion() {

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(nombreAudio);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            Toast.makeText(this, "Ha ocurrido un error en la reproducción", Toast.LENGTH_SHORT).show();
        }
        verificacion = false;
    }

    private void detenerReproduccion() {
        mediaPlayer.release();
        mediaPlayer = null;
        verificacion = true;
    }


    private void onPlay(boolean comenzarRep) {
        if (comenzarRep)
            comenzarReproduccion();
        else
            detenerReproduccion();
    }
//////////////////////////////////////////////////////////////////////
    @Override
    public boolean onTouch(View view, MotionEvent event) {
    /*switch para realizar una accion dependiendo del caso
                 *extraemos la accion realizada con event.getAction()
                 */

        switch (event.getAction()) {

           /*El caso ACTION_DOWN se identifica duando el (en este caso el boton) es presionado*/
            case MotionEvent.ACTION_DOWN:
                /*
                *Verificamos el estado del boton si es precioando entra en el metodo
                */
                if (!estadoBoton) {
                    btnGrabarAudio.setX(primerToqueX);
                    tiempo = System.currentTimeMillis();//Contamos el tiempo qe el boton es precionado
                    btnGrabarAudio.setText("Grabando");// Se envia el texto al boton "Grabando"
                    estadoBoton = true;//Cambiamos el estado del boton
                    new GrabarAudio().execute();//Ejecutamoe el hijo encargado de la grabacion
               }

                break;
                        /*Segundo caso ACTION_MOVE se identifica cuando el boton grabar es deslizado*/
            case MotionEvent.ACTION_MOVE:
                    /*Auno no se de como funciona esto pero esta parte es la encargada de levantar el boton
                     *y seguir por donde el usuario deslice por la pantalla
                     */
                    View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                    view.startDrag(data, shadowBuilder, view, 0);
                    layoutEliminar.setVisibility(View.VISIBLE);// Hacemos visible el icono de eliminar

                break;
                        /*El caso ACTION_UP  se identifica cuando el boton deja de ser precioando, pude ocurrie en cualquier momento
                        * aunque el usuario mueva el boton de un lugar a otro
                        * */
            case MotionEvent.ACTION_UP:

                /*Enviamos el valor obtenido en el primer toque para que el boton regrese a su lugar*/
                btnGrabarAudio.setX(primerToqueX);
                /*Ocultamos el icono eliminar*/
                layoutEliminar.setVisibility(View.GONE);
                /*Verificamos el tiempo que el boton fue precionado, esto para evitar que un error en tipo de ejecuacion
                 *si el boton es precionado mas de un segundo (1200 ms) entra.
                 */
                if (((Long) System.currentTimeMillis() - tiempo) > 1200) {
                    estadoBoton = false;//Cambimos el estado dell boton
                    mediaRecorder.stop();//Detenemos la grabacion
                    mediaRecorder.release();//Guardamos el archivo
                    mediaRecorder = null;//Reiniciamos la variable para proximas grabaciones
                    btnGrabarAudio.setText("Grabar");//Enviamos el texto inicial al boton grabar
                    tiempo = null;//Reiniciamos el contador del tiempo

                } else {
                            /*Si el boton es precionado menos de un segundo enviamoe el mensaje al usuario*/
                    Toast.makeText(MainActivity.this, "Manten precionado para grabar", Toast.LENGTH_SHORT).show();
                }

                break;

        }

        return true;//Debolvemos un true para que entre contantemente en este metodo
    }


    /*
    *   Hilo encargado de grabar el audio
    */

    public class GrabarAudio extends AsyncTask<Void, Void, Void> {

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

            try {
                //Prepara el audio
                mediaRecorder.prepare();
            } catch (IOException e) {
                //Toast.makeText(this, "No se grabará correctamente", Toast.LENGTH_SHORT).show();
            }

            //Comienza la grabación
            mediaRecorder.start();

            return null;
        }
    }


    /*
    * Metodo encargado de escuchar las interaciones con el Contenedor Eliminar
    */

    View.OnDragListener dragListener = new View.OnDragListener() {
        @Override
        public boolean onDrag(View view, DragEvent dragEvent) {

            int dragEventAction = dragEvent.getAction();//Obtenemos y guardamos la accion que se realiza
            final View viewDrag = (View) dragEvent.getLocalState();//Obtenemos y guardamos que objeto es que interactuo con el contenedor eliminar

            /*switch comparamos el tipo de accion que se realiza*/
            switch (dragEventAction) {

                /*Caso ACTION_DRAG_ENTERED se detecta cuando el objeto entra en el contenedor eliminar*/
                case DragEvent.ACTION_DRAG_ENTERED:

                    //ACCIÓN ARRASTRE ENTRADA
                    eliminarAudio.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.colorEliminar));//Cambiamos el color del icono
                    txtMensajeEliminar.setText("Suelta para eliminar");//Cambiamos el texto
                    btnGrabarAudio.setText("Grabar");//Enviamos el texto inicial al boton grabar


                    break;
                /*
                *Caso ACTION_DRAG_EXITED se detecta cuando el objeto sale del rango del contenedor
                */
                case DragEvent.ACTION_DRAG_EXITED:
                    //Cuando sale del contenedor
                    txtMensajeEliminar.setText("Arraste aqui para eliminar");//Enviamos el texto original
                    eliminarAudio.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.colorNegro));//Cambiamos en el color del icono al inicial

                    break;

                /* Caso ACTION_DROP  seidentifica cuanod el objeto es soltado entro del rango del icono eliminar*/
                case DragEvent.ACTION_DROP:

                    if (viewDrag.getId() == R.id.btGrabar) {
                        layoutEliminar.setVisibility(View.GONE);
                        eliminarAudio.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.colorNegro));
                        EliminarGrabacion();
                        ocultarIconoEliminar();
                    }
                    break;
            }
            return true;
        }
    };

    /*Metodo encargado de ocultar el icono eliminar*/
    private void ocultarIconoEliminar() {
        final Handler handler = new Handler();//Objetopara "pausar" la ejecucion del codigo
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Ejecutar despues de 2s = 2000ms
                layoutEliminar.setVisibility(View.GONE);//Ocultamos el contenedor eliminar
            }
        }, 2000);//Tiempo establecido para reanudar el codigo para reanudar la ejecucion del codigo
    }

}
