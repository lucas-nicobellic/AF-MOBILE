package com.example.af_final;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    Button btnObter;
    TextView edtLat;
    TextView edtlong;
    TextView txtStatus;
    Button btnBuscar;
    RecyclerView rvlocais;
    Button btnSalvar;
    ArrayList<String> listaLocais = new ArrayList<>();

    LugarAdapter adapter;
    Spinner categorias;

    private double latAtual = 0;
    private double lngAtual = 0;


    private static final int LOCATION_PERMISSION_REQUEST = 1;
    private FusedLocationProviderClient fusedLocationClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnSalvar = findViewById(R.id.btnSalvar);


        btnObter = findViewById(R.id.btnObter);
        edtLat = findViewById(R.id.edtLat);
        edtlong = findViewById(R.id.edtLong);
        btnBuscar = findViewById(R.id.btnBuscar);
        categorias = findViewById(R.id.categorias);
        txtStatus = findViewById(R.id.txtStatus);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        // nesta parte ele vai pedir a permissão ao usuario para liberar o acesso ao gps do celular
        btnObter.setOnClickListener(v -> {

            if (ContextCompat.checkSelfPermission(
                    MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST
                );

            } else {

                obterLocalizacao();

            }

        });

        btnSalvar.setOnClickListener(v -> salvarLocais());
        rvlocais = findViewById(R.id.rvlocais);

        adapter = new LugarAdapter(listaLocais);

        rvlocais.setLayoutManager(
                new LinearLayoutManager(this)
        );

        rvlocais.setAdapter(adapter);

        listaLocais.add("Aguardando...");


        adapter.notifyDataSetChanged();



        // no onCreate, troca buscarLocaisTeste() por:
        btnBuscar.setOnClickListener(v -> {
            try {
                buscarLocais();
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        });

    }
    private void obterLocalizacao() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                latAtual = location.getLatitude();
                lngAtual = location.getLongitude();

                edtLat.setText("Latitude: " + latAtual);
                edtlong.setText("Longitude: " + lngAtual);
            }
        });
    }

        //conexão com a api
        private void buscarLocais() throws UnsupportedEncodingException {
            if (latAtual == 0 && lngAtual == 0) {
                Toast.makeText(this, "Obtenha a localização primeiro!", Toast.LENGTH_SHORT).show();


                return;
            }
            runOnUiThread(() -> {
                txtStatus.setText("Procurando locais...");
                txtStatus.setVisibility(View.VISIBLE);
            });

            // pega a categoria selecionada no spinner
            String categoriaSpinner = categorias.getSelectedItem().toString().toLowerCase();

            // mapeia o texto do spinner para o valor da Overpass API
            String amenity;
            switch (categoriaSpinner) {
                case "farmácia":  amenity = "pharmacy";    break;
                case "hospital":  amenity = "hospital";    break;
                case "escola":    amenity = "school";      break;
                case "restaurante": amenity = "restaurant"; break;
                case "praça":     amenity = "park";        break;
                case "mercado":   amenity = "supermarket"; break;
                default:          amenity = "restaurant";  break;
            }

            String query = "[out:json];node[amenity=" + amenity + "](around:2000," + latAtual + "," + lngAtual + ");out;";
            String urlStr = "https://overpass-api.de/api/interpreter?data=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8.toString());

            new Thread(() -> {
                try {
                    URL url = new URL(urlStr);
                    HttpURLConnection conexao = (HttpURLConnection) url.openConnection();

                    BufferedReader leitor = new BufferedReader(
                            new InputStreamReader(conexao.getInputStream())
                    );

                    StringBuilder resposta = new StringBuilder();
                    String linha;
                    while ((linha = leitor.readLine()) != null) {
                        resposta.append(linha);
                    }

                    JSONObject json = new JSONObject(resposta.toString());
                    JSONArray elementos = json.getJSONArray("elements");

                    listaLocais.clear();

                    if (elementos.length() == 0) {
                        listaLocais.add("Nenhum local encontrado.");
                    }

                    for (int i = 0; i < elementos.length(); i++) {
                        JSONObject local = elementos.getJSONObject(i);
                        if (local.has("tags")) {
                            JSONObject tags = local.getJSONObject("tags");
                            String nome = tags.optString("name", "(sem nome)");
                            double lat = local.optDouble("lat", 0);
                            double lon = local.optDouble("lon", 0);
                            listaLocais.add(nome + "\n" + lat + ", " + lon);
                        }
                    }

                    runOnUiThread(() -> {
                        adapter.notifyDataSetChanged();
                        txtStatus.setVisibility(View.GONE);
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        txtStatus.setVisibility(View.GONE);
                        Toast.makeText(this, "Erro: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        }

    private void salvarLocais() {
        if (listaLocais.isEmpty() || listaLocais.get(0).equals("Aguardando...")) {
            Toast.makeText(this, "Nenhum local para salvar!", Toast.LENGTH_SHORT).show();
            return;
        }

        com.google.firebase.firestore.FirebaseFirestore db =
                com.google.firebase.firestore.FirebaseFirestore.getInstance();

        String categoria = categorias.getSelectedItem().toString();
        long timestamp = System.currentTimeMillis();

        for (String local : listaLocais) {
            java.util.HashMap<String, Object> dados = new java.util.HashMap<>();
            dados.put("nome", local);
            dados.put("categoria", categoria);
            dados.put("lat", latAtual);
            dados.put("lng", lngAtual);


            db.collection("locais")
                    .add(dados)
                    .addOnSuccessListener(ref ->
                            Toast.makeText(this, "Salvo!", Toast.LENGTH_SHORT).show()
                    )
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Falha: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
        }
    }

    }
