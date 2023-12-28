package com.lojix.ruchidfarmamonitor;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.firebase.BuildConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {
    public static String url = "https://ruchid-farma-default-rtdb.firebaseio.com";
    public static String producao = "/Monitor_entregas";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        atualizar();

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            TextView tvTitulo = findViewById(R.id.tvTitulo);
            tvTitulo.setText("Ruchid Farma Entregas "+ version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        atualizar();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        atualizar();
    }

    private void atualizar(){
        Firebase.setAndroidContext(MainActivity.this);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        Firebase banco = new Firebase(url+producao);
        banco.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<ItemPost> list = new ArrayList<ItemPost>();
                String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                for (DataSnapshot post : dataSnapshot.child(currentDate).getChildren()){
                    String momeCliente = post.child("NomeCliente").getValue(String.class);
                    String endereco = post.child("Endereco").getValue(String.class);
                    String data_hora = post.child("Data_hora").getValue(String.class);
                    String cidade = post.child("Cidade").getValue(String.class);
                    String status = post.child("Status").getValue(String.class);
                    String nF = post.child("NF").getValue(String.class);
                    int ordem = 5;
                    if (status.equals("Aberto")) ordem = 1;
                    if (status.equals("Saida para entrega")) ordem = 2;
                    if (status.equals("Concluido")) ordem = 3;
                    if (status.equals("Cancelado")) ordem = 4;
                    list.add(new ItemPost(momeCliente,endereco,data_hora,cidade,status,nF,currentDate+"/"+post.getKey(),ordem));
                }
                Collections.sort(list, new Comparator<ItemPost>() {
                    @Override
                    public int compare(final ItemPost object1, final ItemPost object2) {
                        return object2.getOrdem() - object1.getOrdem();
                    }
                });
                atualist(list);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Toast.makeText(MainActivity.this,firebaseError.getMessage(),Toast.LENGTH_LONG).show();
            }
        });
    }
    private String converterDatas(String old_date){
        final String OLD_FORMAT = "yyyyMMdd_HHmmss";
        final String NEW_FORMAT = "dd MMMM, yy 'às' HH:mm";

// August 12, 2010
        String oldDateString = old_date;
        String newDateString;

        SimpleDateFormat sdf = new SimpleDateFormat(OLD_FORMAT);
        Date d = null;
        try {
            d = sdf.parse(oldDateString);
            sdf.applyPattern(NEW_FORMAT);
            newDateString = sdf.format(d);
            return newDateString;
        } catch (ParseException e) {
            e.printStackTrace();
            return "-";
        }
    }

    public void atualist (ArrayList <ItemPost> list) {
        RecyclerView rv = (RecyclerView) findViewById(R.id.rvLista);
        ListaAdapter mAdapter = new ListaAdapter(this, list);
        rv.setAdapter(mAdapter);
        rv.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false) );
        rv.scrollToPosition(mAdapter.getItemCount() - 1);//rola rv até o ultimo exibindo-o



        TextView tvResumo = findViewById(R.id.tvResumo);
        int contAbertos = 0;
        int contConcluidos = 0;
        for (ItemPost post : list){
            if (post.getStatus().equals("Aberto")) {
                contAbertos++;
            }
            if (post.getStatus().equals("Concluido")){
                contConcluidos++;
            }
        }
        tvResumo.setText("Total de "+list.size()+" encontrados\nAbertos: "+contAbertos+"\nConcluidos: "+contConcluidos);
    }

