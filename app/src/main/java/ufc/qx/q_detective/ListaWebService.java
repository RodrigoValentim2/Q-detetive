package ufc.qx.q_detective;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.io.File;
import java.util.List;
import java.util.Map;

import ufc.qx.q_detective.dao.DenunciaDAO;
import ufc.qx.q_detective.dominio.Denuncia;

/**
 * Created by mel on 07/12/17.
 */

public class ListaWebService extends Activity {

    private List<Map<String, Object>> listMapDenuncias;
    private DenunciaDAO denunciaDAO;
    private SimpleAdapter adapter;
    private ListView listView;
    private ProgressDialog load;
    private boolean permisaoInternet = false;
    private final String url = "http://35.193.98.124/QDetective/rest/";

    @Override
    protected void onResume() {
        super.onResume();
        this.carregarDados();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_web_service_layout);
        denunciaDAO = new DenunciaDAO(this);
        carregarDados();
        this.iniciarDownload();
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

        listView = findViewById(R.id.listaWebServer);
        listView.setAdapter(adapter);
    }

    public void iniciarDownload() {
        getPermissaoDaInternet();
        if (permisaoInternet) {
            DownloadDenuncias download = new DownloadDenuncias();
            download.execute();
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

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }


    private File getDiretorio(String nomeArquivo) {
        File diretorio;
        if (nomeArquivo.contains("/")) {
            int beginIndex = nomeArquivo.lastIndexOf("/") + 1;
            nomeArquivo = nomeArquivo.substring(beginIndex);
        }
        if(nomeArquivo.contains(".jpg")){
            diretorio = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        } else{
            diretorio = this.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        }
        File pathDaImagem = new File(diretorio, nomeArquivo);
        return pathDaImagem;
    }

    private class DownloadDenuncias extends AsyncTask<Long, Void, WebServiceUtils> {
        @Override
        protected void onPreExecute() {
            load = ProgressDialog.show(ListaWebService.this, "Por favor Aguarde ...", "Recuperando Informações do Servidor...");
        }
        @Override
        protected WebServiceUtils doInBackground(Long... ids) {
            WebServiceUtils webService = new WebServiceUtils();
            String id = (ids != null && ids.length == 1) ? ids[0].toString() : "";
            List<Denuncia> denuncias = webService.getListaDenunciasJson(url, "denuncias", id);

            for (Denuncia denuncia : denuncias) {
                String path = getDiretorio(denuncia.getUriMidia()).getPath();
                webService.downloadImagemBase64(url + "arquivos", path, denuncia.getId());
                denuncia.setUriMidia(path);
            }
            return webService;
        }

        @Override
        protected void onPostExecute(WebServiceUtils webService) {
            for (Denuncia denuncia : webService.getDenuncias()) {
                Denuncia d = denunciaDAO.buscarDenunciaPorId(denuncia.getId());
                if (d != null) {
                    denunciaDAO.atualizarDenuncia(denuncia);
                } else {
                    denunciaDAO.inserirDenuncia(denuncia);
                }
            }
            load.dismiss();
            Toast.makeText(getApplicationContext(), webService.getRespostaServidor(), Toast.LENGTH_LONG).show();
            carregarDados();
        }
    }
}
