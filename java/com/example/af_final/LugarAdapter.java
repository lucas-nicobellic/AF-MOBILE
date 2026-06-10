package com.example.af_final;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class LugarAdapter extends RecyclerView.Adapter<LugarAdapter.ViewHolder> {

    private ArrayList<String> listaLocais;

    public LugarAdapter(ArrayList<String> listaLocais) {
        this.listaLocais = listaLocais;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_local, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {

        holder.txtLocal.setText(
                listaLocais.get(position)
        );
    }

    @Override
    public int getItemCount() {
        return listaLocais.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtLocal;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            txtLocal = itemView.findViewById(R.id.txtLocal);
        }
    }
}