//======================================= A D A P T E R =================================================================

    public class ListaAdapter extends RecyclerView.Adapter<HolderUsados> {
        private ArrayList<ItemPost> list;
        private LayoutInflater inflater;
        private Context c;


        public ListaAdapter(Context context, ArrayList<ItemPost> list) {
            this.list = list;
            this.c = context;
            this.inflater = LayoutInflater.from(context);

        }

        @Override
        public HolderUsados onCreateViewHolder(ViewGroup parent, int viewType) {
                    View layoutView1 = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_principal, parent,false);
                    HolderUsados convertView1 = new HolderUsados(layoutView1);
                    return convertView1;

        }
        @Override
        public void onBindViewHolder(final HolderUsados holder, int position) {
            final int pos = position;
            final ItemPost post = getItem(position);
            holder.tvItem.setText(post.getData_hora() +" - "+post.getCidade().toUpperCase());
            holder.tvItemDescri.setText("Cliente: "+post.getNomeCliente()+"\n" +
                    "Endereço: "+post.getEndereco()+"\n"+
                    "NF: "+post.getNF());

            List<String> listsp = new ArrayList<String>();
            listsp.add("Aberto");
            listsp.add("Saida para entrega");
            listsp.add("Concluido");
            listsp.add("Cancelado");

            holder.spStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    Firebase fb = new Firebase(url+producao);
                    String novostatus = holder.spStatus.getSelectedItem().toString();
                    fb.child(post.local_objeto).child("Status").setValue(novostatus);
                   // post.setStatus(novostatus);

                 // atualizarStatus(pos);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
            ArrayAdapter<String> dataAdapter1 = new ArrayAdapter<String>(c, android.R.layout.simple_spinner_item, listsp);
            dataAdapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            holder.spStatus.setAdapter(dataAdapter1);
            try {
                holder.spStatus.setSelection(listsp.indexOf(post.getStatus()));
            } catch (Exception e){

            }
        }
        public void atualizarStatus(int pos){

        }

        public ItemPost getItem(int position) {
            return  list.get(position);
        }
        @Override
        public int getItemCount() {
            return this.list.size();
        }

        private String converterDatas(String old_date){
            final String OLD_FORMAT = "yyyyMMdd_HHmmss";
            final String NEW_FORMAT = "dd MMMM, yy 'às' HH:mm";

// August 12, 2010
            String oldDateString = old_date;
            String newDateString;

            SimpleDateFormat sdf = new SimpleDateFormat(OLD_FORMAT);
            Date d = null;
            try {
                d = sdf.parse(oldDateString);
                sdf.applyPattern(NEW_FORMAT);
                newDateString = sdf.format(d);
                return newDateString;
            } catch (ParseException e) {
                e.printStackTrace();
                return "-";
            }
        }
    }

//==================================    H O L D E R S =================================================================

    public class HolderUsados extends RecyclerView.ViewHolder {
        public TextView tvItem;
        public TextView tvItemDescri;
        public Spinner spStatus;
        public HolderUsados(View itemView) {
            super(itemView);
            tvItem = (TextView) itemView.findViewById(R.id.tvItem);
            tvItemDescri = (TextView) itemView.findViewById(R.id.tvItemDescri);
            spStatus = (Spinner)  itemView.findViewById(R.id.spinner);


        }
    }

    //================================== P O S T  Listiview ================================================================================

    public class ItemPost {
        private String NomeCliente;
        private String Endereco;
        private String Data_hora;
        private String Cidade;
        private String Status;
        private String NF;

        private String local_objeto;

        private int ordem;

        public ItemPost(String nomeCliente, String endereco, String data_hora, String cidade, String status, String NF, String local_objeto,int ordem) {
            NomeCliente = nomeCliente;
            Endereco = endereco;
            Data_hora = data_hora;
            Cidade = cidade;
            Status = status;
            this.NF = NF;
            this.local_objeto = local_objeto;
            this.ordem = ordem;
        }

        public int getOrdem() {
            return ordem;
        }

        public void setOrdem(int ordem) {
            this.ordem = ordem;
        }

        public String getLocal_objeto() {
            return local_objeto;
        }

        public void setLocal_objeto(String local_objeto) {
            this.local_objeto = local_objeto;
        }

        public String getNomeCliente() {
            return NomeCliente;
        }

        public void setNomeCliente(String nomeCliente) {
            NomeCliente = nomeCliente;
        }

        public String getEndereco() {
            return Endereco;
        }

        public void setEndereco(String endereco) {
            Endereco = endereco;
        }

        public String getData_hora() {
            return Data_hora;
        }

        public void setData_hora(String data_hora) {
            Data_hora = data_hora;
        }

        public String getCidade() {
            return Cidade;
        }

        public void setCidade(String cidade) {
            Cidade = cidade;
        }

        public String getStatus() {
            return Status;
        }

        public void setStatus(String status) {
            Status = status;
        }

        public String getNF() {
            return NF;
        }

        public void setNF(String NF) {
            this.NF = NF;
        }
    }
}