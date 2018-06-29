package ufc.qx.q_detective;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.io.File;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import ufc.qx.q_detective.dao.DenunciaDAO;
import ufc.qx.q_detective.dominio.Denuncia;

/**
 * Created by mel on 07/12/17.
 */

public class ListaMinhasDenuncias extends Activity implements AdapterView.OnItemClickListener, SimpleAdapter.ViewBinder,  MenuDialogFragmentList.NotificarEscutadorDoDialog {

    private List<Map<String, Object>> listMapDenuncias;
    private DenunciaDAO denunciaDAO;
    private SimpleAdapter adapter;
    private ListView listView;
    private ProgressDialog load;
    private Denuncia denuncia;
    private final String url = "http://35.193.98.124/QDetective/rest/";
    private Uri uri;
    private Integer id = null;
    private boolean permisaoInternet = false;
//    private ProgressDialog load;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_denuncias_layout);
        denunciaDAO = new DenunciaDAO(this);
        carregarDados();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.carregarDados();
    }

    @Override
    protected void onDestroy() {
        this.denunciaDAO.close();
        super.onDestroy();
    }


    public void carregarDados() {
        listMapDenuncias = denunciaDAO.listarDenuncias();


        String[] key = {
                DatabaseHelper.Denuncia.CATEGORIA,
                DatabaseHelper.Denuncia.DESCRICAO,
                DatabaseHelper.Denuncia.USUARIO,
                DatabaseHelper.Denuncia.DATA
        };

        int[] value = {R.id.categoria, R.id.descricao,R.id.usuario, R.id.data};

        adapter = new SimpleAdapter(this, listMapDenuncias, R.layout.denuncias_layout_list, key, value);

        listView = findViewById(R.id.listaDenuncias);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener( this);

    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        MenuDialogFragmentList fragmento = new MenuDialogFragmentList();

        Map<String, Object> item = listMapDenuncias.get(i);
        Bundle bundle = new Bundle();
        bundle.putInt("id", (int) item.get(DatabaseHelper.Denuncia._ID));
        fragmento.setArguments(bundle);
        fragmento.show(this.getFragmentManager(), "confirma");

    }



    @Override
    public boolean setViewValue(View view, Object o, String s) {
        return false;
    }

    @Override
    public void onDialogExcluiClick(int id) {
        Denuncia denuncia =denunciaDAO.buscarDenunciaPorId(id);
        String path = denuncia.getUriMidia();
        if (denunciaDAO.removerDenuncia(id)) {
            File file = new File(path);
            file.delete();
            carregarDados();
        }
    }

    @Override
    public void onDialogEditarClick(int id) {
        Intent intent = new Intent(this, DenunciaActivity.class);
        intent.putExtra("id", id);
        Log.d("tg", "onDialogEditarClick: teste");
        startActivity(intent);
    }

    @Override
    public void onDialogEnviarParaWebServiceClick(int id) {
        getPermissaoDaInternet();
        if (permisaoInternet) {
            denuncia = denunciaDAO.buscarDenunciaPorId(id);
            UploadDenuncia uploadDenuncia = new UploadDenuncia();
            uploadDenuncia.execute(denuncia);
            this.id = id;
        }

    }

    @Override
    public void onDialogEnviarDetalhesClick(int id) {
        Intent intent = new Intent(ListaMinhasDenuncias.this, DetalhesActivity.class);
        intent.putExtra("id", id);
        Log.d("tg", "onDialogEnviarDetalhesClick: teste");
        startActivity(intent);

    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    class UploadDenuncia extends AsyncTask<Denuncia, Void, WebServiceUtils> {

        @Override
        protected void onPreExecute() {
            load = ProgressDialog.show(ListaMinhasDenuncias.this, "Por favor Aguarde ...", "Recuperando Informações do Servidor...");
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected WebServiceUtils doInBackground(Denuncia... denuncias) {
            WebServiceUtils webService = new WebServiceUtils();
            Denuncia denuncia = denuncias[0];
            String urlDados = url + "denuncias";
            Log.d("GDE", "doInBackground: "+denuncia.getUriMidia());
            if (webService.sendDenunciaJson(urlDados, denuncia)) {
                urlDados = url + "arquivos/postFotoBase64";
                webService.uploadImagemBase64(urlDados, new File(denuncia.getUriMidia()));
                if(id != null){
                    String path = denuncia.getUriMidia();
                    if (denunciaDAO.removerDenuncia(id)){
                        File file = new File(path);
                        file.delete();
                        finish();
                    }
                }


            }
            return webService;
        }
        @Override
        protected void onPostExecute(WebServiceUtils webService) {
            Toast.makeText(getApplicationContext(),
                    webService.getRespostaServidor(),
                    Toast.LENGTH_LONG).show();
            load.dismiss();
        }
    }
    private void getPermissaoDaInternet() {
        boolean internet = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED;
        boolean redeStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED;
        if (internet && redeStatus) {
            if (isOnline()) {
                permisaoInternet = true;
                return;
            } else {
                permisaoInternet = false;
                Toast.makeText(this, "Sem conexão de Internet.", Toast.LENGTH_LONG).show();
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.INTERNET,
                            Manifest.permission.ACCESS_NETWORK_STATE},
                    1);
        }
 }
}
